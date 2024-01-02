package com.beeproduced.bee.persistent.blaze.patch.bytebuddy.proxy

import com.beeproduced.bee.persistent.blaze.meta.proxy.BlazeInstantiators
import com.blazebit.persistence.view.impl.proxy.AssignmentConstructorReflectionInstantiator
import com.blazebit.persistence.view.impl.proxy.TupleConstructorReflectionInstantiator
import net.bytebuddy.ByteBuddy
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy
import net.bytebuddy.implementation.MethodDelegation
import net.bytebuddy.implementation.bind.annotation.Argument
import net.bytebuddy.implementation.bind.annotation.FieldValue
import net.bytebuddy.implementation.bind.annotation.This
import net.bytebuddy.matcher.ElementMatchers
import java.lang.reflect.Constructor

/**
 *
 *
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
                    ClassReloadingStrategy.fromInstalledAgent()
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
            AbstractReflectionInstantiatorUtils
                .prepareTupleInvoker.prepareTuple(self, tuple)
            // val array: Array<Any?> = defaultObject.copyOf(defaultObject.size)
            // array[2] = tuple
            val instantiator = BlazeInstantiators
                .tupleInstantiators.getValue(viewName)
            // TODO: Even called on inherited entities that do not contain this field!
            // Hacky Workaround, ..., check in creator if param null in embedded field?
            // Add check for creator(?):
            //   if (field1 !is <field1> || field2 !is <field2> || ...) return null
            return try { instantiator.create(tuple) } catch (ex: Exception) { null }
        }
    }


}