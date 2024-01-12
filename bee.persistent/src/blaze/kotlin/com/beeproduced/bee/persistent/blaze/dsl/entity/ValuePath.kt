package com.beeproduced.bee.persistent.blaze.dsl.entity

import com.beeproduced.bee.persistent.blaze.dsl.expression.Expression
import com.beeproduced.bee.persistent.blaze.dsl.expression.ValueExpression
import com.beeproduced.bee.persistent.blaze.dsl.sort.SortableExpression
import kotlin.reflect.KClass

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-11
 */
class ValuePath<V : Any, T : Any>(
    private val path: String,
    private val qualifiedName: String
) : ValueExpression<V, T>, SortableExpression<T> {

    constructor(path: String, kClass: KClass<V>) : this(path, requireNotNull(kClass.qualifiedName))

    override fun Expression<T>.toExpressionString(): String = path
    override fun ValueExpression<V, T>.unwrapKey(): String = qualifiedName
}