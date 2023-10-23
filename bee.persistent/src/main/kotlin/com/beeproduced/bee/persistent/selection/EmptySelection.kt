package com.beeproduced.bee.persistent.selection

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-01-30
 */
class EmptySelection : DataSelection {
    override val skipOvers = SkipOverCollection(emptyList())

    override fun contains(fieldGlobPattern: String): Boolean = false

    override fun subSelect(fieldGlobPattern: String): DataSelection? = null
    override fun merge(vararg selections: SimpleSelection): DataSelection {
        return merge(selections.asList())
    }

    override fun merge(selections: Collection<SimpleSelection>): DataSelection {
        if (selections.isEmpty()) return this
        val selection = SimpleSelection(setOf())
        return selection.merge(selections)
    }
}