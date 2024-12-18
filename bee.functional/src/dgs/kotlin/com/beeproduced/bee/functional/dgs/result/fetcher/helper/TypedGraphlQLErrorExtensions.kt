package com.beeproduced.bee.functional.dgs.result.fetcher.helper

import com.netflix.graphql.types.errors.ErrorType
import com.netflix.graphql.types.errors.TypedGraphQLError
import graphql.execution.DataFetcherExceptionHandlerParameters

/**
 * @author Kacper Urbaniec
 * @version 2022-10-10
 */
fun TypedGraphQLError.errorType(): ErrorType {
  return javaClass.getDeclaredField("classification").let {
    it.isAccessible = true
    return@let it.get(this) as ErrorType
  }
}

fun TypedGraphQLError.extendWithHandlerParameters(
  handlerParameters: DataFetcherExceptionHandlerParameters
): TypedGraphQLError {
  return TypedGraphQLError.newBuilder()
    .message(this.message)
    .errorType(this.errorType())
    .path(handlerParameters.path)
    .build()
}
