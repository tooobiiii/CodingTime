package me.tooobiiii.codingtime.ui

import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.NamedColorUtil
import me.tooobiiii.codingtime.core.formatDuration
import me.tooobiiii.codingtime.storage.CodingTimeStore
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.RoundRectangle2D
import javax.swing.JComponent
import javax.swing.JPanel

/** `Kotlin ▓▓▓▓▓░░░ 3h 12m` rows, sorted by time and scaled against the largest entry. */
class BreakdownPanel : JPanel(GridBagLayout()) {

    init {
        isOpaque = false
    }

    fun setItems(items: List<CodingTimeStore.Share>, limit: Int = MAX_ROWS) {
        removeAll()

        val maxSeconds = items.firstOrNull()?.seconds ?: 0L
        items.take(limit).forEachIndexed { row, item ->
            val constraints = GridBagConstraints().apply {
                gridy = row
                insets = JBUI.insets(2, 0)
                anchor = GridBagConstraints.LINE_START
            }

            add(JBLabel(item.name), constraints.apply { gridx = 0 })
            add(
                ShareBar(if (maxSeconds > 0) item.seconds.toDouble() / maxSeconds else 0.0),
                constraints.apply {
                    gridx = 1
                    weightx = 1.0
                    fill = GridBagConstraints.HORIZONTAL
                    insets = JBUI.insets(2, 10)
                },
            )
            add(
                JBLabel(formatDuration(item.seconds)).apply { foreground = NamedColorUtil.getInactiveTextColor() },
                constraints.apply {
                    gridx = 2
                    weightx = 0.0
                    fill = GridBagConstraints.NONE
                    anchor = GridBagConstraints.LINE_END
                    insets = JBUI.insets(2, 0)
                },
            )
        }

        revalidate()
        repaint()
    }

    private class ShareBar(private val fraction: Double) : JComponent() {

        init {
            isOpaque = false
            preferredSize = JBUI.size(80, 6)
            minimumSize = JBUI.size(24, 6)
        }

        override fun paintComponent(g: Graphics) {
            val g2 = g.create() as Graphics2D
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                val arc = height.toFloat()

                g2.color = CodingTimeColors.track
                g2.fill(RoundRectangle2D.Float(0f, 0f, width.toFloat(), height.toFloat(), arc, arc))

                val filled = (width * fraction).toFloat().coerceIn(0f, width.toFloat())
                if (filled > 0) {
                    g2.color = CodingTimeColors.accent
                    g2.fill(RoundRectangle2D.Float(0f, 0f, filled, height.toFloat(), arc, arc))
                }
            } finally {
                g2.dispose()
            }
        }
    }

    private companion object {
        const val MAX_ROWS = 8
    }
}
