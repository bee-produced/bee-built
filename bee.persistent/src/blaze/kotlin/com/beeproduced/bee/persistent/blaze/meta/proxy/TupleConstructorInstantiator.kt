package com.beeproduced.bee.persistent.blaze.meta.proxy

typealias TupleCreator = (Array<Any?>, IntArray)->Any
data class TupleConstructorInstantiator(
    val viewProperties: List<String>,
    val create: TupleCreator
)