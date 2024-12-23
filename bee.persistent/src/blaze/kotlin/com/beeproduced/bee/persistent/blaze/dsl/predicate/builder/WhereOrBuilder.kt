package com.beeproduced.bee.persistent.blaze.dsl.predicate.builder

import com.beeproduced.bee.persistent.blaze.dsl.predicate.Predicate
import com.beeproduced.bee.persistent.blaze.dsl.predicate.PredicateContainer
import com.blazebit.persistence.BaseWhereBuilder
import com.blazebit.persistence.WhereBuilder
import com.blazebit.persistence.WhereOrBuilder

/**
 * @author Kacper Urbaniec
 * @version 2024-01-12
 */
class WhereOrBuilder(private val predicates: List<Predicate>) : PredicateContainer {

  override fun <W : BaseWhereBuilder<W>> Predicate.applyBuilder(builder: W): W {
    if (builder is WhereOrBuilder<*>) {
      @Suppress("UNCHECKED_CAST")
      return iterateOverPredicates(builder) as W
    } else {
      builder as WhereBuilder<*>
      var b = builder.whereOr()
      b = iterateOverPredicates(b)
      @Suppress("UNCHECKED_CAST")
      return b.endOr() as W
    }
  }

  private fun <B> iterateOverPredicates(builder: WhereOrBuilder<B>): WhereOrBuilder<B> {
    var b = builder
    for (predicate in predicates) {
      if (predicate is WhereAndBuilder) {
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
