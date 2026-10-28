package com.drdisagree.iconify.services.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.drdisagree.iconify.BuildConfig
import com.drdisagree.iconify.R
import com.drdisagree.iconify.app.MainActivity
import com.drdisagree.iconify.data.config.RPrefs
import com.drdisagree.iconify.data.common.Const.RELEASES_API
import com.drdisagree.iconify.data.keys.SettingsKey
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class UpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val tag = this::class.java.simpleName

    override suspend fun doWork(): Result {
        val isGoodNetwork = isGoodNetworkAvailable()

        if (isGoodNetwork) {
            val jsonStr = fetchLatestRelease()

            if (jsonStr != null) {
                try {
                    val releases = org.json.JSONArray(jsonStr)
                    val latestRelease = (0 until releases.length())
                        .map { releases.getJSONObject(it) }
                        .firstOrNull { item ->
                            !item.optBoolean("draft", false) &&
                                    item.optJSONArray("assets")?.let { assets ->
                                        (0 until assets.length()).any {
                                            assets.getJSONObject(it).optString("name")
                                                .endsWith(".apk", ignoreCase = true)
                                        }
                                    } == true
                        } ?: return Result.success()
                    val tagName = latestRelease.getString("tag_name")

                    if (compareVersions(tagName, BuildConfig.VERSION_NAME)) {
                        showUpdateNotification(tagName)
                    }
                    return Result.success()
                } catch (e: Exception) {
                    Log.e(tag, "Error: ${e.message}")
                    return Result.retry()
                }
            } else {
                return Result.retry()
            }
        }

        return Result.retry()
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

    private fun isGoodNetworkAvailable(): Boolean {
        val updateOverWifiOnly = RPrefs.getBoolean(SettingsKey.UPDATE_OVER_WIFI)
        val connectivityManager =
            applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return capabilities != null && (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && (!updateOverWifiOnly || capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)))
    }

    private suspend fun fetchLatestRelease(): String? {
        return withContext(Dispatchers.IO) {
            var urlConnection: HttpURLConnection? = null
            var bufferedReader: BufferedReader? = null

            try {
                val url = URL(RELEASES_API)
                urlConnection = url.openConnection() as HttpURLConnection
                urlConnection.connect()

                val inputStream = urlConnection.inputStream
                bufferedReader = BufferedReader(InputStreamReader(inputStream))

                val stringBuffer = StringBuilder()
                var line: String?

                while (bufferedReader.readLine().also { line = it } != null) {
                    stringBuffer.append(line).append("\n")
                }

                if (stringBuffer.isEmpty()) {
                    null
                } else {
                    stringBuffer.toString()
                }
            } catch (e: Exception) {
                null
            } finally {
                urlConnection?.disconnect()
                bufferedReader?.close()
            }
        }
    }

    private fun showUpdateNotification(version: String) {
        Shell.cmd("pm grant ${BuildConfig.APPLICATION_ID} android.permission.POST_NOTIFICATIONS")
            .exec()

        val notificationIntent = Intent(applicationContext, MainActivity::class.java).apply {
            putExtra("open_app_updates", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(
            applicationContext,
            applicationContext.getString(R.string.update_notification_channel_name)
        )
            .setSmallIcon(R.drawable.ic_launcher_fg)
            .setContentTitle(applicationContext.getString(R.string.new_update_title))
            .setContentText(applicationContext.getString(R.string.update_dialog_desc, version))
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createChannel(notificationManager)

        notificationManager.notify(0, notificationBuilder.build())
    }

    private fun createChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            applicationContext.getString(R.string.update_notification_channel_name),
            applicationContext.getString(R.string.update_notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.description =
            applicationContext.getString(R.string.update_notification_channel_desc)

        notificationManager.createNotificationChannel(channel)
    }
}