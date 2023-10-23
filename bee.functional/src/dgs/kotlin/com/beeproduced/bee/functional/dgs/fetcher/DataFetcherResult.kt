@file:Suppress("FunctionName")

package com.beeproduced.bee.functional.dgs.fetcher

import com.netflix.graphql.types.errors.TypedGraphQLError
import graphql.execution.DataFetcherResult

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2022-10-11
 */

class DataFetcher {
    companion object {
        fun <V> Ok(value: V): DataFetcherResult<V> {
            return DataFetcherResult
                .newResult<V>()
                .data(value)
                .build()
        }

        fun <V> InternalErr(message: String, vararg formatArgs: Any): DataFetcherResult<V> {
            return DataFetcherResult
                .newResult<V>()
                .error(
                    TypedGraphQLError
                        .newInternalErrorBuilder()
                        .message(message, formatArgs)
                        .build()
                ).build()
        }

        fun <V> NotFoundErr(message: String, vararg formatArgs: Any): DataFetcherResult<V> {
            return DataFetcherResult
                .newResult<V>()
                .error(
                    TypedGraphQLError
                        .newNotFoundBuilder()
                        .message(message, formatArgs)
                        .build()
                ).build()
        }

        fun <V> PermissionDeniedErr(message: String, vararg formatArgs: Any): DataFetcherResult<V> {
            return DataFetcherResult
                .newResult<V>()
                .error(
                    TypedGraphQLError
                        .newPermissionDeniedBuilder()
                        .message(message, formatArgs)
                        .build()
                ).build()
        }

        fun <V> BadRequestErr(message: String, vararg formatArgs: Any): DataFetcherResult<V> {
            return DataFetcherResult
                .newResult<V>()
                .error(
                    TypedGraphQLError
                        .newBadRequestBuilder()
                        .message(message, formatArgs)
                        .build()
                ).build()
        }

        fun <V> ConflictErr(message: String, vararg formatArgs: Any): DataFetcherResult<V> {
            return DataFetcherResult
                .newResult<V>()
                .error(
                    TypedGraphQLError
                        .newConflictBuilder()
                        .message(message, formatArgs)
                        .build()
                ).build()
        }
    }
}
