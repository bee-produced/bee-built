package com.beeproduced.bee.persistent.blaze.dsl.entity

import com.beeproduced.bee.persistent.blaze.dsl.expression.Expression
import com.beeproduced.bee.persistent.blaze.dsl.sort.SortableExpression

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