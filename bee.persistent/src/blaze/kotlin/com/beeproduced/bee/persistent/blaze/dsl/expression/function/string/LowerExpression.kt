package com.beeproduced.bee.persistent.blaze.dsl.expression.function.string

import com.beeproduced.bee.persistent.blaze.dsl.expression.Expression

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-11
 */
internal class LowerExpression(
    private val expression: Expression<String>
) : Expression<String> {
    override fun Expression<String>.toExpressionString(): String
        = "LOWER(${expression.run { toExpressionString() }})"
}