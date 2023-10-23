package com.beeproduced.result.errors.deprecated

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2022-01-29
 */
@Deprecated("Use new error handling")
interface AppError {
    val description: String
    val source: AppError?
    // val exception: Exception?  TODO: Discuss if this should be introduced, leads to transparten error handling when using java functions
}
