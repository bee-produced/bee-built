package com.beeproduced.bee.functional.extensions.com.github.michaelbull.result

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result

// Utility for private class Failure:
// private class Failure<out E>(
//   val error: E,
// ) {
//   override fun equals(other: Any?): Boolean {
//     return other is Failure<*> && error == other.error
//   }
//
//   override fun hashCode(): Int {
//     return error.hashCode()
//   }
//
//   override fun toString(): String {
//     return "Failure($error)"
//   }
// }

@Suppress("NOTHING_TO_INLINE")
inline fun isFailure(obj: Any?): Boolean {
  val clazz = obj?.javaClass ?: return false
  return clazz.name == "com.github.michaelbull.result.Failure"
}

@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun failureAsErr(obj: Any?): Result<Nothing, *> {
  if (obj == null) return Err(null)
  return try {
    val errorField = obj.javaClass.getDeclaredField("error")
    errorField.isAccessible = true
    val e = errorField[obj]
    Err(e)
  } catch (_: Exception) {
    Err(null)
  }
}
