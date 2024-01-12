package com.beeproduced.bee.persistent.blaze.dsl.select

import com.beeproduced.bee.persistent.blaze.dsl.predicate.Predicate
import com.beeproduced.bee.persistent.blaze.dsl.predicate.builder.WhereAndBuilder
import com.beeproduced.bee.persistent.blaze.dsl.predicate.builder.WhereOrBuilder
import com.blazebit.persistence.BaseWhereBuilder

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-11
 */
class SelectQueryBuilder<T: Any> : SelectQuery<T> {
    private var where: Predicate? = null

    override fun where(predicate: Predicate): Selection<T> = apply {
        where = predicate
    }

    override fun SelectQuery<T>.whereAnd(vararg predicates: Predicate) = apply {
        where = WhereAndBuilder(predicates.toList())
    }

    override fun SelectQuery<T>.whereOr(vararg predicates: Predicate): Selection<T> = apply {
        where = WhereOrBuilder(predicates.toList())
    }

    fun <W : BaseWhereBuilder<W>> applyBuilder(builder: W): W {
        where?.run { return applyBuilder(builder) }
        // TODO order by
        return builder
    }
}