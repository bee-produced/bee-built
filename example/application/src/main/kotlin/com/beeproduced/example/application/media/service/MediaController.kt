package com.beeproduced.example.application.media.service

import com.beeproduced.data.dgs.selection.toDataSelection
import com.beeproduced.lib.events.manager.EventManager
import com.beeproduced.result.extensions.dgs.getDataFetcher
import com.beeproduced.service.media.events.GetRecentlyAddedFilms
import com.beeproduced.utils.logFor
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.map
import com.github.michaelbull.result.onFailure
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import graphql.execution.DataFetcherResult
import graphql.relay.Connection
import graphql.schema.DataFetchingEnvironment

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
            .map { input -> GetRecentlyAddedFilms(input, dfe.selectionSet.toDataSelection()) }
            .andThen(eventManager::send)
            .map(mapper::toDto)
            .onFailure { e -> logger.error(e.stackTraceToString()) }
            .getDataFetcher()
    }
}