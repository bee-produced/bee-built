package com.beeproduced.bee.persistent.blaze

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

    // TODO: Change return type to T later on...
    fun select(selection: BeeSelection): List<T>
}