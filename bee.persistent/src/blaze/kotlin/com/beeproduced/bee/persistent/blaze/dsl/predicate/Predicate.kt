package com.beeproduced.bee.persistent.blaze.dsl.predicate

import com.beeproduced.bee.persistent.blaze.dsl.expression.Expression
import com.blazebit.persistence.BaseWhereBuilder

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-10
 */
interface Predicate {
    // Is only visible via scope like `with`
    // https://medium.com/@wada811/kotlintips-private-protected-and-internal-methods-in-interfaces-9504df2f0289
    fun Predicate.expression(): Expression<*>
    fun <W : BaseWhereBuilder<W>> Predicate.applyBuilder(builder: W): W
}