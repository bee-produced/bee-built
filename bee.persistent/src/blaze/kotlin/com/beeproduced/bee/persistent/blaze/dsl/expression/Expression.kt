package com.beeproduced.bee.persistent.blaze.dsl.expression

import com.beeproduced.bee.persistent.blaze.dsl.predicate.EqualValuePredicate
import com.beeproduced.bee.persistent.blaze.dsl.predicate.NotEqualValuePredicate
import com.beeproduced.bee.persistent.blaze.dsl.predicate.Predicate

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-10
 */
interface Expression<T : Any> {

    fun Expression<T>.toExpressionString(): String

    fun eq(value: T?): Predicate = EqualValuePredicate(this, value)

    fun notEq(value: T?): Predicate = NotEqualValuePredicate(this, value)

    // TODO: Implement most functions of RestrictionBuilder
}
