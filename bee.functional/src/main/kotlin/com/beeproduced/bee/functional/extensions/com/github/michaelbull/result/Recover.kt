package com.beeproduced.bee.functional.extensions.com.github.michaelbull.result

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * @author Kacper Urbaniec
 * @version 2024-02-12
 */
@OptIn(ExperimentalContracts::class)
public inline fun <V, E> Result<V, E>.recoverIfPossible(
  predicate: (E) -> Boolean,
  transform: (E) -> Result<V, Any>,
): Result<V, E> {
  contract {
    callsInPlace(predicate, InvocationKind.AT_MOST_ONCE)
    callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
  }

  return when {
    isOk -> this
    else ->
      if (predicate(error)) {
        transform(error).mapError { error }
      } else {
        this
      }
  }
}
