package com.drdisagree.iconify.features.changelog.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drdisagree.iconify.BuildConfig
import com.drdisagree.iconify.R
import com.drdisagree.iconify.core.utils.ChangelogParser
import com.drdisagree.iconify.data.common.Const.RELEASES_API
import com.drdisagree.iconify.data.common.Const.GITHUB_REPO
import com.drdisagree.iconify.features.changelog.models.ChangelogData
import com.drdisagree.iconify.features.changelog.states.ChangelogState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URL

class ChangelogViewModel : ViewModel() {

    var uiState by mutableStateOf<ChangelogState>(ChangelogState.Loading)
        private set

    fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (BuildConfig.DEBUG) {
                    loadDebugChangelog()
                } else {
                    loadReleaseChangelog()
                }
            } catch (_: Exception) {
                uiState = ChangelogState.Error
            }
        }
    }

    private fun loadReleaseChangelog() {
        val releases = org.json.JSONArray(URL(RELEASES_API).readText())
        val release = (0 until releases.length())
            .map { releases.getJSONObject(it) }
            .firstOrNull { item -> !item.optBoolean("draft", false) }
            ?: throw IllegalStateException("No published IconBro release")
        val parsed = ChangelogParser.parseReleaseChangelog(release)

        uiState = ChangelogState.Success(parsed)
    }

    private fun loadDebugChangelog() {
        val releases = org.json.JSONArray(URL(RELEASES_API).readText())
        val baseTag = (0 until releases.length())
            .map { releases.getJSONObject(it) }
            .firstOrNull { item -> !item.optBoolean("draft", false) }
            ?.getString("tag_name") ?: throw IllegalStateException("No published IconBro release")

        val compareUrl =
            "https://api.github.com/repos/in3zalo-gk/IconBro/compare/$baseTag...HEAD"

        val compareJson = URL(compareUrl).readText()
        val commits = JSONObject(compareJson).getJSONArray("commits")

        val changes = mutableSetOf<String>()

        for (i in 0 until commits.length()) {
            val commit = commits.getJSONObject(i)
            if (commit.getJSONArray("parents").length() > 1) continue

            val subject = commit
                .getJSONObject("commit")
                .getString("message")
                .lineSequence()
                .first()

            changes += "- $subject"
        }

        uiState = ChangelogState.Success(
            ChangelogData(
                titleRes = R.string.changelog_changes_since,
                titleArg = baseTag,
                contents = changes.toList()
            )
        )
    }
}