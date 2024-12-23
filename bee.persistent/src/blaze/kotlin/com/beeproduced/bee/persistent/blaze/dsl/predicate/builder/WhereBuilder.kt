package com.beeproduced.bee.persistent.blaze.dsl.predicate.builder

import com.beeproduced.bee.persistent.blaze.dsl.predicate.Predicate

/**
 * @author Kacper Urbaniec
 * @version 2024-01-12
 */
fun and(vararg predicates: Predicate): Predicate {
  return WhereAndBuilder(predicates.toList())
}

fun or(vararg predicates: Predicate): Predicate {
  return WhereOrBuilder(predicates.toList())
}
