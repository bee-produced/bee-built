package com.beeproduced.bee.persistent.blaze.dsl.path

import com.beeproduced.bee.persistent.blaze.dsl.expression.Expression

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-10
 */
data class Path<T : Any>(
    private val path: String,
) : SortableExpression<T> {
    override fun Expression<T>.toExpressionString(): String = path

}