package com.beeproduced.bee.persistent.jpa.repository.dsl

import com.linecorp.kotlinjdsl.query.clause.select.MultiSelectClause
import com.linecorp.kotlinjdsl.query.clause.select.SingleSelectClause
import com.linecorp.kotlinjdsl.query.spec.OrderSpec
import com.linecorp.kotlinjdsl.query.spec.expression.ColumnSpec
import com.linecorp.kotlinjdsl.query.spec.expression.EntitySpec
import com.linecorp.kotlinjdsl.query.spec.expression.ExpressionSpec
import com.linecorp.kotlinjdsl.query.spec.predicate.PredicateSpec
import com.linecorp.kotlinjdsl.querydsl.CriteriaQueryDsl
import com.linecorp.kotlinjdsl.querydsl.from.Relation
import jakarta.persistence.criteria.JoinType

/**
 * @author Kacper Urbaniec
 * @version 2023-02-23
 */
class DummyCriteriaQueryDsl : CriteriaQueryDsl<Any> {

  private var limitClause = false
  val hasLimitClause
    get() = limitClause

  override fun limit(maxResults: Int) {
    limitClause = true
  }

  override fun limit(offset: Int, maxResults: Int) {
    limitClause = true
  }

  override fun maxResults(maxResults: Int) {
    limitClause = true
  }

  override fun offset(offset: Int) {
    limitClause = true
  }

  override fun <T, R> associate(
    left: EntitySpec<T>,
    right: EntitySpec<R>,
    relation: Relation<T, R?>,
    joinType: JoinType,
  ) {}

  override fun <T, R> fetch(
    left: EntitySpec<T>,
    right: EntitySpec<R>,
    relation: Relation<T, R?>,
    joinType: JoinType,
  ) {}

  override fun from(entity: EntitySpec<*>) {}

  override fun groupBy(columns: List<ExpressionSpec<*>>) {}

  override fun having(predicate: PredicateSpec) {}

  override fun hints(hints: Map<String, Any>) {}

  override fun <T, R> join(
    left: EntitySpec<T>,
    right: EntitySpec<R>,
    relation: Relation<T, R?>,
    joinType: JoinType,
  ) {}

  override fun <T> join(entity: EntitySpec<T>, predicate: PredicateSpec) {}

  private val orderByClauses = mutableListOf<OrderSpec>()

  val hasOrderByClause
    get() = orderByClauses.isNotEmpty()

  val orders: List<OrderSpec>
    get() = orderByClauses

  fun orderByColumns() = orderByClauses.map { it.getColumnSpec() }

  override fun orderBy(orders: List<OrderSpec>) {
    orderByClauses.addAll(orders)
  }

  override fun select(distinct: Boolean, expression: ExpressionSpec<Any>): SingleSelectClause<Any> {
    throw IllegalAccessException("Should not be invoked")
  }

  override fun select(
    distinct: Boolean,
    expressions: List<ExpressionSpec<*>>,
  ): MultiSelectClause<Any> {
    throw IllegalAccessException("Should not be invoked")
  }

  override fun sqlHints(hints: List<String>) {}

  override fun <P, C : P> treat(
    root: ColumnSpec<*>,
    parent: EntitySpec<P>,
    child: EntitySpec<C>,
    parentJoinType: JoinType,
  ) {}

  override fun where(predicate: PredicateSpec?) {}

  override fun whereAnd(predicates: List<PredicateSpec?>) {}

  override fun whereOr(predicates: List<PredicateSpec?>) {}
}
