package me.tooobiiii.codingtime.ui

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import me.tooobiiii.codingtime.CodingTimeBundle

class CodingTimeStatusBarWidgetFactory : StatusBarWidgetFactory, DumbAware {

    override fun getId(): String = CodingTimeStatusBarWidget.WIDGET_ID

    override fun getDisplayName(): String = CodingTimeBundle.message("statusBar.displayName")

    override fun isAvailable(project: Project): Boolean = true

    override fun createWidget(project: Project): StatusBarWidget = CodingTimeStatusBarWidget(project)

    override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true
}
