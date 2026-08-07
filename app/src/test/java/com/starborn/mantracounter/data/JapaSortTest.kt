package com.starborn.mantracounter.data

import org.junit.Assert.assertEquals
import org.junit.Test

class JapaSortTest {

    private fun japa(
        id: Long,
        name: String,
        created: Long,
        favourite: Boolean = false,
        deity: String = "",
        order: Int = id.toInt(),
    ) = Japa(
        id = id,
        name = name,
        deity = deity,
        createdAt = created,
        favourite = favourite,
        sortOrder = order,
    )

    private val japas = listOf(
        japa(1, "gayatri mantra", created = 300),
        japa(2, "Om Namah Shivaya", created = 100, favourite = true),
        japa(3, "Hare Krishna", created = 200),
        japa(4, "asatoma sadgamaya", created = 400, favourite = true),
    )

    private fun names(sort: JapaSort) = japas.applySort(sort).map { it.name }

    @Test
    fun `name sorting ignores case`() {
        assertEquals(
            listOf("asatoma sadgamaya", "gayatri mantra", "Hare Krishna", "Om Namah Shivaya"),
            names(JapaSort.NameAsc),
        )
        assertEquals(
            listOf("Om Namah Shivaya", "Hare Krishna", "gayatri mantra", "asatoma sadgamaya"),
            names(JapaSort.NameDesc),
        )
    }

    @Test
    fun `created sorting runs both ways`() {
        assertEquals(
            listOf("asatoma sadgamaya", "gayatri mantra", "Hare Krishna", "Om Namah Shivaya"),
            names(JapaSort.CreatedNewest),
        )
        assertEquals(
            listOf("Om Namah Shivaya", "Hare Krishna", "gayatri mantra", "asatoma sadgamaya"),
            names(JapaSort.CreatedOldest),
        )
    }

    @Test
    fun `favourites float to the top without reshuffling the rest`() {
        assertEquals(
            listOf("Om Namah Shivaya", "asatoma sadgamaya", "gayatri mantra", "Hare Krishna"),
            names(JapaSort.Favourites),
        )
    }

    @Test
    fun `deity sorting ignores case and runs both ways`() {
        val withDeities = listOf(
            japa(1, "Maha Mantra", created = 1, deity = "krishna"),
            japa(2, "Panchakshari", created = 2, deity = "Shiva"),
            japa(3, "Gayatri", created = 3, deity = "Devi"),
        )
        assertEquals(
            listOf("Devi", "krishna", "Shiva"),
            withDeities.applySort(JapaSort.DeityAsc).map { it.deity },
        )
        assertEquals(
            listOf("Shiva", "krishna", "Devi"),
            withDeities.applySort(JapaSort.DeityDesc).map { it.deity },
        )
    }

    @Test
    fun `japas with no deity go last in both directions`() {
        val mixed = listOf(
            japa(1, "Unnamed", created = 1),
            japa(2, "Panchakshari", created = 2, deity = "Shiva"),
            japa(3, "Also unnamed", created = 3),
            japa(4, "Maha Mantra", created = 4, deity = "Krishna"),
        )
        assertEquals(
            listOf("Krishna", "Shiva", "", ""),
            mixed.applySort(JapaSort.DeityAsc).map { it.deity },
        )
        assertEquals(
            listOf("Shiva", "Krishna", "", ""),
            mixed.applySort(JapaSort.DeityDesc).map { it.deity },
        )
        // Within the no-deity group the order is by name, not arbitrary.
        assertEquals(
            listOf("Also unnamed", "Unnamed"),
            mixed.applySort(JapaSort.DeityAsc).filter { it.deity.isBlank() }.map { it.name },
        )
    }

    @Test
    fun `sorting never loses or duplicates a japa`() {
        JapaSort.entries.forEach { sort ->
            val sorted = japas.applySort(sort)
            assertEquals(japas.size, sorted.size)
            assertEquals(japas.map { it.id }.toSet(), sorted.map { it.id }.toSet())
        }
    }

    @Test
    fun `archive folders are named by deity, alphabetically`() {
        val archived = listOf(
            japa(1, "Panchakshari", created = 1, deity = "shiva"),
            japa(2, "Maha Mantra", created = 2, deity = "Krishna"),
            japa(3, "Gayatri", created = 3, deity = "Devi"),
        )
        assertEquals(
            listOf("Devi", "Krishna", "shiva"),
            archived.groupByDeity().map { it.first },
        )
    }

    @Test
    fun `japas with no deity are filed under Uncategorized, last`() {
        val archived = listOf(
            japa(1, "Nameless", created = 1),
            japa(2, "Panchakshari", created = 2, deity = "Shiva"),
            japa(3, "Blank spaces", created = 3, deity = "   "),
            japa(4, "Maha Mantra", created = 4, deity = "Krishna"),
        )
        val folders = archived.groupByDeity()
        assertEquals(listOf("Krishna", "Shiva", UNCATEGORIZED), folders.map { it.first })
        // A deity of only whitespace counts as none.
        assertEquals(2, folders.last().second.size)
    }

    @Test
    fun `folders hold every japa exactly once`() {
        val archived = listOf(
            japa(1, "A", created = 1, deity = "Shiva"),
            japa(2, "B", created = 2, deity = "Shiva"),
            japa(3, "C", created = 3),
        )
        val folders = archived.groupByDeity()
        assertEquals(2, folders.size)
        assertEquals(archived.size, folders.sumOf { it.second.size })
        assertEquals(archived.map { it.id }.toSet(), folders.flatMap { it.second }.map { it.id }.toSet())
    }

    @Test
    fun `an unknown stored preference falls back to the default`() {
        assertEquals(JapaSort.DEFAULT, JapaSort.fromName(null))
        assertEquals(JapaSort.DEFAULT, JapaSort.fromName("SomethingRemoved"))
        assertEquals(JapaSort.NameDesc, JapaSort.fromName("NameDesc"))
    }
}
