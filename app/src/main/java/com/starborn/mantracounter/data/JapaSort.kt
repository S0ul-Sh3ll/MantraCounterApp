package com.starborn.mantracounter.data

/**
 * How the main list is ordered. Applied in memory rather than in SQL: the list is small, and one
 * `ORDER BY` per option would mean five near-identical queries to keep in step.
 */
enum class JapaSort(val label: String) {
    Favourites("Favourites first"),
    NameAsc("Name A–Z"),
    NameDesc("Name Z–A"),
    CreatedNewest("Newest first"),
    CreatedOldest("Oldest first"),
    DeityAsc("Deity A–Z"),
    DeityDesc("Deity Z–A");

    companion object {
        val DEFAULT = Favourites

        fun fromName(value: String?): JapaSort =
            entries.firstOrNull { it.name == value } ?: DEFAULT
    }
}

fun List<Japa>.applySort(sort: JapaSort): List<Japa> = when (sort) {
    // Within each group the manual order is kept, so favouriting does not otherwise shuffle
    // the list out from under someone.
    JapaSort.Favourites -> sortedWith(
        compareByDescending<Japa> { it.favourite }.thenBy { it.sortOrder }.thenBy { it.id }
    )

    JapaSort.NameAsc -> sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    JapaSort.NameDesc -> sortedWith(
        compareByDescending<Japa, String>(String.CASE_INSENSITIVE_ORDER) { it.name }
    )

    JapaSort.CreatedNewest -> sortedWith(compareByDescending<Japa> { it.createdAt }.thenByDescending { it.id })
    JapaSort.CreatedOldest -> sortedWith(compareBy<Japa> { it.createdAt }.thenBy { it.id })

    // Japas with no deity named go last either way — sorting by a field they do not have should
    // not push them to the top of an ascending list.
    JapaSort.DeityAsc -> sortedWith(
        compareBy<Japa> { it.deity.isBlank() }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.deity }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
    )

    JapaSort.DeityDesc -> sortedWith(
        compareBy<Japa> { it.deity.isBlank() }
            .thenByDescending(String.CASE_INSENSITIVE_ORDER) { it.deity }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
    )
}

/** The folder a japa with no deity named is filed under. */
const val UNCATEGORIZED = "Uncategorized"

/**
 * Files japas into folders by deity for the archive. Named folders sort alphabetically and
 * case-insensitively; [UNCATEGORIZED] always sits last, however it would otherwise sort.
 */
fun List<Japa>.groupByDeity(): List<Pair<String, List<Japa>>> =
    groupBy { it.deity.trim().ifBlank { UNCATEGORIZED } }
        .toList()
        .sortedWith(
            compareBy<Pair<String, List<Japa>>> { it.first == UNCATEGORIZED }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.first }
        )
