package me.tooobiiii.codingtime.tracking

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import me.tooobiiii.codingtime.settings.CodingTimeSettings
import me.tooobiiii.codingtime.storage.CodingTimeStore

class CodingTimeStartupActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        CodingTimeStore.getInstance().prune(CodingTimeSettings.getInstance().state.retentionDays)
        project.service<CodingTimeTracker>().start()
    }
}
