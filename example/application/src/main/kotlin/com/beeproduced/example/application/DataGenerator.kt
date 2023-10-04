package com.beeproduced.example.application

import com.beeproduced.data.selection.SimpleSelection
import com.beeproduced.lib.events.manager.EventManager
import com.beeproduced.result.AppResult
import com.beeproduced.service.media.entities.Film
import com.beeproduced.service.media.entities.input.CreateFilmInput
import com.beeproduced.service.media.events.CreateFilm
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
    private val filmCount = 10
    private lateinit var persons: List<Person>
    private lateinit var companies: List<Company>
    private lateinit var films: List<Film>

    @EventListener(ApplicationReadyEvent::class)
    fun generate() {
        logger.info("Starting data generation...")
        setupPersons()
        setupCompanies()
        setupFilms()
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

    private fun setupFilms() {
        val studios = listOf(companies.random())
        val directors = listOf(persons.random())
        val cast = listOf(persons.random(), persons.random())
        films = (0 until filmCount).map {
            val create = CreateFilmInput(
                faker.movie.title(),
                (1960..2023).random(),
                faker.movie.quote(),
                (40..180).random(),
                studios.map { it.id },
                directors.map { it.id },
                cast.map { it.id }
            )
            val selection = SimpleSelection(setOf())
            eventManager.send(CreateFilm(create, selection)).getOrFail()
        }
        logger.info("Created ${films.count()} films: ${films.map { it.id }}")
    }

    private fun <V> AppResult<V>.getOrFail(): V {
        return getOrThrow { e -> IllegalStateException("Data generation failed: ${e.stackTraceToString()}") }
    }
}