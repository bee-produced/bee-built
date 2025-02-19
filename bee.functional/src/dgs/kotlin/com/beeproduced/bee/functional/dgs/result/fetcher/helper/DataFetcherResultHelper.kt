@file:Suppress("FunctionName")

package com.beeproduced.bee.functional.dgs.result.fetcher.helper

import com.netflix.graphql.types.errors.TypedGraphQLError
import graphql.execution.DataFetcherResult

/**
 * @author Kacper Urbaniec
 * @version 2022-10-11
 */
class DataFetcherResultHelper {
  companion object {
    fun <V> Ok(value: V): DataFetcherResult<V> {
      return DataFetcherResult.newResult<V>().data(value).build()
    }

    fun <V> InternalErr(
      message: String,
      debugInfo: Map<String, Any?>? = null,
      context: Any? = null,
      vararg formatArgs: Any,
    ): DataFetcherResult<V> {
      return DataFetcherResult.newResult<V>()
        .error(
          TypedGraphQLError.newInternalErrorBuilder()
            .message(message, formatArgs)
            .let { if (debugInfo == null) it else it.debugInfo(debugInfo) }
            .build()
        )
        .let { if (context == null) it else it.localContext(context) }
        .build()
    }

    fun <V> NotFoundErr(message: String, vararg formatArgs: Any): DataFetcherResult<V> {
      return DataFetcherResult.newResult<V>()
        .error(TypedGraphQLError.newNotFoundBuilder().message(message, formatArgs).build())
        .build()
    }

    fun <V> PermissionDeniedErr(message: String, vararg formatArgs: Any): DataFetcherResult<V> {
      return DataFetcherResult.newResult<V>()
        .error(TypedGraphQLError.newPermissionDeniedBuilder().message(message, formatArgs).build())
        .build()
    }

    fun <V> BadRequestErr(
      message: String,
      debugInfo: Map<String, Any?>? = null,
      context: Any? = null,
      vararg formatArgs: Any,
    ): DataFetcherResult<V> {
      return DataFetcherResult.newResult<V>()
        .error(
          TypedGraphQLError.newBadRequestBuilder()
            .message(message, formatArgs)
            .let { if (debugInfo == null) it else it.debugInfo(debugInfo) }
            .build()
        )
        .let { if (context == null) it else it.localContext(context) }
        .build()
    }

    fun <V> ConflictErr(message: String, vararg formatArgs: Any): DataFetcherResult<V> {
      return DataFetcherResult.newResult<V>()
        .error(TypedGraphQLError.newConflictBuilder().message(message, formatArgs).build())
        .build()
    }
  }
}
