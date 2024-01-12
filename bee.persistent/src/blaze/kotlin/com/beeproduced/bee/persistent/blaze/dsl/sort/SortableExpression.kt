package com.beeproduced.bee.persistent.blaze.dsl.sort

import com.beeproduced.bee.persistent.blaze.dsl.expression.Expression

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-12
 */
interface SortableExpression <T : Any> : Expression<T> {

    fun asc(): Sort = OrderBySort(this, Sort.Order.ASC)

    fun desc(): Sort = OrderBySort(this, Sort.Order.DESC)
}