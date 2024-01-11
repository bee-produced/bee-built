package com.beeproduced.bee.persistent.blaze.meta.dsl

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-11
 */
fun interface InlineValueUnwrapper {
    fun unwrap(inline: Any): Any
}