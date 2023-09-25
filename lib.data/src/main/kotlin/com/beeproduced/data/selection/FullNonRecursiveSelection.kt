package com.beeproduced.data.selection

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-01-30
 */
class FullNonRecursiveSelection(skips: Collection<SkipOver> = emptyList()) : DataSelection {
    override val skipOvers: SkipOverCollection = SkipOverCollection(skips)

    override fun contains(fieldGlobPattern: String): Boolean = true

    override fun subSelect(fieldGlobPattern: String): DataSelection = this
    override fun merge(vararg selections: SimpleSelection): DataSelection {
        return this
    }

    override fun merge(selections: Collection<SimpleSelection>): DataSelection {
        return this
    }
}