package com.beeproduced.utils

import java.util.*

/**
 * A type to be used as [Context] parameter to track cycles in graphs.
 * https://github.com/mapstruct/mapstruct-examples/blob/main/mapstruct-mapping-with-cycles/src/main/java/org/mapstruct/example/mapper/CycleAvoidingMappingContext.java
 *
 * Depending on the actual use case, the two methods below could also be changed to only accept
 * certain argument types, e.g. base classes of graph nodes, avoiding the need to capture any other
 * objects that wouldn't necessarily result in cycles.
 *
 * @author Andreas Gudian
 */
class CycleAvoidingMappingContext {
  private val visited: MutableSet<Pair<Class<*>, Any>> = mutableSetOf()

  fun addVisited(clazz: Class<*>, identifier: Any) {
    visited.add(Pair(clazz, identifier))
  }

  fun hasVisited(clazz: Class<*>, identifier: Any): Boolean {
    return visited.contains(Pair(clazz, identifier))
  }

  // Does not work for this use case using immutable data structure

  // private val knownInstances: MutableMap<Any, Any> = IdentityHashMap()
  // @BeforeMapping
  // fun <T> getMappedInstance(source: Any, @TargetType targetType: Class<T>?): T? {
  //     return knownInstances[source] as T?
  // }
  //
  // @BeforeMapping
  // fun storeMappedInstance(source: Any, @MappingTarget target: Any) {
  //     knownInstances[source] = target
  // }
}
