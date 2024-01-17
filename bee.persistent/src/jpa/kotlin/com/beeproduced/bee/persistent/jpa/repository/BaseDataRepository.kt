package com.beeproduced.bee.persistent.jpa.repository

import com.beeproduced.bee.persistent.jpa.entity.DataEntity
import com.beeproduced.bee.persistent.jpa.meta.*
import com.beeproduced.bee.persistent.jpa.proxy.Unproxy
import com.beeproduced.bee.persistent.jpa.repository.dsl.DummyCriteriaDeleteQueryDsl
import com.beeproduced.bee.persistent.jpa.repository.dsl.DummyCriteriaQueryDsl
import com.beeproduced.bee.persistent.jpa.selection.JpaSelection
import com.beeproduced.bee.persistent.selection.DataSelection
import com.beeproduced.bee.persistent.selection.EmptySelection
import com.linecorp.kotlinjdsl.QueryFactoryImpl
import com.linecorp.kotlinjdsl.query.creator.CriteriaQueryCreatorImpl
import com.linecorp.kotlinjdsl.query.creator.SubqueryCreatorImpl
import com.linecorp.kotlinjdsl.query.spec.expression.ColumnSpec
import com.linecorp.kotlinjdsl.query.spec.expression.EntitySpec
import com.linecorp.kotlinjdsl.querydsl.CriteriaDeleteQueryDsl
import com.linecorp.kotlinjdsl.querydsl.CriteriaQueryDsl
import com.linecorp.kotlinjdsl.selectQuery
import jakarta.persistence.EntityManager
import org.hibernate.metamodel.model.domain.internal.MappingMetamodelImpl
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.reflect.ParameterizedType
import kotlin.reflect.KClass

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-01-30
 */
@Suppress("MemberVisibilityCanBePrivate")
abstract class BaseDataRepository<T : DataEntity<T>, ID : Any>(
    protected val entityManager: EntityManager
) {
    protected val kType: KClass<T>
    protected val type: Class<T>
    protected val kIdType: KClass<ID>
    protected val idType: Class<ID>
    protected val info: EntityInfo
    protected val ids: Ids
    protected val fields: Fields
    protected val relations: Relations
    protected val generated: Generated

    protected val selectDistinctIdColumns: CriteriaQueryDsl<*>.() -> Unit
    protected val selectDistinctIdAndOrderColumns: CriteriaQueryDsl<*>.(List<ColumnSpec<*>>) -> Unit
    protected val selectCount: CriteriaQueryDsl<Long>.(forceDistinct: Boolean) -> Unit
    protected val queryFactory = QueryFactoryImpl(
        criteriaQueryCreator = CriteriaQueryCreatorImpl(entityManager),
        subqueryCreator = SubqueryCreatorImpl()
    )

    companion object {
        val logger: Logger = LoggerFactory.getLogger(BaseDataRepository::class.java)
    }

    init {
        // Retrieve generic types
        // https://stackoverflow.com/a/52073780/12347616
        val genericTypes = (this.javaClass.genericSuperclass as ParameterizedType).actualTypeArguments
        @Suppress("UNCHECKED_CAST")
        this.type = genericTypes[0] as Class<T>
        @Suppress("UNCHECKED_CAST")
        this.idType = genericTypes[1] as Class<ID>
        kType = this.type.kotlin
        kIdType = this.idType.kotlin

        // Build metamodel
        val mappingModel = entityManager.entityManagerFactory.metamodel as MappingMetamodelImpl
        val entityPersister = mappingModel.getEntityDescriptor(this.type)
        info = MetaModel.addEntity(this.type, this.idType, entityPersister)
        // Assign often used properties from metamodel
        val membersInfo = info.members
        ids = membersInfo.ids
        fields = membersInfo.fields
        relations = membersInfo.relations
        generated = membersInfo.generated

        // Build function used to select ids
        // Is needed, as there is a difference between selecting entity with single primary key
        // and with composite key
        @Suppress("UNCHECKED_CAST")
        selectDistinctIdColumns = QueryBuilder.selectDistinctIdsFromEntity(this.type, ids)
            as CriteriaQueryDsl<*>.() -> Unit
        @Suppress("UNCHECKED_CAST")
        selectDistinctIdAndOrderColumns = QueryBuilder.selectDistinctIdAndOrderColumnsFromEntity(this.type, ids)
            as CriteriaQueryDsl<*>.(List<ColumnSpec<*>>) -> Unit
        selectCount = QueryBuilder.selectCount(this.type, ids)
    }

    /**
     * Inserts new entity into db.
     * Use [update] to update an already persisted entity, not [persist].
     */
    @Suppress("UNCHECKED_CAST", "NAME_SHADOWING")
    open fun persist(entity: T): T {
        // Copy entity as persisted entity has side effects
        val entity = entity.clone()
        // Set auto-generated values to NULL or primitive default value
        // -----------------------------------------------
        // For assigned identifiers, a merge will always require an SQL SELECT since Hibernate cannot know
        // if there is already a persisted entity having the same identifier. For other identifier generators,
        // Hibernate looks for a null identifier to figure out if the entity is in the transient state.
        // https://vladmihalcea.com/hibernate-and-uuid-identifiers/
        for (generatedInfo in generated.values) {
            val memberInfo = generatedInfo.memberInfo
            val defaultValue = generatedInfo.defaultValue
            memberInfo.field.set(entity, defaultValue)
        }
        entityManager.persist(entity) // Add to persistence context
        entityManager.flush() // Flush to db (useful to get autogenerated id)
        entityManager.clear() // Detach from persistence context
        Unproxy.unproxyEntity(entity, type) // Remove JPA lazy proxies
        return entity
    }

    /**
     * Inserts multiple new entities into db.
     * Use [update] to update an already persisted entity, not [persist].
     */
    open fun persistAll(entities: Collection<T>): List<T> {
        return entities.map { entity -> persist(entity) }
    }

    /**
     * Inserts new entity into db and queries it again with specified relations loaded.
     * Use [update] to update an already persisted entity, not [persist].
     */
    @Suppress("NAME_SHADOWING")
    open fun persistAndSelect(entity: T, selection: DataSelection = EmptySelection()): T {
        val entity = persist(entity)
        return select(selection) {
            where(QueryBuilder.whereClauseFromEntity(entity, type, ids))
            // limit(1)
        }.first()
    }

    /**
     * Inserts multiple new entities into db and queries them again with specified relations loaded.
     * Use [update] to update an already persisted entity, not [persist].
     */
    open fun persistAllAndSelect(entities: Collection<T>, selection: DataSelection = EmptySelection()): List<T> {
        val entityIds = entities
            .map { entity -> persist(entity) }
            .map { entity -> identifier(entity) }

        return selectByIds(entityIds, selection)
    }

    /**
     * Updates all non-relation dependent fields of an entity in db.
     * Fields marked with [javax.persistence.OneToOne] and so on will NOT be updated.
     */
    open fun update(entity: T): T {
        queryFactory.updateQuery(kType) {
            where(QueryBuilder.whereClauseFromEntity(entity, type, ids))
            setParams(QueryBuilder.updateSetParams(entity, type, fields))
        }.executeUpdate()
        return entity
    }

    /**
     * Updates all non-relation dependent fields of an entity in db and queries it again with specified relations loaded.
     * Fields marked with [javax.persistence.OneToOne] and so on will NOT be updated.
     */
    @Suppress("NAME_SHADOWING")
    open fun updateAndSelect(entity: T, selection: DataSelection = EmptySelection()): T {
        val entity = update(entity)
        return select(selection) {
            where(QueryBuilder.whereClauseFromEntity(entity, type, ids))
            // limit(1)
        }.first()
    }

    /**
     * Queries entity with specified relations from db.
     * If [id] does not exist, null will be returned.
     */
    open fun selectById(id: ID, selection: DataSelection = EmptySelection()): T? {
        return select(selection) {
            where(QueryBuilder.whereClauseFromId(type, info.compositeKeyMapping, id, ids))
            // limit(1)
        }.firstOrNull()
    }

    /**
     * Queries entities with specified relations from db.
     * If [ids] do not exist, empty list will be returned.
     * If only some [ids] are missing, a partial result will be returned.
     */
    open fun selectByIds(ids: Collection<ID>, selection: DataSelection = EmptySelection()): List<T> {
        if (ids.isEmpty()) return emptyList()
        return select(selection) {
            where(QueryBuilder.whereClauseFromIds(type, info.compositeKeyMapping, ids, this@BaseDataRepository.ids))
        }
    }

    /**
     * Queries entities with kotlin-jdsl [dsl] and specified relations from db.
     * The [dsl] should NOT contain a “SELECT” nor “FROM” clause.
     */
    open fun select(
        selection: DataSelection = EmptySelection(),
        dsl: CriteriaQueryDsl<*>.() -> Unit = {}
    ): List<T> {
        val testDsl = DummyCriteriaQueryDsl().apply(dsl)
        val query = if (testDsl.hasLimitClause && !testDsl.hasOrderByClause) {
            // Query ids first, then query with where!
            // Hibernate cannot use `LIMIT` when joining tables!
            // To be precise, it returns a “limited” result but queries EVERYTHING and limits in memory!
            // https://vladmihalcea.com/fix-hibernate-hhh000104-entity-fetch-pagination-warning-message/
            // https://hibernate.atlassian.net/browse/HHH-15964?focusedCommentId=110593
            val resultIds: List<Any> = queryFactory.selectQuery(info.nonPrimitiveIdType) {
                // select(selectIdColumns)
                selectDistinctIdColumns(this)
                from(EntitySpec(type))
                dsl(this)
            }.resultList

            if (resultIds.isEmpty()) return emptyList()

            queryFactory.selectQuery(type) {
                select(EntitySpec(type))
                from(EntitySpec(type))
                where(QueryBuilder.whereClauseFromIds(type, info.compositeKeyMapping, resultIds, ids))
            }
        } else if (testDsl.hasLimitClause && testDsl.hasOrderByClause) {
            val orderByColumns = testDsl.orderByColumns()
            val idsAndOrderBy: List<List<Any>> = queryFactory.selectQuery<List<Any>> {
                selectDistinctIdAndOrderColumns(this, orderByColumns)
                from(EntitySpec(type))
                dsl(this)
            }.resultList

            if (idsAndOrderBy.isEmpty()) return emptyList()
            val resultIds = idsAndOrderBy.map {
                it.dropLast(orderByColumns.count())
            }

            queryFactory.selectQuery(type) {
                select(EntitySpec(type))
                from(EntitySpec(type))
                where(QueryBuilder.whereClauseFromIds(type, info.compositeKeyMapping, resultIds, ids))
                orderBy(testDsl.orders)
            }
        } else {
            queryFactory.selectQuery(type) {
                select(EntitySpec(type))
                from(EntitySpec(type))
                dsl(this)
            }
        }

        // Add load graph, the *special* sauce, makes all lazy marked properties eager!
        // Will execute MULTIPLE sub select statements BUT in ONE SELECT
        if (selection !is EmptySelection) {
            val graph = JpaSelection.dataSelectionToEntityGraph(type, selection, entityManager)
            query.setHint("jakarta.persistence.loadgraph", graph)
        }
        val results = query.resultList

        entityManager.clear()
        Unproxy.unproxyEntities(results, type)
        return results
    }

    open fun count(dsl: CriteriaQueryDsl<*>.() -> Unit = {}): Long {
        return count(true, dsl)
    }

    open fun count(distinct: Boolean, dsl: CriteriaQueryDsl<*>.() -> Unit = {}): Long {
        val count = queryFactory.selectQuery(Long::class.javaObjectType) {
            selectCount(this, distinct)
            from(EntitySpec(type))
            dsl(this)
        }
        return count.singleResult
    }

    /**
     * Returns true if entity with given ID exists in db.
     */
    open fun exists(id: ID): Boolean {
        val resultIds: List<Any> = queryFactory.selectQuery(info.nonPrimitiveIdType) {
            selectDistinctIdColumns(this)
            from(EntitySpec(type))
            where(QueryBuilder.whereClauseFromId(type, info.compositeKeyMapping, id, ids))
        }.resultList

        return resultIds.isNotEmpty()
    }

    /**
     * Returns true if for EVERY given ID an entity exists in the db.
     * Just with one ID not found, “false” will be returned.
     */
    open fun existsAll(ids: Collection<ID>): Boolean {
        val uniqueIds = ids.toSet()
        val resultIds: List<Any> = queryFactory.selectQuery(info.nonPrimitiveIdType) {
            selectDistinctIdColumns(this)
            from(EntitySpec(type))
            where(
                QueryBuilder.whereClauseFromIds(
                    type,
                    info.compositeKeyMapping,
                    uniqueIds,
                    this@BaseDataRepository.ids
                )
            )
        }.resultList

        return resultIds.count() == uniqueIds.count()
    }

    /**
     * Deletes all entities that match the provided kotlin-jdsl [dsl] from db.
     */
    open fun delete(dsl: CriteriaDeleteQueryDsl.() -> Unit): DeletedRowsCount {
        val testDsl = DummyCriteriaDeleteQueryDsl().apply(dsl)
        return if (testDsl.hasWhereClause) {
            val query = queryFactory.deleteQuery(kType, dsl)
            query.executeUpdate()
        } else {
            logger.warn("delete has no where clause and will not be executed")
            0
        }
    }

    /**
     * Deletes all entities from db.
     */
    open fun deleteAll(): DeletedRowsCount {
        val query = queryFactory.deleteQuery(kType) {}
        return query.executeUpdate()
    }

    /**
     * Deletes entity with specified [id] from db.
     * If id does not exist, nothing will be deleted.
     */
    open fun deleteById(id: ID): DeletedRowsCount {
        val query = queryFactory.deleteQuery(kType) {
            where(QueryBuilder.whereClauseFromId(type, info.compositeKeyMapping, id, ids))
        }
        return query.executeUpdate()
    }

    /**
     * Deletes all entities with specified [ids] from db.
     * If [ids] do not exist, nothing will be deleted.
     * If only some [ids] are existent, then only them will be deleted.
     */
    open fun deleteByIds(ids: Collection<ID>): DeletedRowsCount {
        val query = queryFactory.deleteQuery(kType) {
            where(QueryBuilder.whereClauseFromIds(type, info.compositeKeyMapping, ids, this@BaseDataRepository.ids))
        }
        return query.executeUpdate()
    }

    /**
     * Returns the ID of an entity.
     * Useful for composite keys.
     */
    @Suppress("UNCHECKED_CAST")
    open fun identifier(entity: T): ID {
        return MetaModel.getEntityIdentifier(entity, type) as ID
    }
}

typealias DeletedRowsCount = Int
