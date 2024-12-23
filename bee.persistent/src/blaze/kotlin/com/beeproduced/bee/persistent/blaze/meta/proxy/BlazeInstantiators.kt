package com.beeproduced.bee.persistent.blaze.meta.proxy

/**
 * @author Kacper Urbaniec
 * @version 2024-01-01
 */
typealias ViewName = String

object BlazeInstantiators {
  val tupleInstantiators: MutableMap<ViewName, TupleConstructorInstantiator> = mutableMapOf()
  val assignmentInstantiators: MutableMap<ViewName, AssignmentConstructorInstantiator> =
    mutableMapOf()
}
