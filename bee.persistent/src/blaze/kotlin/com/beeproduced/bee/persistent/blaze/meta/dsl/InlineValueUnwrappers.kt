package com.beeproduced.bee.persistent.blaze.meta.dsl

import java.lang.invoke.LambdaMetafactory
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

/**
 * @author Kacper Urbaniec
 * @version 2024-01-11
 */
typealias FullyQualifiedName = String

object InlineValueUnwrappers {
  val unwrappers: MutableMap<FullyQualifiedName, InlineValueUnwrapper> = mutableMapOf()

  fun registerUnwrapper(inlineValueClass: Class<*>, innerClass: Class<*>) {
    // //
    // https://youtrack.jetbrains.com/issue/KT-50518/Boxing-Unboxing-methods-for-JvmInline-value-classes-should-be-public-accessible
    val method = inlineValueClass.getDeclaredMethod("unbox-impl")
    val lookup = MethodHandles.privateLookupIn(inlineValueClass, MethodHandles.lookup())
    val methodHandle = lookup.unreflect(method)
    val unwrap =
      LambdaMetafactory.metafactory(
          lookup,
          InlineValueUnwrapper::unwrap.name,
          MethodType.methodType(InlineValueUnwrapper::class.java),
          MethodType.methodType(Any::class.java, Any::class.java),
          methodHandle,
          MethodType.methodType(innerClass, inlineValueClass),
        )
        .target
        .invokeExact() as InlineValueUnwrapper

    unwrappers[inlineValueClass.canonicalName] = unwrap
  }

  @Suppress("UNCHECKED_CAST")
  fun <V : Any, T : Any> unwrap(value: V?, unwrapKey: String): T? {
    return if (value == null) null else unwrappers.getValue(unwrapKey).unwrap(value) as T
  }
}
