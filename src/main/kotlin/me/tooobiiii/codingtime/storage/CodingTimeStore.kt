package me.tooobiiii.codingtime.storage

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XCollection
import me.tooobiiii.codingtime.CodingTimeListener
import me.tooobiiii.codingtime.core.DaySplitter
import me.tooobiiii.codingtime.core.DayTotal
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

/**
 * Aggregated coding time, kept per day / project / language and persisted in the IDE configuration
 * directory. Everything stays on this machine.
 */
@Service(Service.Level.APP)
@State(name = "CodingTime", storages = [Storage("codingTime.xml", roamingType = RoamingType.DISABLED)])
class CodingTimeStore : PersistentStateComponent<CodingTimeStore.State> {

    class State {
        @get:XCollection(style = XCollection.Style.v2)
        var entries: MutableList<Entry> = mutableListOf()
    }

    @Tag("entry")
    class Entry() {
        @get:Attribute("date")
        var date: String = ""

        @get:Attribute("project")
        var project: String = ""

        @get:Attribute("language")
        var language: String = ""

        @get:Attribute("seconds")
        var seconds: Long = 0

        constructor(date: String, project: String, language: String, seconds: Long) : this() {
            this.date = date
            this.project = project
            this.language = language
            this.seconds = seconds
        }
    }

    private data class Key(val date: LocalDate, val project: String, val language: String)

    data class Share(val name: String, val seconds: Long)

    private val totals = ConcurrentHashMap<Key, Long>()

    /** Adds the interval, splitting it across midnight when needed. */
    fun record(startMillis: Long, endMillis: Long, project: String, language: String) {
        val slices = DaySplitter.split(startMillis, endMillis)
        if (slices.isEmpty()) return

        for (slice in slices) {
            totals.merge(Key(slice.date, project, language), slice.seconds, Long::plus)
        }
        fireChanged()
    }

    fun secondsOn(date: LocalDate, project: String? = null): Long =
        totalSeconds(date, date, project)

    fun totalSeconds(from: LocalDate, to: LocalDate, project: String? = null): Long =
        totals.entries.sumOf { (key, seconds) -> if (key.matches(from, to, project)) seconds else 0L }

    /** Dense list of daily totals, including days without any recorded time. */
    fun dailyTotals(from: LocalDate, to: LocalDate, project: String? = null): List<DayTotal> {
        val byDate = HashMap<LocalDate, Long>()
        for ((key, seconds) in totals) {
            if (key.matches(from, to, project)) byDate.merge(key.date, seconds, Long::plus)
        }
        return generateSequence(from) { it.plusDays(1) }
            .takeWhile { !it.isAfter(to) }
            .map { DayTotal(it, byDate[it] ?: 0L) }
            .toList()
    }

    fun byLanguage(from: LocalDate, to: LocalDate, project: String? = null): List<Share> =
        shares(from, to, project) { it.language }

    fun byProject(from: LocalDate, to: LocalDate): List<Share> =
        shares(from, to, null) { it.project }

    fun knownProjects(): List<String> =
        totals.keys.map { it.project }.distinct().sorted()

    /** First day with recorded time, or `null` while nothing has been recorded yet. */
    fun earliestDate(project: String? = null): LocalDate? =
        totals.keys.filter { project == null || it.project == project }.minOfOrNull { it.date }

    /** Drops statistics older than [retentionDays] days. */
    fun prune(retentionDays: Int) {
        if (retentionDays <= 0) return
        val cutoff = LocalDate.now().minusDays(retentionDays.toLong())
        val removed = totals.keys.removeIf { it.date.isBefore(cutoff) }
        if (removed) fireChanged()
    }

    fun clear() {
        totals.clear()
        fireChanged()
    }

    override fun getState(): State = State().apply {
        entries = totals.entries
            .sortedWith(compareBy({ it.key.date }, { it.key.project }, { it.key.language }))
            .mapTo(mutableListOf()) { (key, seconds) ->
                Entry(key.date.toString(), key.project, key.language, seconds)
            }
    }

    override fun loadState(state: State) {
        totals.clear()
        for (entry in state.entries) {
            val date = runCatching { LocalDate.parse(entry.date) }.getOrNull() ?: continue
            if (entry.seconds <= 0) continue
            totals.merge(Key(date, entry.project, entry.language), entry.seconds, Long::plus)
        }
    }

    private inline fun shares(
        from: LocalDate,
        to: LocalDate,
        project: String?,
        crossinline dimension: (Key) -> String,
    ): List<Share> {
        val bucket = HashMap<String, Long>()
        for ((key, seconds) in totals) {
            if (key.matches(from, to, project)) bucket.merge(dimension(key), seconds, Long::plus)
        }
        return bucket.map { (name, seconds) -> Share(name, seconds) }
            .sortedWith(compareByDescending<Share> { it.seconds }.thenBy { it.name })
    }

    private fun Key.matches(from: LocalDate, to: LocalDate, project: String?): Boolean =
        !date.isBefore(from) && !date.isAfter(to) && (project == null || this.project == project)

    private fun fireChanged() {
        ApplicationManager.getApplication()
            .messageBus
            .syncPublisher(CodingTimeListener.TOPIC)
            .codingTimeChanged()
    }

    companion object {
        fun getInstance(): CodingTimeStore = service()
    }
}
