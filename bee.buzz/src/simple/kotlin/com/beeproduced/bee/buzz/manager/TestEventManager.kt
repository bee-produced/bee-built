package com.beeproduced.bee.buzz.manager

import com.beeproduced.bee.buzz.Request
import com.beeproduced.bee.functional.result.errors.AppError
import com.github.michaelbull.result.Result

open class TestEventManager : SimpleEventManager() {
    var sendInterceptor: (request: Request<*>, callOriginal: () -> Result<*, AppError>) -> Result<*, AppError> =
        { request, callOriginal ->
            callOriginal()
        }

    @Suppress("UNCHECKED_CAST")
    override fun <T> send(request: Request<T>): Result<T, AppError> {
        val callOriginal = { super.send(request) }
        return sendInterceptor(request, callOriginal) as Result<T, AppError>
    }

}