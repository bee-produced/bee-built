package com.beeproduced.service.organisation.person.feature

import com.beeproduced.lib.events.manager.EventManager
import com.beeproduced.lib.events.requestHandler
import com.beeproduced.result.AppResult
import com.beeproduced.service.organisation.entities.Person
import com.beeproduced.service.organisation.events.CreatePerson
import com.beeproduced.service.organisation.events.GetAllPersons
import com.beeproduced.service.organisation.events.PersonsExist
import com.beeproduced.service.organisation.utils.organisationAdapter
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-09-27
 */
@Configuration
class PersonEvents(
    private val eventManager: EventManager,
    private val service: PersonService,
) {
    @PostConstruct
    private fun register() {
        eventManager.register(requestHandler(::create))
        eventManager.register(requestHandler(::getAll))
        eventManager.register(requestHandler(::exists))
    }

    private fun create(request: CreatePerson): AppResult<Person>
        = service.create(request.create, request.selection.organisationAdapter())
    private fun getAll(request: GetAllPersons): AppResult<Collection<Person>>
        = service.getAll(request.selection.organisationAdapter())
    private fun exists(request: PersonsExist): AppResult<Unit>
        = service.exists(request.ids)
}