package com.beeproduced.bee.persistent.selection

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-01-30
 */
interface DataSelection {
    val skipOvers: SkipOverCollection

    // This will return true if the field selection set matches a specified "glob" pattern
    // matching ie the glob pattern matching supported by java.nio.file.FileSystem.getPathMatcher.
    fun contains(fieldGlobPattern: String): Boolean

    // Get selector for specified field if existing
    fun subSelect(fieldGlobPattern: String): DataSelection?

    fun merge(vararg selections: SimpleSelection): DataSelection

    fun merge(selections: Collection<SimpleSelection>): DataSelection
}