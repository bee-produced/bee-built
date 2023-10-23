package com.beeproduced.bee.functional.dgs.config

import com.beeproduced.bee.functional.dgs.aspect.AroundDgsDataAspect
import com.beeproduced.bee.functional.dgs.aspect.DefaultAroundDgsDataAspect
import com.beeproduced.bee.functional.dgs.handler.AspectExceptionHandler
import graphql.execution.DataFetcherExceptionHandler
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
abstract class DgsErrorHandlingConfiguration {

    init {
        // See: https://stackoverflow.com/a/9005796/12347616
        val aspectAnnotation = AnnotationUtils.findAnnotation(this::class.java, EnableAspectJAutoProxy::class.java)
        if (aspectAnnotation == null) {
            throw IllegalStateException("DGS-Error-Handling: [${this::class.simpleName}] must be annotated with [org.springframework.context.annotation.EnableAspectJAutoProxy]")
        }
    }

    @Bean
    open fun beeFunctionalDgsDataFetcherExceptionHandler(): DataFetcherExceptionHandler {
        return AspectExceptionHandler()
    }

    @Bean
    open fun beeFunctionalDgsAspect(): AroundDgsDataAspect {
        return DefaultAroundDgsDataAspect()
    }
}
