package me.tooobiiii.codingtime.tracking

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.tooobiiii.codingtime.CodingTimeListener
import me.tooobiiii.codingtime.core.ActivityAccumulator
import me.tooobiiii.codingtime.settings.CodingTimeSettings
import me.tooobiiii.codingtime.storage.CodingTimeStore
import java.time.LocalDate
import java.time.ZoneId

/**
 * Watches editor activity in one project and turns it into recorded coding time.
 *
 * Only editors of *this* project are counted, so several open projects each track their own time.
 */
@Service(Service.Level.PROJECT)
class CodingTimeTracker(private val project: Project, private val scope: CoroutineScope) : Disposable {

    private val accumulator = ActivityAccumulator()
    private val lock = Any()

    fun start() {
        installListeners()
        scope.launch {
            while (isActive) {
                delay(TICK_MILLIS)
                tick()
            }
        }
    }

    private fun installListeners() {
        val multicaster = EditorFactory.getInstance().eventMulticaster

        multicaster.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                onActivityIn(event.document)
            }
        }, this)

        multicaster.addCaretListener(object : CaretListener {
            override fun caretPositionChanged(event: CaretEvent) {
                if (!CodingTimeSettings.getInstance().state.trackNavigation) return
                if (event.editor.project != project) return
                onActivityIn(event.editor.document)
            }
        }, this)

        project.messageBus.connect(this).subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) {
                    if (!CodingTimeSettings.getInstance().state.trackNavigation) return
                    event.newFile?.let(::registerActivity)
                }
            },
        )
    }

    private fun onActivityIn(document: Document) {
        fileOf(document)?.let(::registerActivity)
    }

    /** Resolves the edited document to a file that is currently open in *this* project. */
    private fun fileOf(document: Document): VirtualFile? {
        if (project.isDisposed) return null
        val file = FileDocumentManager.getInstance().getFile(document) ?: return null
        return file.takeIf { FileEditorManager.getInstance(project).selectedFiles.contains(it) }
    }

    private fun registerActivity(file: VirtualFile) {
        val language = LanguageNames.of(file) ?: return
        val timeout = CodingTimeSettings.getInstance().idleTimeoutMillis
        val completed = synchronized(lock) {
            accumulator.onActivity(System.currentTimeMillis(), language, timeout)
        }
        completed?.let(::persist)
    }

    private fun tick() {
        val timeout = CodingTimeSettings.getInstance().idleTimeoutMillis
        val completed = synchronized(lock) { accumulator.onTick(System.currentTimeMillis(), timeout) }
        when {
            completed != null -> persist(completed)
            // Keep the live status bar value moving while a session is running.
            openSegmentExists() -> fireChanged()
        }
    }

    /**
     * Seconds of the currently running session that fall on [date] and are not yet persisted.
     * Time after the last activity is deliberately not counted.
     */
    fun unpersistedSeconds(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long {
        val segment = synchronized(lock) { accumulator.openSegment() } ?: return 0
        val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val from = maxOf(segment.startMillis, dayStart)
        val to = minOf(segment.endMillis, dayEnd)
        return if (to <= from) 0 else (to - from) / 1000
    }

    fun isTracking(): Boolean = openSegmentExists()

    override fun dispose() {
        val pending = synchronized(lock) { accumulator.flush() }
        pending?.let(::persist)
    }

    private fun openSegmentExists(): Boolean = synchronized(lock) { accumulator.openSegment() } != null

    private fun persist(segment: ActivityAccumulator.Segment) {
        CodingTimeStore.getInstance()
            .record(segment.startMillis, segment.endMillis, project.name, segment.language)
    }

    private fun fireChanged() {
        ApplicationManager.getApplication()
            .messageBus
            .syncPublisher(CodingTimeListener.TOPIC)
            .codingTimeChanged()
    }

    companion object {
        private const val TICK_MILLIS = 5_000L
    }
}
