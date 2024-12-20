package com.beeproduced.bee.persistent.blaze.meta.proxy

typealias TupleCreator = (tuple: Array<Any?>) -> Any?

data class TupleConstructorInstantiator(val viewProperties: List<String>, val create: TupleCreator)
