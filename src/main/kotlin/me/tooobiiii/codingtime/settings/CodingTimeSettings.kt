package me.tooobiiii.codingtime.settings

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

@Service(Service.Level.APP)
@State(name = "CodingTimeSettings", storages = [Storage("codingTime.xml", roamingType = RoamingType.DISABLED)])
class CodingTimeSettings : SimplePersistentStateComponent<CodingTimeSettings.State>(State()) {

    class State : BaseState() {
        /** Whether inactivity closes the current coding session at all. */
        var idleTimeoutEnabled: Boolean by property(true)

        /** A pause longer than this closes the current coding session. */
        var idleTimeoutSeconds: Int by property(DEFAULT_IDLE_TIMEOUT_SECONDS)

        /** Days of daily statistics to keep. */
        var retentionDays: Int by property(DEFAULT_RETENTION_DAYS)

        /** Count caret movement and file switches, not only document edits. */
        var trackNavigation: Boolean by property(true)

        /** Status bar shows the current project's time instead of the total across all projects. */
        var statusBarScopedToProject: Boolean by property(true)
    }

    /** Inactivity that ends a session, or [NEVER] when the timeout is switched off. */
    val idleTimeoutMillis: Long
        get() = if (state.idleTimeoutEnabled) state.idleTimeoutSeconds.toLong() * 1000L else NEVER

    companion object {
        /** An idle timeout no gap can ever exceed, so a session only ends when the project closes. */
        const val NEVER = Long.MAX_VALUE

        const val DEFAULT_IDLE_TIMEOUT_SECONDS = 120
        const val DEFAULT_RETENTION_DAYS = 365

        fun getInstance(): CodingTimeSettings = service()
    }
}
