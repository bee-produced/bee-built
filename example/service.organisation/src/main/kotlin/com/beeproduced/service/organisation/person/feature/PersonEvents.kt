package com.beeproduced.service.organisation.person.feature

import com.beeproduced.bee.buzz.manager.EventManager
import com.beeproduced.bee.buzz.requestHandler
import com.beeproduced.bee.functional.result.AppResult
import com.beeproduced.service.organisation.entities.Person
import com.beeproduced.service.organisation.events.*
import com.beeproduced.service.organisation.utils.organisationAdapter
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration

/**
 * @author Kacper Urbaniec
 * @version 2023-09-27
 */
@Configuration
class PersonEvents(private val eventManager: EventManager, private val service: PersonService) {
  @PostConstruct
  private fun register() {
    eventManager.register(requestHandler(::create))
    eventManager.register(requestHandler(::getAll))
    eventManager.register(requestHandler(::getByIds))
    eventManager.register(requestHandler(::exists))
  }

  private fun create(request: CreatePerson): AppResult<Person> =
    service.create(request.create, request.selection.organisationAdapter())

  private fun getAll(request: GetAllPersons): AppResult<Collection<Person>> =
    service.getAll(request.selection.organisationAdapter())

  private fun getByIds(request: GetPersonsByIds): AppResult<Collection<Person>> =
    service.getByIds(request.ids, request.selection.organisationAdapter())

  private fun exists(request: PersonsExist): AppResult<Unit> = service.exists(request.ids)
}
