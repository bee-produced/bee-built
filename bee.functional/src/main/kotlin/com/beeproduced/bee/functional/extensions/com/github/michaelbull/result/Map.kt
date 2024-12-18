package com.beeproduced.bee.functional.extensions.com.github.michaelbull.result

import com.beeproduced.bee.functional.result.errors.AppError
import com.beeproduced.bee.functional.result.errors.BadRequestError
import com.beeproduced.bee.functional.result.errors.InternalAppError
import com.beeproduced.bee.functional.result.errors.ResultError
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * @author Kacper Urbaniec
 * @version 2023-01-12
 */
@OptIn(ExperimentalContracts::class)
@PublishedApi
internal inline fun <V, reified E : AppError> Result<V, AppError>.mapInternalErrorGeneric(
  transform: (E) -> E
): Result<V, AppError> {
  contract { callsInPlace(transform, InvocationKind.AT_MOST_ONCE) }

  return when (this) {
    is Ok -> this
    is Err ->
      error.let { e ->
        when (e) {
          is E -> Err(transform(e))
          else -> Err(e)
        }
      }
  }
}

@PublishedApi
internal inline fun <V, reified E : AppError> Result<V, AppError>.mapInternalErrorGeneric(
  constructor: (String, ResultError, Long, Long) -> E,
  description: String,
): Result<V, AppError> {

  return when (this) {
    is Ok -> this
    is Err ->
      error.let { e ->
        when (e) {
          // Third parameter skip traces +1 to omit outer function call
          // Inline functions like this are NOT counted!
          is E -> Err(constructor(description, e, 1, 1))
          else -> Err(e)
        }
      }
  }
}

inline infix fun <V> Result<V, AppError>.mapInternalError(
  transform: (InternalAppError) -> InternalAppError
): Result<V, AppError> {
  return mapInternalErrorGeneric(transform)
}

infix fun <V> Result<V, AppError>.mapInternalError(description: String): Result<V, AppError> {
  // See: https://stackoverflow.com/a/50511988/12347616
  return mapInternalErrorGeneric(::InternalAppError, description)
}

inline infix fun <V> Result<V, AppError>.mapBadRequestError(
  transform: (BadRequestError) -> BadRequestError
): Result<V, AppError> {
  return mapInternalErrorGeneric(transform)
}

infix fun <V> Result<V, AppError>.mapBadRequestError(description: String): Result<V, AppError> {
  return mapInternalErrorGeneric(::BadRequestError, description)
}
