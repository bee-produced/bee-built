package com.beeproduced.bee.functional.dgs.result.fetcher.implementation.aspect

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.core.annotation.AnnotationUtils

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2022-10-10
 */
@EnableAspectJAutoProxy
abstract class ResultFetcherAspectConfiguration {

    init {
        // See: https://stackoverflow.com/a/9005796/12347616
        val aspectAnnotation = AnnotationUtils.findAnnotation(this::class.java, EnableAspectJAutoProxy::class.java)
        if (aspectAnnotation == null) {
            throw IllegalStateException("DGS-Error-Handling: [${this::class.simpleName}] must be annotated with [org.springframework.context.annotation.EnableAspectJAutoProxy]")
        }
    }

    @Bean
    open fun beeFunctionalResultFetcherAspect(): AroundDgsDataAspect {
        return DefaultAroundDgsDataAspect()
    }
}
