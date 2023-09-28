package com.beeproduced.service.media.film.feature

import com.beeproduced.data.selection.DataSelection
import com.beeproduced.lib.events.manager.EventManager
import com.beeproduced.result.AppResult
import com.beeproduced.result.errors.BadRequestError
import com.beeproduced.result.jpa.transactional.TransactionalResult
import com.beeproduced.service.media.entities.Film
import com.beeproduced.service.media.entities.FilmId
import com.beeproduced.service.media.entities.input.CreateFilmInput
import com.beeproduced.utils.logFor
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import org.springframework.stereotype.Service

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
        TODO("not implemented")
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