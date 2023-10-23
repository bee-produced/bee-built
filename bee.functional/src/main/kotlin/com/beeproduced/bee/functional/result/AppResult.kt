package com.beeproduced.bee.functional.result

import com.beeproduced.bee.functional.result.errors.AppError
import com.github.michaelbull.result.Result

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-03-10
 */

typealias AppResult<V> = Result<V, AppError>