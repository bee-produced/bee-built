package com.beeproduced.result.extensions.errors

import com.beeproduced.result.errors.InternalAppError
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
 * @version 2023-03-10
 */

@OptIn(ExperimentalContracts::class)
inline fun <V> unsafe(block: () -> V): Result<V, InternalAppError> {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    return try {
        Ok(block())
    } catch (e: Throwable) {
        Err(InternalAppError(e.message ?: e.javaClass.name))
    }
}