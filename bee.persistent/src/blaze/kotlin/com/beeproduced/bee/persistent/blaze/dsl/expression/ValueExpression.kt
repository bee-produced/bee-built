package com.beeproduced.bee.persistent.blaze.dsl.expression

import com.beeproduced.bee.persistent.blaze.dsl.predicate.Predicate
import com.beeproduced.bee.persistent.blaze.meta.dsl.InlineValueUnwrappers

/**
 * @author Kacper Urbaniec
 * @version 2024-01-11
 */
interface ValueExpression<V : Any, T : Any> : Expression<T> {

  fun ValueExpression<V, T>.unwrapKey(): String

  fun eqInline(value: V?): Predicate = eq(unwrap(value))

  fun notEqInline(value: V?): Predicate = notEq(unwrap(value))
}

@Suppress("UNCHECKED_CAST")
private fun <V : Any, T : Any> ValueExpression<V, T>.unwrap(value: V?): T? =
  InlineValueUnwrappers.unwrap(value, unwrapKey())
