package com.beeproduced.bee.functional.dgs.handler

import com.beeproduced.bee.functional.dgs.fetcher.extendWithHandlerParameters
import com.beeproduced.bee.functional.dgs.fetcher.DataFetcherErrThrower
import com.netflix.graphql.dgs.exceptions.DefaultDataFetcherExceptionHandler
import com.netflix.graphql.types.errors.TypedGraphQLError
import graphql.execution.DataFetcherExceptionHandler
import graphql.execution.DataFetcherExceptionHandlerParameters
import graphql.execution.DataFetcherExceptionHandlerResult
import org.slf4j.LoggerFactory

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2022-10-10
 */
class AspectExceptionHandler : DataFetcherExceptionHandler {
    private val logger = LoggerFactory.getLogger(AspectExceptionHandler::class.java)
    private val defaultHandler = DefaultDataFetcherExceptionHandler()

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onException(
        handlerParameters: DataFetcherExceptionHandlerParameters
    ): DataFetcherExceptionHandlerResult {
        return when (val ex = handlerParameters.exception) {
            is DataFetcherErrThrower -> onDataFetcherErrThrowerException(ex, handlerParameters)
            else -> onUnexpectedException(ex, handlerParameters)
        }
    }

    private fun onDataFetcherErrThrowerException(
        ex: DataFetcherErrThrower,
        handlerParameters: DataFetcherExceptionHandlerParameters
    ): DataFetcherExceptionHandlerResult {
        val error = ex.error
        val graphQLErrors = error.errors.map {
            if (it is TypedGraphQLError) {
                it.extendWithHandlerParameters(handlerParameters)
            } else it
        }
        return DataFetcherExceptionHandlerResult
            .newResult()
            .errors(graphQLErrors)
            .build()
    }

    private fun onUnexpectedException(
        ex: Throwable,
        handlerParameters: DataFetcherExceptionHandlerParameters
    ): DataFetcherExceptionHandlerResult {
        logger.error("Encountered unexpected error")
        logger.error(ex.stackTraceToString())
        return defaultHandler.onException(handlerParameters)
    }
}
