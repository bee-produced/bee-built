package com.beeproduced.example.application

import com.beeproduced.data.selection.SimpleSelection
import com.beeproduced.lib.events.manager.EventManager
import com.beeproduced.result.AppResult
import com.beeproduced.service.organisation.entities.Company
import com.beeproduced.service.organisation.entities.Person
import com.beeproduced.service.organisation.entities.input.CreateAddressInput
import com.beeproduced.service.organisation.entities.input.CreateCompanyInput
import com.beeproduced.service.organisation.entities.input.CreatePersonInput
import com.beeproduced.service.organisation.events.CreateCompany
import com.beeproduced.service.organisation.events.CreatePerson
import com.beeproduced.utils.logFor
import com.github.michaelbull.result.getOrThrow
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.fakerConfig
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-09-27
 */
@Component
class DataGenerator(
    private val eventManager: EventManager
) {
    private val logger = logFor<DataGenerator>()
    private val faker = Faker(fakerConfig { locale = "de-AT" })

    private val personCount = 5
    private val companyCount = 2
    private lateinit var persons: List<Person>
    private lateinit var companies: List<Company>

    @EventListener(ApplicationReadyEvent::class)
    fun generate() {
        logger.info("Starting data generation...")
        setupPersons()
        setupCompanies()
        logger.info("Data generation finished")
    }

    private fun setupPersons() {
        persons = (0 until personCount).map {
            val create = CreatePersonInput(
                faker.name.neutralFirstName(),
                faker.name.lastName(),
                CreateAddressInput(
                    faker.address.streetAddress(),
                    faker.address.secondaryAddress(),
                    faker.address.postcode(),
                    faker.address.city()
                )
            )
            val selection = SimpleSelection(setOf())
            eventManager.send(CreatePerson(create, selection)).getOrFail()
        }
        logger.info("Created ${persons.count()} persons: ${persons.map { it.id }}")
    }

    private fun setupCompanies() {
        companies = persons.chunked(2).drop(1).map { p ->
            val create = CreateCompanyInput(
                faker.company.name(),
                CreateAddressInput(
                    faker.address.streetAddress(),
                    faker.address.secondaryAddress(),
                    faker.address.postcode(),
                    faker.address.city()
                ),
                p.map { it.id }
            )
            val selection = SimpleSelection(setOf())
            eventManager.send(CreateCompany(create, selection)).getOrFail()
        }
        logger.info("Created ${companies.count()} companies: ${companies.map { it.id }}")
    }

    private fun <V> AppResult<V>.getOrFail(): V {
        return getOrThrow { e -> IllegalStateException("Data generation failed: ${e.stackTraceToString()}") }
    }
}