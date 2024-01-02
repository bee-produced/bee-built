package com.beeproduced.bee.persistent.blaze.meta.proxy

typealias AssignmentCreator = (tuple: Array<Any?>, sort: IntArray)->Any
data class AssignmentConstructorInstantiator(
    val viewProperties: List<String>,
    val create: AssignmentCreator
)