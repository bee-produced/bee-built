package com.beeproduced.bee.persistent.blaze.dsl.predicate.builder

import com.beeproduced.bee.persistent.blaze.dsl.predicate.Predicate
import com.beeproduced.bee.persistent.blaze.dsl.predicate.PredicateContainer
import com.blazebit.persistence.BaseWhereBuilder

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-12
 */
class WhereAndBuilder(
    private val predicates: List<Predicate>
) : PredicateContainer {
    override fun <W : BaseWhereBuilder<W>> Predicate.applyBuilder(builder: W): W {
        var b = builder
        for (predicate in predicates) {
            b = predicate.run { applyBuilder(b) }
        }
        return b
    }
}