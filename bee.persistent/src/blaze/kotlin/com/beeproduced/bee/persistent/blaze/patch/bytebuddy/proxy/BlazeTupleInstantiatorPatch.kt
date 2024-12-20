package com.beeproduced.bee.persistent.blaze.patch.bytebuddy.proxy

import com.beeproduced.bee.persistent.blaze.meta.proxy.BlazeInstantiators
import com.blazebit.persistence.view.impl.proxy.TupleConstructorReflectionInstantiator
import java.lang.reflect.Constructor
import net.bytebuddy.ByteBuddy
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy
import net.bytebuddy.implementation.MethodDelegation
import net.bytebuddy.implementation.bind.annotation.Argument
import net.bytebuddy.implementation.bind.annotation.FieldValue
import net.bytebuddy.implementation.bind.annotation.This
import net.bytebuddy.matcher.ElementMatchers

/**
 * @author Kacper Urbaniec
 * @version 2024-01-02
 */
open class BlazeTupleInstantiatorPatch {

  companion object {
    fun patchTupleConstructorReflectionInstantiator(byteBuddy: ByteBuddy) {
      byteBuddy
        .redefine(TupleConstructorReflectionInstantiator::class.java)
        .method(ElementMatchers.named("newInstance"))
        .intercept(MethodDelegation.to(BlazeTupleInstantiatorPatch::class.java))
        .make()
        .load(
          TupleConstructorReflectionInstantiator::class.java.getClassLoader(),
          ClassReloadingStrategy.fromInstalledAgent(),
        )
    }

    @JvmStatic
    @Suppress("unused")
    fun newInstance(
      @Argument(0) tuple: Array<Any?>,
      @FieldValue("constructor") constructor: Constructor<*>,
      @FieldValue("defaultObject") defaultObject: Array<Any?>,
      @This self: TupleConstructorReflectionInstantiator<Any>,
    ): Any? {
      val clazz = constructor.declaringClass
      val viewName = clazz.simpleName.substringBefore("_$$")
      AbstractReflectionInstantiatorUtils.prepareTupleInvoker.prepareTuple(self, tuple)
      // val array: Array<Any?> = defaultObject.copyOf(defaultObject.size)
      // array[2] = tuple
      val instantiator = BlazeInstantiators.tupleInstantiators.getValue(viewName)
      // Only returns null on embedded properties that are only present on
      // one of the subclasses in case of inheritance
      return instantiator.create(tuple)
    }
  }
}
