package me.tooobiiii.codingtime.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.util.Consumer
import me.tooobiiii.codingtime.CodingTimeBundle
import me.tooobiiii.codingtime.CodingTimeListener
import me.tooobiiii.codingtime.core.formatDuration
import me.tooobiiii.codingtime.settings.CodingTimeSettings
import me.tooobiiii.codingtime.stats.StatsQuery
import java.awt.Component
import java.awt.event.MouseEvent
import java.time.LocalDate

class CodingTimeStatusBarWidget(private val project: Project) : StatusBarWidget, StatusBarWidget.TextPresentation {

    private var statusBar: StatusBar? = null

    override fun ID(): String = WIDGET_ID

    override fun install(statusBar: StatusBar) {
        this.statusBar = statusBar
        ApplicationManager.getApplication().messageBus.connect(this)
            .subscribe(CodingTimeListener.TOPIC, CodingTimeListener { refresh() })
    }

    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this

    override fun getText(): String = "⏱ ${formatDuration(query().today())}"

    override fun getAlignment(): Float = Component.CENTER_ALIGNMENT

    override fun getTooltipText(): String {
        val query = query()
        val today = LocalDate.now()
        val lines = buildList {
            add(CodingTimeBundle.message("statusBar.tooltip.title"))
            add(CodingTimeBundle.message("statusBar.tooltip.today", formatDuration(query.today())))
            add(
                CodingTimeBundle.message(
                    "statusBar.tooltip.week",
                    formatDuration(query.totalIn(today.minusDays(6), today)),
                ),
            )
            if (!query.isTracking()) add(CodingTimeBundle.message("statusBar.tooltip.idle"))
            add(CodingTimeBundle.message("statusBar.tooltip.hint"))
        }
        return lines.joinToString("<br>", prefix = "<html>", postfix = "</html>")
    }

    override fun getClickConsumer(): Consumer<MouseEvent> = Consumer {
        ToolWindowManager.getInstance(project).getToolWindow(CodingTimeToolWindowFactory.TOOL_WINDOW_ID)?.activate(null)
    }

    override fun dispose() {
        statusBar = null
    }

    private fun query() = StatsQuery(project, CodingTimeSettings.getInstance().state.statusBarScopedToProject)

    private fun refresh() {
        ApplicationManager.getApplication().invokeLater(
            { statusBar?.updateWidget(WIDGET_ID) },
            project.disposed,
        )
    }

    companion object {
        const val WIDGET_ID: String = "me.tooobiiii.codingtime.statusBar"
    }
}
