package com.beeproduced.service.organisation.person.feature

import com.beeproduced.bee.persistent.jpa.repository.BaseDataRepository
import com.beeproduced.service.organisation.entities.Person
import com.beeproduced.service.organisation.entities.PersonId
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

/**
 * @author Kacper Urbaniec
 * @version 2023-09-27
 */
@Component
class PersonRepository(@Qualifier("organisationEntityManager") em: EntityManager) :
  BaseDataRepository<Person, PersonId>(em)
