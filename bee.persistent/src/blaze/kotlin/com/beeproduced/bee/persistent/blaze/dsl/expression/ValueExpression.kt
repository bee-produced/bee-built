package com.beeproduced.bee.persistent.blaze.dsl.expression

import com.beeproduced.bee.persistent.blaze.dsl.predicate.Predicate

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-11
 */
interface ValueExpression<V : Any, T : Any> : Expression<T> {

    // https://stackoverflow.com/a/59716818/12347616

    @JvmName("eqValue")
    @Suppress("INAPPLICABLE_JVM_NAME")
    fun eq(value: V?): Predicate {
        // TODO: Unwrap!
        return eq("Foxtrot" as T)
    }
}