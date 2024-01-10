package com.beeproduced.bee.persistent.blaze.dsl.expression

import com.beeproduced.bee.persistent.blaze.dsl.predicate.Predicate

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-10
 */
interface Expression<T> {

    fun Expression<T>.toExpressionString(): String

    fun equal(value: T): Predicate
}
