package com.beeproduced.bee.persistent.blaze.dsl.expression

import com.beeproduced.bee.persistent.blaze.dsl.expression.function.string.LowerExpression

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-11
 */
object StringFunctions {

    fun lower(expression: Expression<String>) = LowerExpression(expression)

    // TODO: Implement remaining functions https://persistence.blazebit.com/documentation/1.6/core/manual/en_US/index.html#string-functions

}