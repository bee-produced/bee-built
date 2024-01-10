package com.beeproduced.bee.persistent.blaze.dsl.select

import com.beeproduced.bee.persistent.blaze.dsl.predicate.Predicate

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-10
 */
interface SelectWhere<T> : Selection<T> {

    fun where(predicate: Predicate): Selection<T>

}