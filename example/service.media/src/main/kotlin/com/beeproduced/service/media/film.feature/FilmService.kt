package com.beeproduced.service.media.film.feature

import com.beeproduced.bee.persistent.jpa.repository.extensions.PaginationResult
import com.beeproduced.bee.persistent.selection.DataSelection
import com.beeproduced.lib.events.manager.EventManager
import com.beeproduced.result.AppResult
import com.beeproduced.result.errors.BadRequestError
import com.beeproduced.result.extensions.errors.andThenOnSuccess
import com.beeproduced.result.jpa.transactional.TransactionalResult
import com.beeproduced.service.media.entities.Film
import com.beeproduced.service.media.entities.FilmId
import com.beeproduced.service.media.entities.input.CreateFilmInput
import com.beeproduced.service.media.entities.input.FilmPagination
import com.beeproduced.service.media.entities.input.UpdateFilmInput
import com.beeproduced.service.organisation.entities.CompanyId
import com.beeproduced.service.organisation.entities.PersonId
import com.beeproduced.service.organisation.events.CompaniesExist
import com.beeproduced.service.organisation.events.PersonsExist
import com.beeproduced.utils.logFor
import com.github.michaelbull.result.*
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
        companyIds: Collection<CompanyId>?,
        personIds: Collection<PersonId>?
    ): AppResult<Unit> {
        val companyResult = if (companyIds.isNullOrEmpty()) Ok(Unit)
        else eventManager.send(CompaniesExist(companyIds))
        return companyResult.andThen {
            if (personIds.isNullOrEmpty()) Ok(Unit)
            else eventManager.send(PersonsExist(personIds))
        }
    }

    @TransactionalResult(
        "mediaTransactionManager",
        exceptionDescription = "Could not update film",
    )
    fun update(
        update: UpdateFilmInput,
        selection: DataSelection
    ): AppResult<Film> {
        logger.debug("update({}, {})", update, selection)
        return repository.selectById(update.id)
            .toResultOr { BadRequestError("No film with id ${update.id} found") }
            .andThenOnSuccess {
                val personIds = mutableListOf<PersonId>()
                    .also { update.directors?.let(it::addAll) }
                    .also { update.cast?.let(it::addAll) }
                organisationIdsExist(update.studios, personIds)
            }.map { film ->
                repository.update(film.copy(
                    title = update.title ?: film.title,
                    year = update.year ?: film.year,
                    synopsis = update.synopsis ?: film.synopsis,
                    runtime = update.runtime ?: film.runtime,
                    studioIds = update.studios?.toSet() ?: film.studioIds,
                    directorIds = update.directors?.toSet() ?: film.directorIds,
                    castIds = update.cast?.toSet() ?: film.castIds,
                ))
            }
    }

    @TransactionalResult(
        "mediaTransactionManager",
        exceptionDescription = "Could not fetch all films",
        readOnly = true
    )
    fun getRecentlyAdded(
        pagination: FilmPagination,
        selection: DataSelection
    ): AppResult<PaginationResult<Film, String>> {
        return Ok(repository.recentlyAdded(
            pagination.first,
            pagination.after,
            pagination.last,
            pagination.before,
            selection
        ))
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