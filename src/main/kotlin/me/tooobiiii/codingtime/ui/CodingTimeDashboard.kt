package me.tooobiiii.codingtime.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.NamedColorUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import me.tooobiiii.codingtime.CodingTimeBundle
import me.tooobiiii.codingtime.CodingTimeListener
import me.tooobiiii.codingtime.core.ChartBuckets
import me.tooobiiii.codingtime.core.formatDuration
import me.tooobiiii.codingtime.stats.StatsQuery
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants

/** Tool window content: totals, a daily activity chart and breakdowns by language and project. */
class CodingTimeDashboard(private val project: Project, parentDisposable: Disposable) : BorderLayoutPanel() {

    private enum class Scope(private val key: String) {
        PROJECT("toolWindow.scope.project"),
        ALL("toolWindow.scope.all");

        val displayName: String get() = CodingTimeBundle.message(key)
    }

    /** [days] is `null` for the all-time range, which starts at the first recorded day instead. */
    private enum class Range(private val key: String, val days: Int?) {
        WEEK("toolWindow.range.week", 7),
        MONTH("toolWindow.range.month", 30),
        ALL("toolWindow.range.all", null);

        val displayName: String get() = CodingTimeBundle.message(key)
    }

    private val scopeCombo = ComboBox(Scope.entries.toTypedArray())
    private val rangeCombo = ComboBox(Range.entries.toTypedArray())

    private val todayValue = JBLabel()
    private val rangeValue = JBLabel()
    private val averageValue = JBLabel()

    private val chart = ActivityBarChart()
    private val languages = BreakdownPanel()
    private val projects = BreakdownPanel()

    private val languagesSection: JComponent
    private val projectsSection: JComponent
    private val statistics: JComponent
    private val emptyState = JBLabel(CodingTimeBundle.message("toolWindow.empty")).apply {
        foreground = NamedColorUtil.getInactiveTextColor()
        border = JBUI.Borders.emptyTop(16)
    }

    private val dayLabelFormatter = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
    private val weekLabelFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    private val monthLabelFormatter = DateTimeFormatter.ofPattern("MMM", Locale.getDefault())
    private val monthTooltipFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

    init {
        border = JBUI.Borders.empty(12)

        languagesSection = section(CodingTimeBundle.message("toolWindow.section.languages"), languages)
        projectsSection = section(CodingTimeBundle.message("toolWindow.section.projects"), projects)

        statistics = JPanel(VerticalLayout(JBUI.scale(4), VerticalLayout.FILL)).apply {
            isOpaque = false
            add(section(CodingTimeBundle.message("toolWindow.section.activity"), chart))
            add(languagesSection)
            add(projectsSection)
        }

        val content = JPanel(VerticalLayout(JBUI.scale(4), VerticalLayout.FILL)).apply {
            isOpaque = false
            add(statistics)
            add(emptyState)
        }

        addToTop(header())
        addToCenter(
            JBScrollPane(content).apply {
                border = JBUI.Borders.empty()
                isOpaque = false
                viewport.isOpaque = false
                horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
            },
        )

        scopeCombo.addActionListener { refresh() }
        rangeCombo.addActionListener { refresh() }

        ApplicationManager.getApplication().messageBus.connect(parentDisposable)
            .subscribe(CodingTimeListener.TOPIC, CodingTimeListener { refreshLater() })

        refresh()
    }

    private fun header(): JComponent {
        scopeCombo.renderer = SimpleListCellRenderer.create("") { it.displayName }
        rangeCombo.renderer = SimpleListCellRenderer.create("") { it.displayName }

        val filters = JPanel(HorizontalLayout(JBUI.scale(8))).apply {
            isOpaque = false
            border = JBUI.Borders.emptyBottom(12)
            add(scopeCombo)
            add(rangeCombo)
        }

        val tiles = JPanel(HorizontalLayout(JBUI.scale(24))).apply {
            isOpaque = false
            border = JBUI.Borders.emptyBottom(4)
            add(tile(CodingTimeBundle.message("toolWindow.header.today"), todayValue))
            add(tile(CodingTimeBundle.message("toolWindow.header.range"), rangeValue))
            add(tile(CodingTimeBundle.message("toolWindow.header.average"), averageValue))
        }

        return BorderLayoutPanel().apply {
            isOpaque = false
            addToTop(filters)
            addToCenter(tiles)
        }
    }

    private fun tile(caption: String, value: JBLabel): JComponent = BorderLayoutPanel().apply {
        isOpaque = false
        value.font = JBFont.h1()
        addToTop(value)
        addToCenter(
            JBLabel(caption).apply {
                font = JBFont.small()
                foreground = NamedColorUtil.getInactiveTextColor()
            },
        )
    }

    private fun section(title: String, body: JComponent): JComponent = BorderLayoutPanel().apply {
        isOpaque = false
        border = JBUI.Borders.emptyTop(16)
        addToTop(
            JBLabel(title).apply {
                font = JBFont.medium().asBold()
                border = JBUI.Borders.emptyBottom(6)
            },
        )
        addToCenter(body)
    }

    private fun refreshLater() {
        ApplicationManager.getApplication().invokeLater({ refresh() }, project.disposed)
    }

    private fun refresh() {
        val scope = scopeCombo.item ?: Scope.PROJECT
        val range = rangeCombo.item ?: Range.WEEK
        val query = StatsQuery(project, scope == Scope.PROJECT)

        val to = LocalDate.now()
        val from = range.days
            ?.let { to.minusDays((it - 1).toLong()) }
            ?: (query.earliestDate() ?: to)
        val dailyTotals = query.dailyTotals(from, to)
        val rangeTotal = dailyTotals.sumOf { it.seconds }
        // The all-time range averages over the days actually tracked, not over a fixed window.
        val dayCount = (ChronoUnit.DAYS.between(from, to) + 1).coerceAtLeast(1)

        todayValue.text = formatDuration(query.today())
        rangeValue.text = formatDuration(rangeTotal)
        averageValue.text = formatDuration(rangeTotal / dayCount)

        chart.bars = ChartBuckets.of(dailyTotals).map { bucket ->
            ActivityBarChart.Bar(
                label = label(bucket),
                tooltip = tooltip(bucket),
                seconds = bucket.seconds,
                highlighted = !to.isBefore(bucket.start) && !to.isAfter(bucket.end),
            )
        }
        languages.setItems(query.languages(from, to))
        projects.setItems(query.projects(from, to))

        projectsSection.isVisible = scope == Scope.ALL
        languagesSection.isVisible = true
        statistics.isVisible = rangeTotal > 0
        emptyState.isVisible = rangeTotal == 0L

        revalidate()
        repaint()
    }

    private fun label(bucket: ChartBuckets.Bucket): String = when (bucket.granularity) {
        ChartBuckets.Granularity.DAY -> bucket.start.format(dayLabelFormatter)
        ChartBuckets.Granularity.WEEK -> bucket.start.format(weekLabelFormatter)
        ChartBuckets.Granularity.MONTH -> bucket.start.format(monthLabelFormatter)
    }

    private fun tooltip(bucket: ChartBuckets.Bucket): String {
        val duration = formatDuration(bucket.seconds)
        return when (bucket.granularity) {
            ChartBuckets.Granularity.DAY ->
                "${bucket.start.format(dateFormatter)}: $duration"

            ChartBuckets.Granularity.WEEK ->
                "${bucket.start.format(dateFormatter)} – ${bucket.end.format(dateFormatter)}: $duration"

            ChartBuckets.Granularity.MONTH ->
                "${bucket.start.format(monthTooltipFormatter)}: $duration"
        }
    }
}
