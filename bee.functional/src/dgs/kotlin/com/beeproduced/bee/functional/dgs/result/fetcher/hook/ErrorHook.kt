package com.beeproduced.bee.functional.dgs.result.fetcher.hook

import com.beeproduced.bee.functional.dgs.result.fetcher.helper.DataFetcherErrThrower
import com.beeproduced.bee.functional.result.errors.AppError
import graphql.GraphQLError
import graphql.execution.DataFetcherExceptionHandlerParameters

interface ErrorHook {
  fun handleError(
    error: AppError,
    ex: DataFetcherErrThrower,
    handlerParameters: DataFetcherExceptionHandlerParameters,
    graphQLErrors: List<GraphQLError>,
  )

  fun handleException(
    exception: Throwable,
    handlerParameters: DataFetcherExceptionHandlerParameters,
  )
}
