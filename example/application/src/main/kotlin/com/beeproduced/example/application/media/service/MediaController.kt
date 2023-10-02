package com.beeproduced.example.application.media.service

import com.beeproduced.data.dgs.selection.toDataSelection
import com.beeproduced.example.application.organisation.service.*
import com.beeproduced.lib.events.manager.EventManager
import com.beeproduced.result.extensions.dgs.getDataFetcher
import com.beeproduced.service.media.events.GetRecentlyAddedFilms
import com.beeproduced.utils.logFor
import com.github.michaelbull.result.*
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import graphql.execution.DataFetcherResult
import graphql.relay.Connection
import graphql.schema.DataFetchingEnvironment
import java.util.concurrent.CompletableFuture

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-09-27
 */
@DgsComponent
class MediaController(
    val eventManager: EventManager,
    val mapper: MediaMapper
) {
    private val logger = logFor<MediaController>()

    @DgsQuery
    fun recentlyAddedFilms(
        @InputArgument("last") last: Int?,
        @InputArgument("before") before: String?,
        @InputArgument("first") first: Int?,
        @InputArgument("after") after: String?,
        dfe: DataFetchingEnvironment
    ): DataFetcherResult<Connection<FilmDto>> {
        return Ok(mapper.toPagination(first, after, last, before))
            .onSuccess { dfe.setContext(dfe) }
            .map { input -> GetRecentlyAddedFilms(input, dfe.selectionSet.toDataSelection()) }
            .andThen(eventManager::send)
            .map(mapper::toDto)
            .onFailure { e -> logger.error(e.stackTraceToString()) }
            .getDataFetcher()
    }

    @DgsData(parentType = "Film", field = "studios")
    fun filmStudios(dfe: DataFetchingEnvironment): CompletableFuture<List<CompanyDto>?> {
        val dataLoader = dfe.getCompanyDataLoader()
        val film = dfe.getSource<FilmDto>()
        return dataLoader.loadMany(film.studioIds)
    }

    @DgsData(parentType = "Film", field = "directors")
    fun filmDirectors(dfe: DataFetchingEnvironment): CompletableFuture<List<PersonDto>?> {
        val dataLoader = dfe.getPersonDataLoader()
        val film = dfe.getSource<FilmDto>()
        return dataLoader.loadMany(film.directorIds)
    }

    @DgsData(parentType = "Film", field = "cast")
    fun filmCast(dfe: DataFetchingEnvironment): CompletableFuture<List<PersonDto>?> {
        val dataLoader = dfe.getPersonDataLoader()
        val film = dfe.getSource<FilmDto>()
        return dataLoader.loadMany(film.castIds)
    }
}