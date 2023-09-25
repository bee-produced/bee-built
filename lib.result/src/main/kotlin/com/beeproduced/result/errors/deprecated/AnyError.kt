package com.beeproduced.result.errors.deprecated

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2022-03-10
 */
@Deprecated("Use new error handling")
class AnyError(
    val any: Any,
    description: String? = null,
    override val source: AppError? = null
) : AppError {
    override val description: String = description ?: any.toString()
}
