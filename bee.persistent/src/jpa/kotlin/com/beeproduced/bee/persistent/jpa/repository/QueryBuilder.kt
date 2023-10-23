package com.beeproduced.bee.persistent.jpa.repository

import com.beeproduced.bee.persistent.jpa.meta.EntityInfo
import com.beeproduced.bee.persistent.jpa.meta.Fields
import com.beeproduced.bee.persistent.jpa.meta.Ids
import com.linecorp.kotlinjdsl.query.spec.expression.ColumnSpec
import com.linecorp.kotlinjdsl.query.spec.expression.EntitySpec
import com.linecorp.kotlinjdsl.query.spec.expression.ExpressionSpec
import com.linecorp.kotlinjdsl.query.spec.predicate.*
import com.linecorp.kotlinjdsl.querydsl.CriteriaQueryDsl

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-02-02
 */
object QueryBuilder {

    // Query builder helper
    // ====================

    /**
     * Builds a `WHERE` clause for the table of `entityType` with the values from the given `entity`.
     */
    fun whereClauseFromEntity(
        entity: Any,
        entityType: Class<*>,
        ids: Ids
    ): PredicateSpec? {
        var whereClause: PredicateSpec? = null
        // Equal to following criteria DSL that can be used inside a where clause:
        // column(EntityType::id1).equal(entity.id1).and(column(EntityType::id2).equal(entity.id2)..
        for (id in ids.values) {
            val member = id.field
            val entitySpec = EntitySpec(entityType)
            val columnSpec = ColumnSpec<Any?>(entitySpec, member.name)
            val idValue = member.get(entity)
            val equalSpec = EqualValueSpec(columnSpec, idValue)

            whereClause = if (whereClause == null) {
                equalSpec
            } else {
                AndSpec(listOf(whereClause, equalSpec))
            }
        }
        return whereClause
    }

    /**
     * Builds a `WHERE` clause for the table of `entityType` with the values from the given `id`.
     * The `id` can be primitive e.g., `Long` or a class that is used as an [javax.persistence.IdClass].
     */
    fun whereClauseFromId(
        entityType: Class<*>,
        compositeKeyMapping: Set<EntityInfo.EntityToIdField>?,
        id: Any,
        idInfo: Ids
    ): PredicateSpec? {
        return if (compositeKeyMapping == null) whereClauseFromPrimitiveId(entityType, id, idInfo)
        else whereClauseFromCompositeKey(entityType, compositeKeyMapping, id)
    }

    /**
     * Builds a `WHERE` clause for the table of `entityType` with the values from the given `id`.
     * The `id` must be a primitive e.g., `Long`
     */
    private fun whereClauseFromPrimitiveId(
        entityType: Class<*>,
        id: Any,
        idInfo: Ids
    ): PredicateSpec {
        val member = idInfo.values.first().field
        val entitySpec = EntitySpec(entityType)
        val columnSpec = ColumnSpec<Any?>(entitySpec, member.name)
        return EqualValueSpec(columnSpec, id)
    }

    /**
     * Builds a `WHERE` clause for the table of `entityType` with the values from the given composite `id`.
     * The `id` must be a class that is used as an [javax.persistence.IdClass].
     */
    private fun whereClauseFromCompositeKey(
        entityType: Class<*>,
        compositeKeyMapping: Set<EntityInfo.EntityToIdField>,
        idInfo: Any,
    ): PredicateSpec? {
        var whereClause: PredicateSpec? = null
        for ((entityField, idField) in compositeKeyMapping) {
            val entitySpec = EntitySpec(entityType)
            val columnSpec = ColumnSpec<Any?>(entitySpec, entityField.name)
            val idValue = idField.get(idInfo)
            val equalSpec = EqualValueSpec(columnSpec, idValue)

            whereClause = if (whereClause == null) {
                equalSpec
            } else {
                AndSpec(listOf(whereClause, equalSpec))
            }
        }
        return whereClause
    }


    fun whereClauseFromIds(
        entityType: Class<*>,
        compositeKeyMapping: Set<EntityInfo.EntityToIdField>?,
        ids: Collection<Any>,
        idInfo: Ids
    ): PredicateSpec? {
        return if (compositeKeyMapping == null) whereClauseFromPrimitiveIds(entityType, ids, idInfo)
        else whereClauseFromCompositeKeys(entityType, compositeKeyMapping, ids)
    }

    private fun whereClauseFromPrimitiveIds(
        entityType: Class<*>,
        ids: Collection<Any>,
        idInfo: Ids
    ): PredicateSpec {
        val member = idInfo.values.first().field
        val entitySpec = EntitySpec(entityType)
        val columnSpec = ColumnSpec<Any?>(entitySpec, member.name)
        return InValueSpec(columnSpec, ids)
    }

    private fun whereClauseFromCompositeKeys(
        entityType: Class<*>,
        compositeKeyMapping: Set<EntityInfo.EntityToIdField>,
        ids: Collection<Any>,
    ): PredicateSpec {
        // TODO: Optimize? Is there are way to use `IN` instead of joining `AND` statements with `OR`
        val whereClauses = mutableListOf<PredicateSpec>()
        for (id in ids) {
            val clause =
                whereClauseFromCompositeKey(entityType, compositeKeyMapping, id)
                    ?: continue
            whereClauses.add(clause)
        }
        return OrSpec(whereClauses)
    }

    fun updateSetParams(
        entity: Any,
        entityType: Class<*>,
        fields: Fields
    ): Map<ColumnSpec<*>, Any?> {
        // Equal to following criteria DSL that can be used in update query:
        // mapOf(column(EntityType::field1) to entity.field1, column(EntityType::field2) to entity.field2..
        return fields.values.associateBy(
            { fieldInfo ->
                val entitySpec = EntitySpec(entityType)
                ColumnSpec<Any?>(entitySpec, fieldInfo.member.name)
            },
            { fieldInfo -> fieldInfo.get(entity) }
        )
    }

    // fun selectIdsFromEntity(entityType: Class<*>, ids: Ids): List<ColumnSpec<*>> {
    //     return ids.values.map { id ->
    //         val member = id.field
    //         val entitySpec = EntitySpec(entityType)
    //         ColumnSpec<Any?>(entitySpec, member.name)
    //     }
    //
    // }

    fun selectIdsFromEntity(
        entityType: Class<*>,
        ids: Ids
    ): CriteriaQueryDsl<Any>.() -> Unit {
        // Return correct corresponding select statement
        // If entity does not have a composite key
        // use CriteriaQueryDsl's SingleSelectClause
        // If entity has a composite key
        // use CriteriaQueryDsl's MultiSelectClause
        // Why? Using only MultiSelectClause can result in not found constructors
        // E.g., the UUID constructor cannot be invoked with a list
        return if (ids.values.count() == 1) {
            val id = ids.values.first()
            val member = id.field
            val entitySpec = EntitySpec(entityType)
            // SingleSelectClause(idType, false, ColumnSpec(entitySpec, member.name))
            val column: ExpressionSpec<Any> = ColumnSpec(entitySpec, member.name)
            val singleSelectQuery: CriteriaQueryDsl<Any>.() -> Unit = {
                // dsl.select(ColumnSpec<Any?>(entitySpec, member.name))
                this.select(column)
            }
            singleSelectQuery
        } else {
            val columns = ids.values.map { id ->
                val member = id.field
                val entitySpec = EntitySpec(entityType)
                ColumnSpec<Any>(entitySpec, member.name)
            }
            val multiSelectQuery: CriteriaQueryDsl<*>.() -> Unit = {
                this.select(columns)
            }
            multiSelectQuery
        }
    }

    fun selectCount(
        entityType: Class<*>,
        ids: Ids
    ): CriteriaQueryDsl<Long>.() -> Unit {
        val id = ids.values.first()
        val member = id.field
        val entitySpec = EntitySpec(entityType)
        val column: ExpressionSpec<Any> = ColumnSpec(entitySpec, member.name)
        val singleSelectQuery: CriteriaQueryDsl<Long>.() -> Unit = {
            // dsl.select(ColumnSpec<Any?>(entitySpec, member.name))
            this.select(listOf(count(column)))
        }
        return singleSelectQuery
    }
}