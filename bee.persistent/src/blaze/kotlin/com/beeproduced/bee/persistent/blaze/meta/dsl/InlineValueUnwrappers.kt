package com.beeproduced.bee.persistent.blaze.meta.dsl

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-11
 */

typealias FullyQualifiedName = String

object InlineValueUnwrappers {
    val unwrappers: MutableMap<FullyQualifiedName, InlineValueUnwrapper> = mutableMapOf()
}