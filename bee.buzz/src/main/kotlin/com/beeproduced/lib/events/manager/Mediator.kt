package com.beeproduced.lib.events.manager

import com.beeproduced.lib.events.Notification
import com.beeproduced.lib.events.NotificationHandler
import com.beeproduced.lib.events.Request
import com.beeproduced.lib.events.RequestHandler
import com.beeproduced.result.errors.AppError
import com.github.michaelbull.result.Result

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2022-02-08
 */
interface Mediator {
    // fun register(event: String, callback: (args: Any?) -> Unit)
    // fun unregister(event: String, callback: (args: Any?) -> Unit)
    // fun notify(event: String, args: Any? = null)

    fun register(handler: RequestHandler<*, *>)
    fun unregister(handler: RequestHandler<*, *>)
    fun <T> send(request: Request<T>): Result<T, AppError>

    fun register(handler: NotificationHandler<*>)
    fun unregister(handler: NotificationHandler<*>)
    fun notify(notification: Notification)
}
