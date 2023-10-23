package com.beeproduced.lib.data

import com.beeproduced.data.selection.SimpleSelection
import com.beeproduced.data.selection.SimpleSelection.FieldNode
import org.junit.jupiter.api.Test
import kotlin.test.*

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-05-09
 */
class DataSelectionTest {

    // More about glob patterns & selection
    // * https://www.graphql-java.com/documentation/field-selection/
    // * https://docs.oracle.com/javase/8/docs/api/java/nio/file/FileSystem.html#getPathMatcher-java.lang.String-
    // * https://javapapers.com/java/glob-with-java-nio/

    @Test
    fun `check contains`() {
        val selection = SimpleSelection(
            setOf(
                FieldNode(
                    "a", setOf(
                        FieldNode("a2")
                    )
                ),
                FieldNode(
                    "b", setOf(
                        FieldNode("b2")
                    )
                )
            )
        )

        assertTrue(selection.contains("a"))
        assertTrue(selection.contains("a/a2"))
        assertTrue(selection.contains("b"))
        assertTrue(selection.contains("b/b2"))

        assertFalse(selection.contains("c"))
        assertFalse(selection.contains("a2"))
        assertFalse(selection.contains("b2"))
    }

    @Test
    fun `check contains with glob patterns`() {
        val selection = SimpleSelection(
            setOf(
                FieldNode(
                    "a", setOf(
                        FieldNode("a2")
                    )
                ),
                FieldNode(
                    "b", setOf(
                        FieldNode("b2")
                    )
                )
            )
        )

        assertTrue(selection.contains("a"))
        assertTrue(selection.contains("a/*"))
        assertTrue(selection.contains("a/**"))
        assertTrue(selection.contains("b"))
        assertTrue(selection.contains("b/*"))
        assertTrue(selection.contains("b/**"))
        assertTrue(selection.contains("{a,b}"))

        assertFalse(selection.contains("a/a2/*"))
        assertFalse(selection.contains("a/a2/**"))
        assertFalse(selection.contains("b/b2/*"))
        assertFalse(selection.contains("b/b2/**"))
        assertFalse(selection.contains("{c,d}"))
    }

    @Test
    fun `check subselect`() {
        val selection = SimpleSelection(
            setOf(
                FieldNode(
                    "a", setOf(
                        FieldNode("a2", setOf(FieldNode("a3")))
                    )
                ),
                FieldNode(
                    "b", setOf(
                        FieldNode("b2", setOf(FieldNode("b3")))
                    )
                )
            )
        )

        val selectionA = selection.subSelect("a")
        assertNotNull(selectionA)
        val selectionB = selection.subSelect("b")
        assertNotNull(selectionB)
        val selectionC = selection.subSelect("c")
        assertNull(selectionC)


        assertTrue(selectionA.contains("a2"))
        assertTrue(selectionA.contains("a2/a3"))
        assertTrue(selectionB.contains("b2"))
        assertTrue(selectionB.contains("b2/b3"))

        assertFalse(selectionA.contains("c"))
        assertFalse(selectionB.contains("c"))
        assertFalse(selectionA.contains("a"))
        assertFalse(selectionB.contains("b"))
    }

    @Test
    fun `check subselect with glob patterns`() {
        val selection = SimpleSelection(
            setOf(
                FieldNode(
                    "aaa", setOf(
                        FieldNode("a2", setOf(FieldNode("a3")))
                    )
                ),
                FieldNode(
                    "bbb", setOf(
                        FieldNode("b2", setOf(FieldNode("b3")))
                    )
                )
            )
        )

        val selectionA = selection.subSelect("a*")
        assertNotNull(selectionA)
        val selectionB = selection.subSelect("b*")
        assertNotNull(selectionB)
        val selectionC = selection.subSelect("c")
        assertNull(selectionC)
        val selectionAB = selection.subSelect("{aaa,bbb}")
        assertNotNull(selectionAB)


        assertTrue(selectionA.contains("a2"))
        assertTrue(selectionA.contains("a2/a3"))
        assertTrue(selectionB.contains("b2"))
        assertTrue(selectionB.contains("b2/b3"))
        assertTrue(selectionAB.contains("a2"))
        assertTrue(selectionAB.contains("a2/a3"))
        assertTrue(selectionAB.contains("b2"))
        assertTrue(selectionAB.contains("b2/b3"))

        assertFalse(selectionA.contains("c"))
        assertFalse(selectionB.contains("c"))
        assertFalse(selectionA.contains("a"))
        assertFalse(selectionB.contains("b"))
    }

    @Test
    fun `check contains after merge`() {
        val selection1 = SimpleSelection(
            setOf(
                FieldNode(
                    "a", setOf(
                        FieldNode("a2")
                    )
                )
            )
        )
        val selection2 = SimpleSelection(
            setOf(
                FieldNode(
                    "b", setOf(
                        FieldNode("b2")
                    )
                )
            )
        )
        val selection = selection1.merge(listOf(selection2))

        assertTrue(selection.contains("a"))
        assertTrue(selection.contains("a/a2"))
        assertTrue(selection.contains("b"))
        assertTrue(selection.contains("b/b2"))

        assertFalse(selection.contains("c"))
        assertFalse(selection.contains("a2"))
        assertFalse(selection.contains("b2"))
    }

    @Test
    fun `check contains with glob patterns after merge`() {
        val selection1 = SimpleSelection(
            setOf(
                FieldNode(
                    "a", setOf(
                        FieldNode("a2")
                    )
                )
            )
        )
        val selection2 = SimpleSelection(
            setOf(
                FieldNode(
                    "b", setOf(
                        FieldNode("b2")
                    )
                )
            )
        )
        val selection = selection1.merge(listOf(selection2))

        assertTrue(selection.contains("a"))
        assertTrue(selection.contains("a/*"))
        assertTrue(selection.contains("a/**"))
        assertTrue(selection.contains("b"))
        assertTrue(selection.contains("b/*"))
        assertTrue(selection.contains("b/**"))
        assertTrue(selection.contains("{a,b}"))

        assertFalse(selection.contains("a/a2/*"))
        assertFalse(selection.contains("a/a2/**"))
        assertFalse(selection.contains("b/b2/*"))
        assertFalse(selection.contains("b/b2/**"))
        assertFalse(selection.contains("{c,d}"))
    }

    @Test
    fun `check subselect after merge`() {
        val selection1 = SimpleSelection(
            setOf(
                FieldNode(
                    "a", setOf(
                        FieldNode("a2", setOf(FieldNode("a3")))
                    )
                )
            )
        )
        val selection2 = SimpleSelection(
            setOf(
                FieldNode(
                    "b", setOf(
                        FieldNode("b2", setOf(FieldNode("b3")))
                    )
                )
            )
        )
        val selection = selection1.merge(listOf(selection2))

        val selectionA = selection.subSelect("a")
        assertNotNull(selectionA)
        val selectionB = selection.subSelect("b")
        assertNotNull(selectionB)
        val selectionC = selection.subSelect("c")
        assertNull(selectionC)


        assertTrue(selectionA.contains("a2"))
        assertTrue(selectionA.contains("a2/a3"))
        assertTrue(selectionB.contains("b2"))
        assertTrue(selectionB.contains("b2/b3"))

        assertFalse(selectionA.contains("c"))
        assertFalse(selectionB.contains("c"))
        assertFalse(selectionA.contains("a"))
        assertFalse(selectionB.contains("b"))
    }

    @Test
    fun `check subselect with glob patterns after merge`() {
        val selection1 = SimpleSelection(
            setOf(
                FieldNode(
                    "aaa", setOf(
                        FieldNode("a2", setOf(FieldNode("a3")))
                    )
                )
            )
        )
        val selection2 = SimpleSelection(
            setOf(
                FieldNode(
                    "bbb", setOf(
                        FieldNode("b2", setOf(FieldNode("b3")))
                    )
                )
            )
        )
        val selection = selection1.merge(listOf(selection2))

        val selectionA = selection.subSelect("a*")
        assertNotNull(selectionA)
        val selectionB = selection.subSelect("b*")
        assertNotNull(selectionB)
        val selectionC = selection.subSelect("c")
        assertNull(selectionC)
        val selectionAB = selection.subSelect("{aaa,bbb}")
        assertNotNull(selectionAB)


        assertTrue(selectionA.contains("a2"))
        assertTrue(selectionA.contains("a2/a3"))
        assertTrue(selectionB.contains("b2"))
        assertTrue(selectionB.contains("b2/b3"))
        assertTrue(selectionAB.contains("a2"))
        assertTrue(selectionAB.contains("a2/a3"))
        assertTrue(selectionAB.contains("b2"))
        assertTrue(selectionAB.contains("b2/b3"))

        assertFalse(selectionA.contains("c"))
        assertFalse(selectionB.contains("c"))
        assertFalse(selectionA.contains("a"))
        assertFalse(selectionB.contains("b"))
    }

    @Test
    fun `check subselect with glob patterns after merge with same initial fields`() {
        val selection1 = SimpleSelection(
            setOf(
                FieldNode(
                    "aaa", setOf(
                        FieldNode("a2", setOf(FieldNode("a3")))
                    )
                ),
                FieldNode(
                    "bbb", setOf(
                        FieldNode("c2", setOf(FieldNode("c3")))
                    )
                )
            )
        )
        val selection2 = SimpleSelection(
            setOf(
                FieldNode(
                    "bbb", setOf(
                        FieldNode("b2", setOf(FieldNode("b3")))
                    )
                )
            )
        )
        val selection = selection1.merge(listOf(selection2))

        val selectionA = selection.subSelect("a*")
        assertNotNull(selectionA)
        val selectionBC = selection.subSelect("b*")
        assertNotNull(selectionBC)
        val selectionABC = selection.subSelect("{aaa,bbb}")
        assertNotNull(selectionABC)


        assertTrue(selectionA.contains("a2"))
        assertTrue(selectionA.contains("a2/a3"))
        assertTrue(selectionBC.contains("b2"))
        assertTrue(selectionBC.contains("b2/b3"))
        assertTrue(selectionBC.contains("c2"))
        assertTrue(selectionBC.contains("c2/c3"))
        assertTrue(selectionABC.contains("a2"))
        assertTrue(selectionABC.contains("a2/a3"))
        assertTrue(selectionABC.contains("b2"))
        assertTrue(selectionABC.contains("b2/b3"))
        assertTrue(selectionABC.contains("c2"))
        assertTrue(selectionABC.contains("c2/c3"))
    }
}