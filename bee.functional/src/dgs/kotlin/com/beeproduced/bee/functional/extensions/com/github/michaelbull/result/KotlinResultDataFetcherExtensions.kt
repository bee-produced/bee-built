package com.beeproduced.bee.functional.extensions.com.github.michaelbull.result

import com.beeproduced.bee.functional.dgs.fetcher.DataFetcher
import com.beeproduced.bee.functional.result.errors.AppError
import com.beeproduced.bee.functional.result.errors.BadRequestError
import com.beeproduced.bee.functional.result.errors.InternalAppError
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import graphql.execution.DataFetcherResult

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2022-10-11
 */

fun <V> Result<V, AppError>.getDataFetcher(): DataFetcherResult<V> {
    return when (this) {
        is Ok -> DataFetcher.Ok(value)
        is Err -> error.let { e ->
            when (e) {
                is InternalAppError -> DataFetcher.InternalErr(error.description())
                is BadRequestError -> DataFetcher.BadRequestErr(error.description())
            }
        }
    }
}


inline fun <V> Result<V, AppError>.getDataFetcher(
    transform: (AppError) -> DataFetcherResult<V>
): DataFetcherResult<V> {
    return when (this) {
        is Ok -> DataFetcher.Ok(value)
        is Err -> transform(error)
    }
}

inline fun <V, D> Result<V, AppError>.getDataFetcher(
    success: (V) -> DataFetcherResult<D>,
    failure: (AppError) -> DataFetcherResult<D>
): DataFetcherResult<D> {
    return when (this) {
        is Ok -> success(value)
        is Err -> failure(error)
    }
}