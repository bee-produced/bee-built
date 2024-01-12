package com.beeproduced.bee.persistent.blaze.meta.dsl

import org.springframework.context.ApplicationEvent

import org.springframework.context.ApplicationListener




/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-12
 */

// TODO: Remove

class TestListener : ApplicationListener<ApplicationEvent?> {
    override fun onApplicationEvent(event: ApplicationEvent) {
        println("event: $event")
    }
}