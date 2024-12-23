package com.beeproduced.bee.persistent.blaze.meta.dsl

import org.springframework.boot.context.event.ApplicationStartingEvent
import org.springframework.context.ApplicationListener

/**
 * @author Kacper Urbaniec
 * @version 2024-01-12
 */

// TODO: Remove

class TestListener : ApplicationListener<ApplicationStartingEvent> {
  override fun onApplicationEvent(event: ApplicationStartingEvent) {
    println("event: $event")
  }
}
