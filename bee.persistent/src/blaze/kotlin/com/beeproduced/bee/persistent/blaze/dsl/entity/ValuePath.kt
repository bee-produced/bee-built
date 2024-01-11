package com.beeproduced.bee.persistent.blaze.dsl.entity

import com.beeproduced.bee.persistent.blaze.dsl.expression.Expression
import com.beeproduced.bee.persistent.blaze.dsl.expression.ValueExpression

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-11
 */
class ValuePath<V : Any, T : Any>(
    private val path: String,
) : ValueExpression<V, T> {
    override fun Expression<T>.toExpressionString(): String = path
}