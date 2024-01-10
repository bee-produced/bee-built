package com.beeproduced.bee.persistent.blaze.dsl.expression

import com.beeproduced.bee.persistent.blaze.dsl.predicate.EqualValuePredicate
import com.beeproduced.bee.persistent.blaze.dsl.predicate.Predicate

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-10
 */


abstract class ExpressionTemplate<T>(
    private val path: String
) : Expression<T> {

    override fun Expression<T>.toExpressionString(): String = path

    override fun equal(value: T): Predicate {
        return EqualValuePredicate(this, value)
    }

}