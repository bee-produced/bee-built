package com.beeproduced.bee.persistent.blaze.meta.proxy

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-01
 */

typealias ViewName = String

object BlazeInstantiators {
    val tupleInstatiators: MutableMap<ViewName, TupleConstructorInstantiator> = mutableMapOf()
}