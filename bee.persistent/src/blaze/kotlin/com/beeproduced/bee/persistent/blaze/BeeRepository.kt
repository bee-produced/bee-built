package com.beeproduced.bee.persistent.blaze

import com.beeproduced.bee.persistent.blaze.selection.BeeSelection

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-12-28
 */
interface BeeBlazeRepository<T : Any, ID: Any> {
    // TODO: Change return type to T later on...
    fun select(selection: BeeSelection): List<Any>
}