package me.tooobiiii.codingtime.ui

import com.intellij.ui.ColorUtil
import com.intellij.ui.JBColor
import java.awt.Color

internal object CodingTimeColors {

    /** Follows the IDE theme's progress accent, with a sane fallback for exotic themes. */
    val accent: JBColor = JBColor.namedColor("ProgressBar.progressColor", JBColor(0x3574F0, 0x548AF7))

    val accentMuted: Color get() = ColorUtil.withAlpha(accent, 0.5)

    val track: JBColor = JBColor(Color(0, 0, 0, 18), Color(255, 255, 255, 20))
}
