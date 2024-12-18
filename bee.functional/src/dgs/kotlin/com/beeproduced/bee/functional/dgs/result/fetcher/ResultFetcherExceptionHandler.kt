package com.beeproduced.bee.functional.dgs.result.fetcher

import com.beeproduced.bee.functional.dgs.result.fetcher.helper.DataFetcherErrThrower
import com.beeproduced.bee.functional.dgs.result.fetcher.helper.extendWithHandlerParameters
import com.netflix.graphql.dgs.exceptions.DefaultDataFetcherExceptionHandler
import com.netflix.graphql.types.errors.TypedGraphQLError
import graphql.execution.DataFetcherExceptionHandler
import graphql.execution.DataFetcherExceptionHandlerParameters
import graphql.execution.DataFetcherExceptionHandlerResult
import java.util.concurrent.CompletableFuture
import org.slf4j.LoggerFactory

/**
 * @author Kacper Urbaniec
 * @version 2022-10-10
 */
class ResultFetcherExceptionHandler : DataFetcherExceptionHandler {
  private val logger = LoggerFactory.getLogger(ResultFetcherExceptionHandler::class.java)
  private val defaultHandler = DefaultDataFetcherExceptionHandler()

  override fun handleException(
    handlerParameters: DataFetcherExceptionHandlerParameters
  ): CompletableFuture<DataFetcherExceptionHandlerResult> {
    return CompletableFuture.supplyAsync { onExceptionHandler(handlerParameters) }
  }

  private fun onExceptionHandler(
    handlerParameters: DataFetcherExceptionHandlerParameters
  ): DataFetcherExceptionHandlerResult {
    return when (val ex = handlerParameters.exception) {
      is DataFetcherErrThrower -> onDataFetcherErrThrowerException(ex, handlerParameters)
      else -> onUnexpectedException(ex, handlerParameters)
    }
  }

  private fun onDataFetcherErrThrowerException(
    ex: DataFetcherErrThrower,
    handlerParameters: DataFetcherExceptionHandlerParameters,
  ): DataFetcherExceptionHandlerResult {
    val error = ex.error
    val graphQLErrors =
      error.errors.map {
        if (it is TypedGraphQLError) {
          it.extendWithHandlerParameters(handlerParameters)
        } else it
      }
    return DataFetcherExceptionHandlerResult.newResult().errors(graphQLErrors).build()
  }

  private fun onUnexpectedException(
    ex: Throwable,
    handlerParameters: DataFetcherExceptionHandlerParameters,
  ): DataFetcherExceptionHandlerResult {
    logger.error("Encountered unexpected error")
    logger.error(ex.stackTraceToString())
    return defaultHandler.onException(handlerParameters)
  }
}
