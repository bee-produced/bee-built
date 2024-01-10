package com.beeproduced.bee.persistent.blaze.dsl.select

import com.beeproduced.bee.persistent.blaze.dsl.predicate.Predicate
import com.blazebit.persistence.BaseWhereBuilder

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-10
 */
class SelectQuery<T: Any> : SelectWhere<T>, Selection<T> {

    private var where: Predicate? = null

    override fun where(predicate: Predicate): Selection<T> = apply {
        where = predicate
    }

    internal fun <W : BaseWhereBuilder<W>> applyBuilder(builder: W): W {
        if (where != null) {
            return where.let { this.applyBuilder(builder) }
        }

        return builder
    }
}