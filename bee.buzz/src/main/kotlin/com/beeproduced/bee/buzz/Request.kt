package com.beeproduced.bee.buzz

import com.beeproduced.bee.functional.result.errors.AppError
import com.github.michaelbull.result.Result

/**
 * Inspired by https://github.com/jbogard/MediatR/wiki.
 *
 * @author Kacper Urbaniec
 * @version 2022-02-10
 */

interface Request<T>

interface RequestHandler<R : Request<T>, T> {
    val requestClass: Class<R>

    fun handle(request: R): Result<T, AppError>
}

inline fun <reified R : Request<T>, reified T> requestHandler(
    crossinline handler: (request: R) -> Result<T, AppError>
): RequestHandler<R, T> = object : RequestHandler<R, T> {
    override val requestClass: Class<R> = R::class.java
    override fun handle(request: R): Result<T, AppError> = handler(request)
}