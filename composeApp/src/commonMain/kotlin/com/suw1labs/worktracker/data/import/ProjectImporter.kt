package com.suw1labs.worktracker.data.import

import com.suw1labs.worktracker.data.model.Project

/**
 * Parses a project list pasted from Excel / SAP / a CSV file.
 * Accepts tab, semicolon or comma separated lines: `code<sep>name[<sep>customer]`.
 * A first line that looks like a header ("project", "code", "number", "name") is skipped.
 */
object ProjectImporter {
    private val PALETTE = listOf("#3B82F6", "#10B981", "#8B5CF6", "#F59E0B", "#EC4899", "#06B6D4", "#EF4444", "#64748B")

    data class ParsedProject(val code: String, val name: String, val client: String)

    fun parse(text: String): List<ParsedProject> {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val result = mutableListOf<ParsedProject>()
        lines.forEachIndexed { index, line ->
            val cells = splitLine(line)
            if (cells.isEmpty()) return@forEachIndexed
            if (index == 0 && looksLikeHeader(cells)) return@forEachIndexed
            val code = cells[0]
            val name = cells.getOrNull(1)?.takeIf { it.isNotBlank() } ?: code
            val client = cells.getOrNull(2) ?: ""
            if (code.isNotBlank()) result.add(ParsedProject(code, name, client))
        }
        return result
    }

    fun toProjects(parsed: List<ParsedProject>, colorOffset: Int = 0): List<Project> =
        parsed.mapIndexed { index, p ->
            Project(
                code = p.code,
                name = p.name,
                client = p.client,
                colorHex = PALETTE[(index + colorOffset) % PALETTE.size]
            )
        }

    private fun splitLine(line: String): List<String> {
        val separator = when {
            line.contains('\t') -> '\t'
            line.contains(';') -> ';'
            line.contains(',') -> ','
            else -> null
        }
        val raw = if (separator == null) listOf(line) else splitCsv(line, separator)
        return raw.map { it.trim().trim('"').trim() }
    }

    /** Minimal CSV splitting that honours double quotes. */
    private fun splitCsv(line: String, separator: Char): List<String> {
        val cells = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == separator && !inQuotes -> {
                    cells.add(current.toString())
                    current.setLength(0)
                }
                else -> current.append(ch)
            }
        }
        cells.add(current.toString())
        return cells
    }

    private fun looksLikeHeader(cells: List<String>): Boolean {
        val first = cells[0].lowercase()
        val second = cells.getOrNull(1)?.lowercase() ?: ""
        val headerWords = listOf("project", "code", "number", "nummer", "projekt", "name", "sap", "id")
        return headerWords.any { first.contains(it) } && (second.isEmpty() || headerWords.any { second.contains(it) })
    }
}
