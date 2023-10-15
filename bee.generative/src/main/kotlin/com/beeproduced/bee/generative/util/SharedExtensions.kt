package com.beeproduced.bee.generative.util

import com.beeproduced.bee.generative.Shared

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-10-15
 */

@Suppress("UNCHECKED_CAST")
fun <T> Shared.getTyped(key: String): T {
    return getValue(key) as T
}