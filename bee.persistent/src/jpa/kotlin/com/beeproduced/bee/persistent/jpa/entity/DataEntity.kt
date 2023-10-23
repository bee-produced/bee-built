package com.beeproduced.bee.persistent.jpa.entity

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-02-07
 */
interface DataEntity<T> {
    fun clone(): T
}