package com.starborn.mantracounter.data

import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Reads back what [BackupWriter] produced.
 *
 * The file stays the plain, human-readable one — this parses that same text rather than a second
 * machine-only copy, so there is exactly one format to keep working. `BackupRoundTripTest` walks
 * a snapshot out through the writer and back in through here, which is what stops the two drifting
 * apart.
 *
 * Background images are not in the file (they are photos, not text) and are not restored.
 */
object BackupReader {

    private val thinRule = Regex("^─{4,}$")
    private val dayRow = Regex("""^\s*(\d{4}-\d{2}-\d{2})\s{2,}(.+)$""")
    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun parse(text: String): List<JapaWithLog> {
        val lines = text.lines()
        val entries = mutableListOf<JapaWithLog>()

        var archivedSection = false
        var started = false
        var current: Builder? = null

        fun flush() {
            current?.build()?.let(entries::add)
            current = null
        }

        for ((index, raw) in lines.withIndex()) {
            val line = raw.trimEnd()
            val trimmed = line.trim()

            when (trimmed) {
                "ACTIVE JAPAS" -> {
                    flush(); started = true; archivedSection = false; continue
                }

                "ARCHIVED JAPAS" -> {
                    flush(); started = true; archivedSection = true; continue
                }

                "End of backup." -> {
                    flush(); break
                }
            }
            if (!started) continue

            // A japa's name is the line immediately above a thin rule.
            val next = lines.getOrNull(index + 1)?.trim()
            if (trimmed.isNotEmpty() && next != null && thinRule.matches(next)) {
                flush()
                current = Builder(name = trimmed, archived = archivedSection)
                continue
            }

            val builder = current ?: continue

            dayRow.find(line)?.let { match ->
                val date = match.groupValues[1]
                // The row is "date  count  malas", padded; the first column after the date is
                // the one that matters. Digits only, because the writer groups by locale.
                val count = match.groupValues[2].split(Regex("\\s{2,}")).firstOrNull().digits()
                if (count != null) builder.log += DailyCount(0, date, count)
                return@let
            }

            val separator = trimmed.indexOf(':')
            if (separator <= 0) continue
            val label = trimmed.substring(0, separator).trim()
            val value = trimmed.substring(separator + 1).trim()
            builder.field(label, value)
        }
        flush()

        return entries
    }

    private class Builder(val name: String, val archived: Boolean) {
        var count = 0L
        var malaSize = Japa.DEFAULT_MALA_SIZE
        var target = 0L
        var accent = 0
        var deity = ""
        var timeMs = 0L
        var favourite = false
        var createdAt = 0L
        var updatedAt = 0L
        var archivedAt: Long? = null
        val log = mutableListOf<DailyCount>()

        fun field(label: String, value: String) {
            when (label) {
                "Total count" -> count = value.firstNumber() ?: count
                "Japa per mala", "Beads per mala" -> malaSize =
                    value.firstNumber()?.toInt()?.coerceAtLeast(1) ?: malaSize

                "Lifetime target" ->
                    target = if (value.startsWith("none")) 0 else value.firstNumber() ?: 0

                // The human "2h 14m" line is for reading; this one is for restoring.
                "Time spent (ms)" -> timeMs = value.firstNumber() ?: timeMs
                "Accent colour" -> accent = value.firstNumber()?.toInt() ?: accent
                "Deity" -> deity = value
                "Favourite" -> favourite = value.equals("yes", ignoreCase = true)
                "Created" -> createdAt = value.asEpoch() ?: createdAt
                "Last counted" -> updatedAt = value.asEpoch() ?: updatedAt
                "Archived" -> archivedAt = value.asEpoch()
            }
        }

        fun build(): JapaWithLog? {
            if (name.isBlank()) return null
            val stamp = if (createdAt > 0) createdAt else System.currentTimeMillis()
            return JapaWithLog(
                japa = Japa(
                    id = 0,
                    name = name,
                    deity = deity,
                    count = count,
                    malaSize = malaSize,
                    lifetimeTarget = target,
                    backgroundUri = null,
                    archived = archived,
                    favourite = favourite,
                    accentIndex = accent,
                    totalTimeMs = timeMs,
                    createdAt = stamp,
                    updatedAt = if (updatedAt > 0) updatedAt else stamp,
                    archivedAt = if (archived) archivedAt ?: stamp else null,
                ),
                log = log.toList(),
            )
        }
    }

    /**
     * Strips grouping separators before parsing. The writer formats numbers for the device's
     * locale, which may put commas, dots or narrow spaces in them.
     */
    private fun String?.digits(): Long? {
        if (this == null) return null
        val digits = filter(Char::isDigit)
        return if (digits.isEmpty()) null else digits.toLongOrNull()
    }

    /**
     * The first number on a line, not every digit on it. "1,000,000 japa (9,259 malas)" carries
     * a second figure in brackets; taking all the digits would splice the two together.
     *
     * Split on `\s`, which in Java regex excludes the no-break and narrow spaces some locales
     * group numbers with, so a French "1 000 000" stays one token.
     */
    private fun String.firstNumber(): Long? =
        split(Regex("\\s+")).firstOrNull { token -> token.any(Char::isDigit) }.digits()

    private fun String.asEpoch(): Long? =
        runCatching { dayFormat.parse(this)?.time }.getOrNull()
}

/** What a restore did, for reporting back to whoever pressed the button. */
data class RestoreResult(val added: Int, val skipped: Int, val days: Int)
