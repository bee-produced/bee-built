package com.beeproduced.example.application.configuration

import com.beeproduced.lib.events.manager.EventManager
import com.beeproduced.lib.events.manager.SimpleEventManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy

@EnableAspectJAutoProxy
@Configuration
class LibrarySetupConfiguration {

    @Bean
    fun eventManager() : EventManager {
        return SimpleEventManager()
    }
}