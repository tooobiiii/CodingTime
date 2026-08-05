package me.tooobiiii.codingtime.stats

import com.intellij.openapi.components.serviceIfCreated
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import me.tooobiiii.codingtime.core.DayTotal
import me.tooobiiii.codingtime.storage.CodingTimeStore
import me.tooobiiii.codingtime.tracking.CodingTimeTracker
import java.time.LocalDate

/**
 * Reads recorded statistics and folds in the session that is currently running, so the UI shows
 * time growing live instead of only after a session is closed.
 *
 * The live part is added to totals and to today's bar; per-language and per-project breakdowns show
 * persisted time only.
 */
class StatsQuery(private val project: Project?, private val scopedToProject: Boolean) {

    private val store = CodingTimeStore.getInstance()
    private val projectFilter: String? = if (scopedToProject) project?.name else null

    fun today(): Long = totalIn(LocalDate.now(), LocalDate.now())

    fun totalIn(from: LocalDate, to: LocalDate): Long {
        val today = LocalDate.now()
        val live = if (!today.isBefore(from) && !today.isAfter(to)) liveSeconds(today) else 0L
        return store.totalSeconds(from, to, projectFilter) + live
    }

    fun dailyTotals(from: LocalDate, to: LocalDate): List<DayTotal> {
        val today = LocalDate.now()
        val live = liveSeconds(today)
        return store.dailyTotals(from, to, projectFilter).map { day ->
            if (day.date == today && live > 0) day.copy(seconds = day.seconds + live) else day
        }
    }

    /**
     * First day that has recorded time in the current scope, or `null` when nothing was ever
     * recorded. Used to anchor the start of the all-time range.
     */
    fun earliestDate(): LocalDate? = store.earliestDate(projectFilter)

    fun languages(from: LocalDate, to: LocalDate): List<CodingTimeStore.Share> =
        store.byLanguage(from, to, projectFilter)

    fun projects(from: LocalDate, to: LocalDate): List<CodingTimeStore.Share> =
        store.byProject(from, to)

    fun isTracking(): Boolean = trackers().any { it.isTracking() }

    private fun liveSeconds(date: LocalDate): Long = trackers().sumOf { it.unpersistedSeconds(date) }

    private fun trackers(): List<CodingTimeTracker> {
        val projects = if (scopedToProject) {
            listOfNotNull(project)
        } else {
            ProjectManager.getInstance().openProjects.toList()
        }
        return projects.filterNot { it.isDisposed }.mapNotNull { it.serviceIfCreated<CodingTimeTracker>() }
    }
}
