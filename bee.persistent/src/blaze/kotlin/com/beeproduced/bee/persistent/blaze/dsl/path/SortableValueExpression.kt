package com.beeproduced.bee.persistent.blaze.dsl.path

import com.beeproduced.bee.persistent.blaze.dsl.expression.ValueExpression

/**
 * @author Kacper Urbaniec
 * @version 2024-01-12
 */
interface SortableValueExpression<V : Any, T : Any> : ValueExpression<V, T>, SortableExpression<T>
