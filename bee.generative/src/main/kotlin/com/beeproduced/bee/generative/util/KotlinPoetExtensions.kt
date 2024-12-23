package com.beeproduced.bee.generative.util

import com.squareup.kotlinpoet.ClassName

/**
 * @author Kacper Urbaniec
 * @version 2023-10-14
 */
fun String.toPoetClassName(): ClassName {
  val lastIndex = this.lastIndexOf(".")
  if (lastIndex == -1) {
    // This means the provided string doesn't have a package part.
    // It's just a class without a package or it's an error.
    return ClassName("", this)
  }
  val packageName = this.substring(0, lastIndex)
  val simpleName = this.substring(lastIndex + 1)
  return ClassName(packageName, simpleName)
}
