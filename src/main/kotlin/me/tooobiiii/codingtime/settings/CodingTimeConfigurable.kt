package me.tooobiiii.codingtime.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.Messages
import com.intellij.ui.dsl.builder.bindIntValue
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import me.tooobiiii.codingtime.CodingTimeBundle
import me.tooobiiii.codingtime.CodingTimeListener
import me.tooobiiii.codingtime.storage.CodingTimeStore

class CodingTimeConfigurable : BoundConfigurable(CodingTimeBundle.message("settings.title")) {

    private val state = CodingTimeSettings.getInstance().state

    override fun createPanel(): DialogPanel = panel {
        group(CodingTimeBundle.message("settings.group.tracking")) {
            row {
                val enabled = checkBox(CodingTimeBundle.message("settings.idleTimeout"))
                    .bindSelected(state::idleTimeoutEnabled)
                spinner(30..3600, 30)
                    .bindIntValue(state::idleTimeoutSeconds)
                    .enabledIf(enabled.selected)
                label(CodingTimeBundle.message("settings.idleTimeout.suffix"))
                    .enabledIf(enabled.selected)
            }.rowComment(CodingTimeBundle.message("settings.idleTimeout.comment"))

            row {
                checkBox(CodingTimeBundle.message("settings.trackCaret"))
                    .bindSelected(state::trackNavigation)
            }.rowComment(CodingTimeBundle.message("settings.trackCaret.comment"))
        }

        group(CodingTimeBundle.message("settings.group.display")) {
            row {
                checkBox(CodingTimeBundle.message("settings.statusBarScope"))
                    .bindSelected(state::statusBarScopedToProject)
            }
        }

        group(CodingTimeBundle.message("settings.group.data")) {
            row(CodingTimeBundle.message("settings.retention")) {
                spinner(7..3650, 7).bindIntValue(state::retentionDays)
                label(CodingTimeBundle.message("settings.retention.suffix"))
            }

            row {
                button(CodingTimeBundle.message("settings.reset")) { deleteAllStatistics() }
            }
        }
    }

    override fun apply() {
        super.apply()
        // Settings such as the status bar scope change what the widgets display.
        fireChanged()
    }

    private fun deleteAllStatistics() {
        val confirmed = Messages.showYesNoDialog(
            CodingTimeBundle.message("settings.reset.confirm.message"),
            CodingTimeBundle.message("settings.reset.confirm.title"),
            Messages.getWarningIcon(),
        ) == Messages.YES
        if (!confirmed) return

        CodingTimeStore.getInstance().clear()
        Messages.showInfoMessage(
            CodingTimeBundle.message("settings.reset.done"),
            CodingTimeBundle.message("settings.reset.confirm.title"),
        )
    }

    private fun fireChanged() {
        ApplicationManager.getApplication()
            .messageBus
            .syncPublisher(CodingTimeListener.TOPIC)
            .codingTimeChanged()
    }
}
