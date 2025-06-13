package com.beeproduced.bee.functional.extensions.com.github.michaelbull.result

import com.beeproduced.bee.functional.result.errors.AppError
import com.github.michaelbull.result.BindingScope
import com.github.michaelbull.result.Err

fun BindingScope<AppError>.returnErr(error: AppError): Nothing {
  val e = Err(error)
  // This bind will always throw an Exception for the `binding` scope
  e.bind<Any>()
  // This exception is used as a hint for the compiler
  throw Exception("")
}
