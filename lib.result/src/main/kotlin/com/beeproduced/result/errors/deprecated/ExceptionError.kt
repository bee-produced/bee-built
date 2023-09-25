package com.beeproduced.result.errors.deprecated

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2022-01-29
 */
@Deprecated("Use new error handling")
class ExceptionError(
    val exception: Throwable,
    description: String? = null,
    override val source: AppError? = null
) : AppError {
    override val description: String = description ?: exception.stackTraceToString()
}
