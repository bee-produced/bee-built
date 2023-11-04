package com.beeproduced.bee.functional.dgs.result.fetcher.implementation.aspect

import com.beeproduced.bee.functional.dgs.result.fetcher.helper.DataFetcherErrThrower
import graphql.execution.DataFetcherResult
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import kotlin.reflect.full.findAnnotation

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2022-10-10
 */

@Aspect
abstract class AroundDgsDataAspect {
    init {
        val aspectAnnotation = this::class.findAnnotation<Aspect>()
        if (aspectAnnotation == null) {
            throw IllegalStateException("DGS-Error-Handling: [${this::class.simpleName}] must be annotated with [org.aspectj.lang.annotation.Aspect]")
        }
        val dgsDataAnnotation = ::aroundDgsData.findAnnotation<Around>()
        if (dgsDataAnnotation == null) {
            throw IllegalStateException("DGS-Error-Handling: Method [aroundDgsData] of [${this::class.simpleName}] must be annotated with [org.aspectj.lang.annotation.Around]")
        }
        if (!dgsDataAnnotation.value.contains(aroundAnnotationExpression)) {
            throw IllegalStateException("DGS-Error-Handling: Method [aroundDgsData] of [${this::class.simpleName}] must contain [$aroundAnnotationExpression] in [Around] annotation")
        }
    }

    companion object {
        const val aroundAnnotationExpression =
            "@annotation(com.netflix.graphql.dgs.DgsData) || @annotation(com.netflix.graphql.dgs.DgsMutation) || @annotation(com.netflix.graphql.dgs.DgsQuery)"
    }

    @Around(aroundAnnotationExpression)
    open fun aroundDgsData(joinPoint: ProceedingJoinPoint): Any? {
        val obj = joinPoint.proceed()
        if (obj !is DataFetcherResult<*>) return obj
        if (obj.errors.isNotEmpty() && obj.data == null) throw DataFetcherErrThrower(obj)
        return obj
    }
}

@Aspect
class DefaultAroundDgsDataAspect : AroundDgsDataAspect()
