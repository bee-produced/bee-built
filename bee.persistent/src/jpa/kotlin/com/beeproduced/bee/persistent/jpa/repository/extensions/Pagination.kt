package com.beeproduced.bee.persistent.jpa.repository.extensions

import com.beeproduced.bee.persistent.jpa.entity.DataEntity
import com.beeproduced.bee.persistent.jpa.repository.BaseDataRepository
import com.beeproduced.bee.persistent.selection.DataSelection
import com.beeproduced.bee.persistent.selection.EmptySelection
import com.linecorp.kotlinjdsl.query.spec.expression.ColumnSpec
import com.linecorp.kotlinjdsl.query.spec.predicate.GreaterThanValueSpec
import com.linecorp.kotlinjdsl.query.spec.predicate.LessThanValueSpec
import com.linecorp.kotlinjdsl.query.spec.predicate.PredicateSpec
import com.linecorp.kotlinjdsl.querydsl.CriteriaQueryDsl
import org.slf4j.LoggerFactory

/**
 * @author Kacper Urbaniec
 * @version 2023-03-20
 */
class Pagination<V, CV, C, W>(
  private val repository: BaseDataRepository<V, *>,
  private val orderBy: ColumnSpec<CV>,
  private val cursor: Cursor<V, CV, C>,
  private val where: (W) -> PredicateSpec = { PredicateSpec.empty },
  private val dsl: CriteriaQueryDsl<*>.() -> Unit = {},
) where V : DataEntity<V>, CV : Comparable<CV> {
  private val logger = LoggerFactory.getLogger(this::class.java)

  fun forward(
    first: Int,
    after: C?,
    whereValues: W,
    selection: DataSelection = EmptySelection(),
  ): PaginationResult<V, C> {
    return handleQuery(first, after, whereValues, where, true, selection)
  }

  fun backward(
    last: Int,
    before: C?,
    whereValues: W,
    selection: DataSelection = EmptySelection(),
  ): PaginationResult<V, C> {
    return handleQuery(last, before, whereValues, where, false, selection)
  }

  private fun handleQuery(
    limit: Int,
    cursor: C?,
    whereValues: W,
    queryWhere: (W) -> PredicateSpec,
    ascending: Boolean,
    selection: DataSelection,
  ): PaginationResult<V, C> {
    if (limit <= 0) return PaginationResult()

    val baseWhereSpec = queryWhere(whereValues)

    val selectedElementCount =
      repository.count {
        this.apply(dsl)
        this.where(baseWhereSpec)
      }
    if (selectedElementCount == 0L) return PaginationResult()

    return if (cursor == null) {
      initialQuery(selectedElementCount, limit, baseWhereSpec, ascending, selection)
    } else {
      cursorQuery(selectedElementCount, limit, cursor, baseWhereSpec, ascending, selection)
    }
  }

  private fun countElements(direction: PredicateSpec, baseWhereSpec: PredicateSpec): Long {
    return repository.count {
      this.apply(dsl)
      this.whereAnd(baseWhereSpec, direction)
    }
  }

  private fun cursorQuery(
    selectedElementCount: Long,
    limit: Int,
    cursor: C,
    baseWhereSpec: PredicateSpec,
    ascending: Boolean,
    selection: DataSelection,
  ): PaginationResult<V, C> {
    val cursorValue = this.cursor.decode(cursor)

    val beforeSpec = LessThanValueSpec(orderBy, cursorValue, false)
    val afterSpec = GreaterThanValueSpec(orderBy, cursorValue, false)

    val inclusiveBeforeSpec = beforeSpec.copy(inclusive = ascending)
    val inclusiveAfterSpec = afterSpec.copy(inclusive = !ascending)

    val elementsBeforeCursor = countElements(inclusiveBeforeSpec, baseWhereSpec)
    val elementsAfterCursor = countElements(inclusiveAfterSpec, baseWhereSpec)

    // Check if query with given cursor was valid
    // If so, pivot element should be included in either before/after cursor queries
    // resulting in being the same amount as all elements
    // Why include pivot?
    // When querying one element with the cursor of the first element
    // the before count would be 0 in ascending order
    // In this case, it is impossible to determine if there is a previous page or not!
    val elementsToQuery = (selectedElementCount - elementsBeforeCursor - elementsAfterCursor) == 0L
    if (!elementsToQuery) return PaginationResult()

    // Also, disallow using cursor pagination before first or last element depending on order
    // Can only happen if cursor is manually created (e.g., encoding lower timestamp than first
    // element)
    if ((ascending && elementsBeforeCursor == 0L) || (!ascending && elementsAfterCursor == 0L))
      return PaginationResult()

    val queriedElements =
      repository
        .select(selection) {
          this.apply(dsl)
          this.whereAnd(baseWhereSpec, if (ascending) afterSpec else beforeSpec)
          if (ascending) this.orderBy(orderBy.asc()) else this.orderBy(orderBy.desc())
          this.limit(limit)
        }
        .map { entity -> Edge(entity, this.cursor.encode(entity)) }

    val pageInfo =
      PageInfo(
        startCursor = queriedElements.firstOrNull()?.cursor,
        endCursor = queriedElements.lastOrNull()?.cursor,
        hasPreviousPage = if (ascending) elementsBeforeCursor > 0 else elementsBeforeCursor > limit,
        hasNextPage = if (ascending) elementsAfterCursor > limit else elementsAfterCursor > 0,
      )

    return PaginationResult(queriedElements, pageInfo)
  }

  private fun initialQuery(
    totalElementCount: Long,
    limit: Int,
    baseWhereSpec: PredicateSpec,
    ascending: Boolean,
    selection: DataSelection,
  ): PaginationResult<V, C> {
    val queriedElements =
      repository
        .select(selection) {
          this.apply(dsl)
          this.where(baseWhereSpec)
          if (ascending) this.orderBy(orderBy.asc()) else this.orderBy(orderBy.desc())
          this.limit(limit)
        }
        .map { entity -> Edge(entity, this.cursor.encode(entity)) }

    val pageInfo =
      PageInfo(
        startCursor = queriedElements.firstOrNull()?.cursor,
        endCursor = queriedElements.lastOrNull()?.cursor,
        hasPreviousPage = if (ascending) false else totalElementCount > limit,
        hasNextPage = if (ascending) totalElementCount > limit else false,
      )

    return PaginationResult(queriedElements, pageInfo)
  }
}

class Cursor<V, CV, C>(private val decode: (C) -> CV, private val encode: (V) -> C) where
CV : Comparable<CV> {
  fun decode(cursor: C): CV {
    return try {
      decode.invoke(cursor)
    } catch (ex: Exception) {
      throw PaginationException("Decoding cursor $cursor failed", ex)
    }
  }

  fun encode(value: V): C {
    return try {
      encode.invoke(value)
    } catch (ex: Exception) {
      throw PaginationException("Encoding value $value failed", ex)
    }
  }
}

data class Edge<V, C>(val node: V, val cursor: C)

data class PageInfo<C>(
  val hasPreviousPage: Boolean,
  val hasNextPage: Boolean,
  val startCursor: C?,
  val endCursor: C?,
)

data class PaginationResult<V, C>(
  val edges: List<Edge<V, C>> = emptyList(),
  val pageInfo: PageInfo<C>? = null,
)

class PaginationException(message: String, source: Throwable? = null) :
  RuntimeException(message, source)
