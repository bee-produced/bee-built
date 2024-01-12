package com.beeproduced.bee.persistent.blaze.dsl.expression.builder

import com.beeproduced.bee.persistent.blaze.dsl.expression.Expression
import com.beeproduced.bee.persistent.blaze.dsl.expression.function.string.LowerExpression

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-12
 */

fun lower(expression: Expression<String>): Expression<String> = LowerExpression(expression)