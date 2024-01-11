package com.beeproduced.bee.persistent.blaze.dsl.entity

import com.beeproduced.bee.persistent.blaze.dsl.expression.Expression
import com.beeproduced.bee.persistent.blaze.dsl.expression.ValueExpression
import kotlin.reflect.KClass

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-11
 */
class ValuePath<V : Any, T : Any>(
    private val path: String,
) : ValueExpression<V, T> {

    constructor(path: String, kclass: KClass<V>) : this(path)

    override fun Expression<T>.toExpressionString(): String = path
}