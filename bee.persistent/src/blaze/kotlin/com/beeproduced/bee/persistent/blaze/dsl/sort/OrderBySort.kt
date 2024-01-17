package com.beeproduced.bee.persistent.blaze.dsl.sort

import com.beeproduced.bee.persistent.blaze.dsl.expression.Expression
import com.blazebit.persistence.OrderByBuilder

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-12
 */
data class OrderBySort<T : Any>(
    val expression: Expression<T>,
    val order: Sort.Order
) : Sort {
    override fun <W : OrderByBuilder<W>> Sort.applyBuilder(builder: W): W {
        val expressionString = expression.run { toExpressionString() }
        return builder.orderBy(expressionString, order.sort)
    }
}