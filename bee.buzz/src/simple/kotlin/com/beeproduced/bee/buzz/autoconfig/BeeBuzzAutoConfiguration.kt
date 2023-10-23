package com.beeproduced.bee.buzz.autoconfig

import com.beeproduced.bee.buzz.manager.EventManager
import com.beeproduced.bee.buzz.manager.Mediator
import com.beeproduced.bee.buzz.manager.Scheduler
import com.beeproduced.bee.buzz.manager.SimpleEventManager
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-10-23
 */
@Configuration
@ConditionalOnClass(value = [EventManager::class, Mediator::class, Scheduler::class])
open class BeeBuzzAutoConfiguration {
    @Bean
    open fun beeBuzzEventManager() : EventManager {
        return SimpleEventManager()
    }
}