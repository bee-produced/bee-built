package com.beeproduced.bee.persistent.blaze.dsl.select

import com.beeproduced.bee.persistent.blaze.dsl.predicate.Predicate
import com.beeproduced.bee.persistent.blaze.dsl.predicate.builder.WhereAndBuilder
import com.beeproduced.bee.persistent.blaze.dsl.predicate.builder.WhereOrBuilder
import com.beeproduced.bee.persistent.blaze.dsl.sort.Sort
import com.blazebit.persistence.BaseWhereBuilder
import com.blazebit.persistence.CriteriaBuilder
import com.blazebit.persistence.FullQueryBuilder

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-11
 */
class SelectQueryBuilder<T: Any> : SelectQuery<T> {
    private var where: Predicate? = null
    private var orderBy: List<Sort>? = null

    override fun where(predicate: Predicate): SelectOrderBy<T> = apply {
        where = predicate
    }

    override fun whereAnd(vararg predicates: Predicate): SelectOrderBy<T> = apply {
        where = WhereAndBuilder(predicates.toList())
    }

    override fun whereOr(vararg predicates: Predicate): SelectOrderBy<T> = apply {
        where = WhereOrBuilder(predicates.toList())
    }

    override fun orderBy(vararg sorts: Sort): Selection<T> = apply {
        orderBy = sorts.toList()
    }

    fun applyBuilder(builder: CriteriaBuilder<T>): CriteriaBuilder<T> {
        var b: CriteriaBuilder<T> = builder
        where?.run { b = applyBuilder(b) }
        orderBy?.let { sorts ->
            for (sort in sorts)
                sort.run { b = applyBuilder(b) }
        }
        return b
    }
}