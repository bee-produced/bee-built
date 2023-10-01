package com.beeproduced.service.media.film.feature

import com.beeproduced.data.selection.DataSelection
import com.beeproduced.lib.events.manager.EventManager
import com.beeproduced.result.AppResult
import com.beeproduced.result.errors.BadRequestError
import com.beeproduced.result.jpa.transactional.TransactionalResult
import com.beeproduced.service.media.entities.Film
import com.beeproduced.service.media.entities.FilmId
import com.beeproduced.service.media.entities.input.CreateFilmInput
import com.beeproduced.service.organisation.entities.CompanyId
import com.beeproduced.service.organisation.entities.PersonId
import com.beeproduced.service.organisation.events.CompaniesExist
import com.beeproduced.service.organisation.events.PersonsExist
import com.beeproduced.utils.logFor
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.map
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-09-27
 */
@Service
class FilmService(
    private val eventManager: EventManager,
    private val repository: FilmRepository,
) {
    private val logger = logFor<FilmRepository>()

    @TransactionalResult(
        "mediaTransactionManager",
        exceptionDescription = "Could not create film",
        readOnly = true
    )
    fun create(
        create: CreateFilmInput,
        selection: DataSelection
    ): AppResult<Film> {
        logger.debug("create({}, {})", create, selection)
        return organisationIdsExist(
            create.studios, create.directors + create.cast
        ).map {
            repository.persist(Film(
                UUID.randomUUID(),
                create.title,
                create.year,
                create.synopsis,
                create.runtime,
                create.studios.toSet(),
                create.directors.toSet(),
                create.cast.toSet(),
                Instant.now().truncatedTo(ChronoUnit.MICROS)
            ))
        }
    }

    fun organisationIdsExist(
        companyIds: Collection<CompanyId>,
        personIds: Collection<PersonId>
    ): AppResult<Unit> {
        val companyResult = if (companyIds.isEmpty()) Ok(Unit)
        else eventManager.send(CompaniesExist(companyIds))
        return companyResult.andThen {
            if (personIds.isEmpty()) Ok(Unit)
            else eventManager.send(PersonsExist(personIds))
        }
    }

    @TransactionalResult(
        "mediaTransactionManager",
        exceptionDescription = "Could not fetch all films",
        readOnly = true
    )
    fun getAll(selection: DataSelection): AppResult<List<Film>> {
        logger.debug("getAll({})", selection)
        return Ok(repository.select(selection))
    }

    @TransactionalResult(
        "organisationTransactionManager",
        exceptionDescription = "Could not fetch all films",
        readOnly = true
    )
    fun getByIds(ids: Collection<FilmId>, selection: DataSelection): AppResult<List<Film>> {
        logger.debug("getByIds({}, {})", ids, selection)
        val uniqueIds = ids.toSet()
        val films = repository.selectByIds(uniqueIds)
        if (films.count() == uniqueIds.count()) return Ok(films)
        return Err(BadRequestError("Could not find all persons"))
    }
}