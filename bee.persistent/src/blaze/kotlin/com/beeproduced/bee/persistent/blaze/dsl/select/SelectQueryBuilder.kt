package com.beeproduced.bee.persistent.blaze.dsl.select

import com.blazebit.persistence.WhereBuilder as BlazeWhereBuilder
import com.blazebit.persistence.WhereOrBuilder as BlazeWhereOrBuilder
import com.beeproduced.bee.persistent.blaze.dsl.predicate.Predicate
import com.beeproduced.bee.persistent.blaze.dsl.predicate.PredicateContainer
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
        where = WhereAnd(predicates.toList())
    }

    override fun SelectQuery<T>.whereOr(vararg predicates: Predicate): Selection<T> = apply {
        where = WhereOrBuilder(predicates.toList())
    }

    // TODO Extract?
    fun <W : BaseWhereBuilder<W>> applyBuilder(builder: W): W {
        where?.run {
            return applyBuilder(builder)
        }

        return builder
    }
}

// internal interface WhereBuilder {
//     fun <W : BaseWhereBuilder<W>> applyBuilder(builder: W): W
// }


internal class WhereOrBuilder(
    private val predicates: List<Predicate>
) : PredicateContainer {

    override fun <W : BaseWhereBuilder<W>> Predicate.applyBuilder(builder: W): W {
        if (builder is BlazeWhereOrBuilder<*>) {
            @Suppress("UNCHECKED_CAST")
            return iterateOverPredicates(builder) as W
        } else {
            builder as BlazeWhereBuilder<*>
            var b = builder.whereOr()
            b = iterateOverPredicates(b)
            @Suppress("UNCHECKED_CAST")
            return b.endOr() as W
        }
    }

    private fun <B> iterateOverPredicates(builder: BlazeWhereOrBuilder<B>): BlazeWhereOrBuilder<B> {
        var b = builder
        for (predicate in predicates) {
            if (predicate is WhereAnd) {
                var orB = b.whereAnd()
                orB = predicate.run { applyBuilder(orB) }
                b = orB.endAnd()
            } else {
                b = predicate.run { applyBuilder(b) }
            }
        }
        return b
    }
}

internal class WhereAnd(
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


fun kek(str: String) {
    if (str == "a" || (str == "b" || str == "c")) {
        println("baum")
    }
}

// internal class SingleWhereBuilder(
//     private val predicate: Predicate
// ) : WhereBuilder {
//     override fun <W : BaseWhereBuilder<W>> applyBuilder(builder: W): W {
//         return predicate.run { applyBuilder(builder) }
//     }
// }