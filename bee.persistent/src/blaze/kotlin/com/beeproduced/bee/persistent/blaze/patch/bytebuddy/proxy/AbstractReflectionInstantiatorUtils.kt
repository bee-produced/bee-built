package com.beeproduced.bee.persistent.blaze.patch.bytebuddy.proxy

import com.blazebit.persistence.view.impl.proxy.AbstractReflectionInstantiator
import java.lang.invoke.LambdaMetafactory
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-02
 */


object AbstractReflectionInstantiatorUtils {

    fun interface PrepareTupleInvoker {
        fun prepareTuple(self: AbstractReflectionInstantiator<Any>, tuple: Array<Any?>)
    }

    val prepareTupleInvoker: PrepareTupleInvoker = buildPrepareTuple()

    private fun buildPrepareTuple(): PrepareTupleInvoker {
        val lookup = MethodHandles.privateLookupIn(AbstractReflectionInstantiator::class.java, MethodHandles.lookup())
        val clazz = AbstractReflectionInstantiator::class.java
        val method = clazz.getDeclaredMethod("prepareTuple", Array<Any?>::class.java)
        val methodHandle = lookup.unreflect(method)
        val site = LambdaMetafactory.metafactory(
            lookup,
            "prepareTuple",
            MethodType.methodType(PrepareTupleInvoker::class.java),
            MethodType.methodType(Void.TYPE, AbstractReflectionInstantiator::class.java, Array::class.java),
            methodHandle,
            MethodType.methodType(Void.TYPE, AbstractReflectionInstantiator::class.java, Array::class.java)
        )
        return site.target.invokeExact() as PrepareTupleInvoker
    }

}