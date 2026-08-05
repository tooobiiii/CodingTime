package me.tooobiiii.codingtime.tracking

import com.intellij.openapi.vfs.VirtualFile

/** Maps a file to the language label shown in the statistics. */
object LanguageNames {

    private val ACRONYMS = setOf(
        "CSS", "CSV", "HTML", "HTTP", "INI", "JS", "JSON", "JSP", "MD", "PHP", "SCSS",
        "SQL", "SVG", "TOML", "TS", "XML", "XSD", "XSL", "YAML",
    )

    private val IGNORED = setOf("UNKNOWN", "IDEA_MODULE", "IDEA_PROJECT", "IDEA_WORKSPACE")

    /** `null` when the file should not be counted as coding at all. */
    fun of(file: VirtualFile): String? {
        val fileType = file.fileType
        if (fileType.isBinary || fileType.name in IGNORED) return null
        return prettify(fileType.name)
    }

    private fun prettify(rawName: String): String =
        rawName.split(' ', '_')
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                val upper = word.uppercase()
                if (upper in ACRONYMS) upper else word.lowercase().replaceFirstChar(Char::titlecase)
            }
            .ifBlank { rawName }
}
