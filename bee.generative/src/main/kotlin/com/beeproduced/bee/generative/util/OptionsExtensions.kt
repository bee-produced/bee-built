package com.beeproduced.bee.generative.util

import com.beeproduced.bee.generative.processor.Options

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-10-14
 */

fun Options.getOption(op: String): String {
    return getOrElse(op) { throw IllegalArgumentException("Option [$op] not provided") }
}