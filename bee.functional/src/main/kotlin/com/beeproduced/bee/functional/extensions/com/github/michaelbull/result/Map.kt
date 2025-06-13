package com.beeproduced.bee.functional.extensions.com.github.michaelbull.result

import com.beeproduced.bee.functional.result.errors.AppError
import com.beeproduced.bee.functional.result.errors.BadRequestError
import com.beeproduced.bee.functional.result.errors.InternalAppError
import com.beeproduced.bee.functional.result.errors.ResultError
import com.github.michaelbull.result.Err
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
internal inline fun <V, reified E : AppError> Result<V, AppError>.mapWhenErrorGeneric(
  transform: (E) -> E
): Result<V, AppError> {
  contract { callsInPlace(transform, InvocationKind.AT_MOST_ONCE) }

  return when {
    isOk -> this
    else ->
      error.let { e ->
        when (e) {
          is E -> Err(transform(e))
          else -> Err(e)
        }
      }
  }
}

@PublishedApi
internal inline fun <V, reified E : AppError> Result<V, AppError>.mapWhenErrorGeneric(
  constructor: (String, ResultError, Map<String, Any?>?, Long, Long) -> E,
  description: String,
  debugInfo: Map<String, Any?>?,
): Result<V, AppError> {

  return when {
    isOk -> this
    else ->
      error.let { e ->
        when (e) {
          // Third parameter skip traces +1 to omit outer function call
          // Inline functions like this are NOT counted!
          is E -> Err(constructor(description, e, debugInfo, 2, 1))
          else -> Err(e)
        }
      }
  }
}

inline infix fun <V> Result<V, AppError>.mapWhenInternalError(
  transform: (InternalAppError) -> InternalAppError
): Result<V, AppError> {
  return mapWhenErrorGeneric(transform)
}

fun <V> Result<V, AppError>.mapWhenInternalError(
  description: String,
  debugInfo: Map<String, Any?>? = null,
): Result<V, AppError> {
  // See: https://stackoverflow.com/a/50511988/12347616
  return mapWhenErrorGeneric(::InternalAppError, description, debugInfo)
}

inline infix fun <V> Result<V, AppError>.mapWhenBadRequestError(
  transform: (BadRequestError) -> BadRequestError
): Result<V, AppError> {
  return mapWhenErrorGeneric(transform)
}

fun <V> Result<V, AppError>.mapWhenBadRequestError(
  description: String,
  debugInfo: Map<String, Any?>? = null,
): Result<V, AppError> {
  return mapWhenErrorGeneric(::BadRequestError, description, debugInfo)
}

@PublishedApi
internal inline fun <V, reified E : AppError> Result<V, AppError>.mapErrorGeneric(
  constructor: (String, ResultError, Map<String, Any?>?, Long, Long) -> E,
  description: String,
  debugInfo: Map<String, Any?>?,
): Result<V, AppError> {

  return when {
    isOk -> this
    else -> Err(constructor(description, error, debugInfo, 2, 1))
  }
}

fun <V> Result<V, AppError>.mapToInternalError(
  description: String,
  debugInfo: Map<String, Any?>? = null,
): Result<V, AppError> {
  return mapErrorGeneric(::InternalAppError, description, debugInfo)
}

fun <V> Result<V, AppError>.mapToBadRequestError(
  description: String,
  debugInfo: Map<String, Any?>? = null,
): Result<V, AppError> {
  return mapErrorGeneric(::BadRequestError, description, debugInfo)
}
