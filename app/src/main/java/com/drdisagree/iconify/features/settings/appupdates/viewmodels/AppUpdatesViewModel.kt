package com.drdisagree.iconify.features.settings.appupdates.viewmodels

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drdisagree.iconify.BuildConfig
import com.drdisagree.iconify.core.utils.ChangelogParser
import com.drdisagree.iconify.data.common.Const.RELEASES_API
import com.drdisagree.iconify.data.common.Const.RELEASES_PAGE
import com.drdisagree.iconify.features.changelog.models.ChangelogData
import com.drdisagree.iconify.features.settings.appupdates.states.AppUpdatesState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

@HiltViewModel
class AppUpdatesViewModel @Inject constructor() : ViewModel() {

    var uiState by mutableStateOf<AppUpdatesState>(AppUpdatesState.Idle)
        private set

    private var lastVersionName: String = ""
    private var lastChangelog: ChangelogData? = null
    private var lastDownloadUrl: String = ""

    fun checkForUpdates() {
        uiState = AppUpdatesState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonText = URL(RELEASES_API).readText()
                val releases = org.json.JSONArray(jsonText)
                val release = (0 until releases.length())
                    .map { releases.getJSONObject(it) }
                    .firstOrNull { item ->
                        !item.optBoolean("draft", false) &&
                                item.optJSONArray("assets")?.let { assets ->
                                    (0 until assets.length()).any {
                                        assets.getJSONObject(it).optString("name")
                                            .endsWith(".apk", ignoreCase = true)
                                    }
                                } == true
                    }
                if (release == null) {
                    uiState = AppUpdatesState.UpToDate(
                        currentVersion = BuildConfig.VERSION_NAME.removePrefix("v"),
                        latestVersion = BuildConfig.VERSION_NAME.removePrefix("v")
                    )
                    return@launch
                }
                val json = release
                val tagName = json.getString("tag_name")

                if (compareVersions(tagName, BuildConfig.VERSION_NAME)) {
                    val changelog = ChangelogParser.parseReleaseChangelog(json)
                    var downloadUrl = RELEASES_PAGE
                    if (json.has("assets")) {
                        val assets = json.getJSONArray("assets")
                        val apk = (0 until assets.length())
                            .map { assets.getJSONObject(it) }
                            .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) }
                        if (apk != null) downloadUrl = apk.getString("browser_download_url")
                    }
                    lastVersionName = tagName
                    lastChangelog = changelog
                    lastDownloadUrl = downloadUrl

                    uiState = AppUpdatesState.UpdateAvailable(
                        versionName = tagName.removePrefix("iconbro-").removePrefix("v"),
                        changelog = changelog,
                        downloadUrl = downloadUrl
                    )
                } else {
                    uiState = AppUpdatesState.UpToDate(
                        currentVersion = BuildConfig.VERSION_NAME.removePrefix("v"),
                        latestVersion = tagName.removePrefix("iconbro-").removePrefix("v")
                    )
                }
            } catch (_: Exception) {
                uiState = AppUpdatesState.Error(
                    currentVersion = BuildConfig.VERSION_NAME
                )
            }
        }
    }

    fun downloadAndInstallUpdate(context: Context, downloadUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            uiState = AppUpdatesState.Downloading(progress = 0f)
            try {
                val url = URL(downloadUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()

                val totalBytes = connection.contentLength
                val inputStream = connection.inputStream

                val tempFile = File(context.cacheDir, "update.apk")
                val outputStream = FileOutputStream(tempFile)

                var downloadedBytes = 0
                val buffer = ByteArray(8192)
                var bytesRead: Int

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    if (totalBytes > 0) {
                        val progress = (downloadedBytes.toFloat() / totalBytes.toFloat()) * 100f
                        uiState = AppUpdatesState.Downloading(progress = progress)
                    }
                }

                outputStream.close()
                inputStream.close()
                connection.disconnect()

                uiState = AppUpdatesState.Installing

                val packageInstaller = context.packageManager.packageInstaller
                val params =
                    PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
                val sessionId = packageInstaller.createSession(params)
                val session = packageInstaller.openSession(sessionId)

                val out = session.openWrite("package", 0, tempFile.length())
                val input = FileInputStream(tempFile)
                input.copyTo(out)
                session.fsync(out)
                input.close()
                out.close()

                val intent = Intent(
                    context,
                    com.drdisagree.iconify.services.receivers.PackageInstallReceiver::class.java
                )
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    sessionId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )

                if (lastChangelog != null) {
                    uiState = AppUpdatesState.UpdateAvailable(
                        versionName = lastVersionName.removePrefix("iconbro-").removePrefix("v"),
                        changelog = lastChangelog!!,
                        downloadUrl = lastDownloadUrl
                    )
                }

                session.commit(pendingIntent.intentSender)
                session.close()

            } catch (_: Exception) {
                uiState = AppUpdatesState.Error(
                    currentVersion = BuildConfig.VERSION_NAME
                )
            }
        }
    }

    private fun compareVersions(latestTag: String, currentVersion: String): Boolean {
        data class Version(val major: Int, val minor: Int, val patch: Int, val prerelease: Int?)
        fun parse(value: String): Version? {
            val match = Regex("(?i)^(?:iconbro-)?v?(\\d+)\\.(\\d+)\\.(\\d+)(?:[-.]?(?:beta|alpha|rc)[-.]?(\\d+))?.*$")
                .matchEntire(value.trim()) ?: return null
            return Version(
                match.groupValues[1].toIntOrNull() ?: return null,
                match.groupValues[2].toIntOrNull() ?: return null,
                match.groupValues[3].toIntOrNull() ?: return null,
                match.groupValues[4].takeIf { it.isNotEmpty() }?.toIntOrNull()
            )
        }
        val latest = parse(latestTag) ?: return false
        val current = parse(currentVersion) ?: return false
        val base = compareValuesBy(latest, current, Version::major, Version::minor, Version::patch)
        if (base != 0) return base > 0
        return when {
            latest.prerelease == null && current.prerelease != null -> true
            latest.prerelease != null && current.prerelease == null -> false
            latest.prerelease != null && current.prerelease != null -> latest.prerelease > current.prerelease
            else -> false
        }
    }
}
