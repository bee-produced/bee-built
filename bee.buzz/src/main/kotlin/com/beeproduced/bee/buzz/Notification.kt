package com.beeproduced.bee.buzz

/**
 * Inspired by https://github.com/jbogard/MediatR/wiki.
 *
 * @author Kacper Urbaniec
 * @version 2022-02-10
 */
interface Notification

interface NotificationHandler<N : Notification> {
  val notificationClass: Class<N>

  fun handle(notification: N)
}

inline fun <reified N : Notification> notificationHandler(
  crossinline handler: (request: N) -> Unit
): NotificationHandler<N> =
  object : NotificationHandler<N> {
    override val notificationClass: Class<N> = N::class.java

    override fun handle(notification: N): Unit = handler(notification)
  }
