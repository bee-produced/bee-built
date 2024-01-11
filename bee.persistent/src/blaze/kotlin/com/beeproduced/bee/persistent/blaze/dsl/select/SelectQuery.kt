package com.beeproduced.bee.persistent.blaze.dsl.select

import com.beeproduced.bee.persistent.blaze.dsl.predicate.Predicate
import com.blazebit.persistence.BaseWhereBuilder

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-10
 */
interface SelectQuery<T : Any> :
    SelectWhere<T>,

    Selection<T>
{
    fun SelectQuery<T>.whereAnd(vararg predicates: Predicate): Selection<T>

    fun SelectQuery<T>.whereOr(vararg predicates: Predicate): Selection<T>

}

fun and(vararg predicates: Predicate): Predicate {
    return WhereAnd(predicates.toList())
}

fun or(vararg predicates: Predicate): Predicate {
    return WhereOrBuilder(predicates.toList())
}