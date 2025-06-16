package com.beeproduced.bee.functional.extensions.com.github.michaelbull.result

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result

// private const val RESULT_CLASS_NAME = "com.github.michaelbull.result.Result"
// private val resultClass: Class<*> = Class.forName(RESULT_CLASS_NAME)
// @PublishedApi
// internal val resultConstructor: Constructor<*> =
//   resultClass.declaredConstructors.first().apply { isAccessible = true }
//
// @Suppress("NOTHING_TO_INLINE")
// inline fun createResult(value: Any?): Result<*, *> {
//   return resultConstructor.newInstance(value) as Result<*, *>
// }
//
// val value = joinPoint.proceed()
// createResult(value) as Result<*, AppError>
// result

@Suppress("NOTHING_TO_INLINE")
inline fun boxResult(value: Any?): Result<*, *> {
  return when {
    isFailure(value) -> failureAsErr(value)
    else -> Ok(value)
  }
}
