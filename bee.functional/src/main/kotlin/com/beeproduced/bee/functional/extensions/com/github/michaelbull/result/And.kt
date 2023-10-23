package com.beeproduced.bee.functional.extensions.com.github.michaelbull.result

import com.beeproduced.bee.functional.result.errors.AppError
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-06-13
 */

@OptIn(ExperimentalContracts::class)
inline infix fun <V> Result<V, AppError>.andThenOnSuccess(action: (V) -> Result<*, AppError>): Result<V, AppError> {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }

    if (this is Ok) {
        val result = action(value)
        if (result is Err) return result
    }

    return this
}