package com.beeproduced.bee.persistent.blaze

import com.beeproduced.bee.persistent.blaze.dsl.select.SelectQuery
import com.beeproduced.bee.persistent.blaze.dsl.select.Selection
import com.beeproduced.bee.persistent.blaze.selection.BeeSelection
import com.blazebit.persistence.CriteriaBuilderFactory
import com.blazebit.persistence.view.EntityViewManager
import jakarta.persistence.EntityManager

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-12-28
 */
interface BeeBlazeRepository<T : Any, ID: Any> {
    val em: EntityManager
    val cbf: CriteriaBuilderFactory
    val evm: EntityViewManager

    /**
     * Inserts new entity into db.
     * Use [update] to update an already persisted entity, not [persist].
     */
    fun <E: T> persist(entity: E): E

    /**
     * Updates all non-relation dependent fields of an entity in db.
     * Fields marked with [jakarta.persistence.OneToOne] and so on will NOT be updated.
     */
    fun <E: T> update(entity: E): E

    /**
     * Queries entities with [dsl] and specified relations from db.
     */
    fun select(
        selection: BeeSelection = BeeSelection.empty(),
        dsl: SelectQuery<T>.() -> Selection<T> = { this }
    ): List<T>

    /**
     * Queries entity with specified relations from db.
     * If [id] does not exist, null will be returned.
     */
    fun selectById(id: ID, selection: BeeSelection = BeeSelection.empty()): T?
}