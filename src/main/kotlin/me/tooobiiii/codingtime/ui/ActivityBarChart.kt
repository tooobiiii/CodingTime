package me.tooobiiii.codingtime.ui

import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.NamedColorUtil
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.MouseEvent
import java.awt.geom.RoundRectangle2D
import javax.swing.JComponent
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Bar chart of coding time over a range, drawn in the current IDE theme's accent. One bar per
 * bucket — a day, a week or a month, depending on how much the range has been rolled up.
 */
class ActivityBarChart : JComponent() {

    data class Bar(val label: String, val tooltip: String, val seconds: Long, val highlighted: Boolean)

    var bars: List<Bar> = emptyList()
        set(value) {
            field = value
            repaint()
        }

    init {
        isOpaque = false
        // Registers the component with the tooltip manager; the actual text is per bar.
        toolTipText = ""
        preferredSize = JBUI.size(240, 136)
    }

    override fun getToolTipText(event: MouseEvent): String? = barAt(event.x)?.tooltip

    override fun paintComponent(g: Graphics) {
        if (bars.isEmpty()) return

        val g2 = g.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.font = JBFont.small()

            val labelHeight = g2.fontMetrics.height + JBUI.scale(4)
            val chartHeight = max(height - labelHeight, JBUI.scale(24)).toFloat()
            val slot = width.toFloat() / bars.size
            val gap = JBUI.scale(3).toFloat()
            val barWidth = max(slot - gap, JBUI.scale(2).toFloat())
            val arc = JBUI.scale(4).toFloat()
            val maxSeconds = max(bars.maxOf { it.seconds }, 1L)
            val labelStep = labelStep(g2, slot)

            bars.forEachIndexed { index, bar ->
                val x = index * slot + (slot - barWidth) / 2f

                g2.color = CodingTimeColors.track
                g2.fill(RoundRectangle2D.Float(x, 0f, barWidth, chartHeight, arc, arc))

                if (bar.seconds > 0) {
                    val filled = max(
                        chartHeight * (bar.seconds.toDouble() / maxSeconds).toFloat(),
                        JBUI.scale(3).toFloat(),
                    )
                    g2.color = if (bar.highlighted) CodingTimeColors.accent else CodingTimeColors.accentMuted
                    g2.fill(RoundRectangle2D.Float(x, chartHeight - filled, barWidth, filled, arc, arc))
                }

                // Anchored to the last bar so the current day, week or month is always labelled.
                if ((bars.lastIndex - index) % labelStep == 0) {
                    g2.color = NamedColorUtil.getInactiveTextColor()
                    val labelWidth = g2.fontMetrics.stringWidth(bar.label)
                    val labelX = (index * slot + (slot - labelWidth) / 2f).roundToInt()
                    g2.drawString(bar.label, labelX, height - g2.fontMetrics.descent)
                }
            }
        } finally {
            g2.dispose()
        }
    }

    private fun labelStep(g2: Graphics2D, slot: Float): Int {
        val widest = bars.maxOf { g2.fontMetrics.stringWidth(it.label) } + JBUI.scale(6)
        return max(ceil(widest / slot).toInt(), 1)
    }

    private fun barAt(x: Int): Bar? {
        if (bars.isEmpty() || width <= 0) return null
        val index = (x / (width.toFloat() / bars.size)).toInt()
        return bars.getOrNull(index)
    }
}
