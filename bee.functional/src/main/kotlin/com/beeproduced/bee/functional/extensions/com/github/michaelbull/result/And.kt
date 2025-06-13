package com.beeproduced.bee.functional.extensions.com.github.michaelbull.result

import com.beeproduced.bee.functional.result.errors.AppError
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.asErr
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * @author Kacper Urbaniec
 * @version 2023-06-13
 */
@OptIn(ExperimentalContracts::class)
inline infix fun <V> Result<V, AppError>.andThenOnSuccess(
  action: (V) -> Result<*, AppError>
): Result<V, AppError> {
  contract { callsInPlace(action, InvocationKind.AT_MOST_ONCE) }

  if (isOk) {
    val result = action(value)
    if (result.isErr) return result.asErr()
  }

  return this
}
