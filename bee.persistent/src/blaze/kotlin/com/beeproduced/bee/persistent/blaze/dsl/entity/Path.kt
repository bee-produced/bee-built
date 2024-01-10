package com.beeproduced.bee.persistent.blaze.dsl.entity

import com.beeproduced.bee.persistent.blaze.dsl.expression.ExpressionTemplate

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-10
 */
class Path<T>(
    path: String,
) : ExpressionTemplate<T>(path)