package com.beeproduced.service.organisation.person.feature

import com.beeproduced.lib.events.manager.EventManager
import com.beeproduced.lib.events.requestHandler
import com.beeproduced.service.organisation.entities.Company
import com.beeproduced.service.organisation.entities.Person
import com.beeproduced.service.organisation.events.CreateCompany
import com.beeproduced.service.organisation.events.CreatePerson
import com.github.michaelbull.result.Ok
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration
import java.util.*

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-09-27
 */
@Configuration
class PersonEvents(
    private val eventManager: EventManager,
) {
    @PostConstruct
    private fun register() {
        eventManager.register(requestHandler { request: CreatePerson ->
            Ok(Person(UUID.randomUUID(), "", "", null, null, null))
        })
    }
}