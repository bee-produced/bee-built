package com.beeproduced.bee.buzz.manager

import com.beeproduced.bee.buzz.Notification
import com.beeproduced.bee.buzz.NotificationHandler
import com.beeproduced.bee.buzz.Request
import com.beeproduced.bee.buzz.RequestHandler
import com.beeproduced.bee.buzz.manager.exceptions.NotificationHandlerNotFound
import com.beeproduced.bee.buzz.manager.exceptions.RequestHandlerNotFound
import com.beeproduced.bee.functional.result.errors.AppError
import com.github.michaelbull.result.Result
import org.slf4j.LoggerFactory
import org.springframework.util.LinkedMultiValueMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * EventManager = Mediator + Scheduler
 * Allows easy communication between services with loose coupling.
 *
 * Inspired by https://www.codeproject.com/Articles/35277/MVVM-Mediator-Pattern
 * and https://www.baeldung.com/java-delay-code-execution#service.
 * Additional info found in https://discuss.kotlinlang.org/t/how-to-bridge-java-runnable-with-kotlin-closures/996/3.
 *
 * @author Kacper Urbaniec
 * @version 2022-02-09
 */
open class SimpleEventManager : EventManager {
    val logger = LoggerFactory.getLogger(SimpleEventManager::class.java)
    val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    val requestHandlers = mutableMapOf<Class<*>, RequestHandler<*, *>>()
    val notificationHandlers = LinkedMultiValueMap<Class<*>, NotificationHandler<*>>()

    override fun register(handler: RequestHandler<*, *>) {
        if (requestHandlers.containsKey(handler.requestClass)) {
            logger.warn(
                "Request handler [${requestHandlers[handler.requestClass]?.javaClass?.typeName}] " +
                        "for request [${handler.requestClass.typeName}] will be replaced "
            )
        }
        requestHandlers[handler.requestClass] = handler
        logger.info(
            "Registered request handler [${handler.javaClass.typeName}] for request " +
                    "[${handler.requestClass.typeName}]"
        )
    }

    override fun unregister(handler: RequestHandler<*, *>) {
        if (!requestHandlers.containsKey(handler.requestClass)) {
            logger.warn(
                "No request handler for request [${handler.requestClass.typeName}] " +
                        "to be unregistered"
            )
        } else {
            requestHandlers.remove(handler.requestClass)
            logger.info(
                "Unregistered request handler [${handler.javaClass.typeName}] for request " +
                        "[${handler.requestClass.typeName}]"
            )
        }
    }

    override fun <T> send(request: Request<T>): Result<T, AppError> {
        return if (!requestHandlers.containsKey(request.javaClass)) {
            throw RequestHandlerNotFound("No request handler for request [${request.javaClass.typeName}] found")
            // Err(
            //     EventError(
            //         "No request handler for request [${request.javaClass.typeName}] found"
            //     )
            // )
        } else {
            // See: https://kotlinlang.org/docs/typecasts.html#unchecked-casts
            // And: https://stackoverflow.com/q/57240983/12347616
            @Suppress("UNCHECKED_CAST")
            val handler = requestHandlers[request.javaClass]!!
                    as RequestHandler<Request<T>, T>
            logger.debug("Invoking handler [${handler.javaClass.typeName}]")
            handler.handle(request)
        }
    }

    override fun register(handler: NotificationHandler<*>) {
        // if (notificationHandlers.containsKey(handler.notificationClass)) {
        //     logger.warn(
        //         "Notification handler [${notificationHandlers[handler.notificationClass]?.javaClass?.typeName}] " +
        //             "for notification [${handler.notificationClass.typeName}] will be replaced "
        //     )
        // }
        val handlers = notificationHandlers[handler.notificationClass]
        if (handlers != null) handlers.add(handler)
        else notificationHandlers[handler.notificationClass] = handler
        logger.info(
            "Registered notification handler [${handler.javaClass.typeName}] for notification " +
                    "[${handler.notificationClass.typeName}]"
        )
    }

    override fun unregister(handler: NotificationHandler<*>) {
        if (!notificationHandlers.containsKey(handler.notificationClass)) {
            logger.warn(
                "No notification handler for notification [${handler.notificationClass.typeName}] " +
                        "to be unregistered"
            )
        } else {
            notificationHandlers.remove(handler.notificationClass)
            logger.info(
                "Unregistered notification handler [${handler.javaClass.typeName}] for notification " +
                        "[${handler.notificationClass.typeName}]"
            )
        }
    }

    override fun notify(notification: Notification) {
        if (!notificationHandlers.containsKey(notification.javaClass)) {
            throw NotificationHandlerNotFound("No notification handlers for notification [${notification.javaClass}] found")
            // logger.warn("No notification handlers for notification [${notification.javaClass}] found")
        } else {
            val handlers = notificationHandlers[notification.javaClass]!!
            logger.info("Invoking ${handlers.count()} handler(s) for notification [${notification.javaClass.typeName}]")
            for (handlerToCast in handlers) {
                @Suppress("UNCHECKED_CAST")
                val handler = handlerToCast as NotificationHandler<Notification>
                logger.debug("Invoking handler [${handler.javaClass.typeName}]")
                handler.handle(notification)
            }
        }
    }

    override fun schedule(f: () -> Unit, delay: Long, unit: TimeUnit): ScheduledFuture<Unit> {
        logger.info("Scheduled function [${f.javaClass.typeName}] to run in $delay $unit")
        return scheduler.schedule<Unit>(f, delay, unit)
    }

    // val events = LinkedMultiValueMap<String, (args: Any?) -> Unit>()
    // override fun register(event: String, callback: (args: Any?) -> Unit) {
    //     events.add(event, callback)
    //     logger.info("Registered callback [${callback.javaClass.typeName}] for event $event")
    // }
    //
    // override fun unregister(event: String, callback: (args: Any?) -> Unit) {
    //     val unregister = events[event]?.remove(callback) ?: false
    //     if (!unregister)
    //         logger.warn("Could not unregister callback [${callback.javaClass.typeName}] for event $event")
    //     else logger.info("Unregistered callback [${callback.javaClass.typeName}] for event $event")
    // }
    //
    // override fun notify(event: String, args: Any?) {
    //     if (events.containsKey(event)) {
    //         val callbacks = events[event]!!
    //         logger.info("Invoking ${callbacks.count()} callbacks for event $event")
    //         for (callback in callbacks) {
    //             logger.debug("Invoking [${callback.javaClass.typeName}]")
    //             callback(args)
    //         }
    //     } else logger.warn("No callbacks for event $event found")
    // }
}