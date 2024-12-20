package com.beeproduced.bee.persistent.jpa.repository.dsl

import com.linecorp.kotlinjdsl.query.spec.expression.EntitySpec
import com.linecorp.kotlinjdsl.query.spec.predicate.PredicateSpec
import com.linecorp.kotlinjdsl.querydsl.CriteriaDeleteQueryDsl
import com.linecorp.kotlinjdsl.querydsl.from.Relation
import jakarta.persistence.criteria.JoinType

/**
 * Used to test if `dsl: CriteriaDeleteQueryDsl.() -> Unit` predicate contains a “WHERE” clause.
 *
 * @author Kacper Urbaniec
 * @version 2023-02-17
 */
class DummyCriteriaDeleteQueryDsl : CriteriaDeleteQueryDsl {

  private var whereClause = false
  val hasWhereClause
    get() = whereClause

  override fun where(predicate: PredicateSpec?) {
    if (predicate != null) whereClause = true
  }

  override fun whereAnd(predicates: List<PredicateSpec?>) {
    if (predicates.isNotEmpty()) whereClause = true
  }

  override fun whereOr(predicates: List<PredicateSpec?>) {
    if (predicates.isNotEmpty()) whereClause = true
  }

  override fun <T, R> associate(
    left: EntitySpec<T>,
    right: EntitySpec<R>,
    relation: Relation<T, R?>,
    joinType: JoinType,
  ) {}

  override fun hints(hints: Map<String, Any>) {}

  override fun sqlHints(hints: List<String>) {}
}
