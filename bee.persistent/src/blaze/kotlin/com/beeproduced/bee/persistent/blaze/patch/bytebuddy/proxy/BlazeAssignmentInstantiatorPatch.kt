package com.beeproduced.bee.persistent.blaze.patch.bytebuddy.proxy

import com.beeproduced.bee.persistent.blaze.meta.proxy.BlazeInstantiators
import com.blazebit.persistence.view.impl.proxy.AssignmentConstructorReflectionInstantiator
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
open class BlazeAssignmentInstantiatorPatch {

    companion object {
        fun patchAssignmentConstructorReflectionInstantiator(byteBuddy: ByteBuddy) {
            byteBuddy
                .redefine(AssignmentConstructorReflectionInstantiator::class.java)
                .method(ElementMatchers.named("newInstance"))
                .intercept(MethodDelegation.to(BlazeAssignmentInstantiatorPatch::class.java))
                .make()
                .load(
                    AssignmentConstructorReflectionInstantiator::class.java.getClassLoader(),
                    ClassReloadingStrategy.fromInstalledAgent()
                )
        }


        @JvmStatic
        @Suppress("unused")
        fun newInstance(
            @Argument(0) tuple: Array<Any?>,
            @FieldValue("constructor") constructor: Constructor<*>,
            @FieldValue("defaultObject") defaultObject: Array<Any?>,
            @This self: AssignmentConstructorReflectionInstantiator<Any>,
        ): Any {
            val clazz = constructor.declaringClass
            val viewName = clazz.simpleName.substringBefore("_$$")
            AbstractReflectionInstantiatorUtils
                .prepareTupleInvoker.prepareTuple(self, tuple)
            // val array: Array<Any?> = defaultObject.copyOf(defaultObject.size)
            // array[3] = tuple
            val sort = defaultObject[2] as IntArray
            val instantiator = BlazeInstantiators
                .assignmentInstantiators.getValue(viewName)
            return instantiator.create(tuple, sort)
        }
    }


}