package com.beeproduced.bee.persistent.blaze.dsl.expression

import com.beeproduced.bee.persistent.blaze.dsl.predicate.Predicate
import com.beeproduced.bee.persistent.blaze.meta.dsl.InlineValueUnwrappers

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-11
 */
interface ValueExpression<V : Any, T : Any> : Expression<T> {

    fun ValueExpression<V, T>.unwrapKey(): String

    // https://stackoverflow.com/a/59716818/12347616

    @JvmName("eqValue")
    @Suppress("INAPPLICABLE_JVM_NAME")
    fun eq(value: V?): Predicate {
        return eq(unwrap(value))
    }
}

@Suppress("UNCHECKED_CAST")
private fun <V : Any, T: Any> ValueExpression<V, T>.unwrap(value: V?): T? {
    return if (value != null) {
        InlineValueUnwrappers
            .unwrappers.getValue(unwrapKey())
            .unwrap(value) as T
    } else null
}