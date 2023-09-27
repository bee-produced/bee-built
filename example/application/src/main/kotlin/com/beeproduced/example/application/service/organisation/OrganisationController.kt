package com.beeproduced.example.application.service.organisation

import com.beeproduced.data.dgs.selection.toDataSelection
import com.beeproduced.lib.events.manager.EventManager
import com.beeproduced.result.AppResult
import com.beeproduced.result.errors.InternalAppError
import com.beeproduced.result.extensions.dgs.getDataFetcher
import com.beeproduced.service.organisation.events.GetAllCompanies
import com.beeproduced.service.organisation.events.GetAllPersons
import com.beeproduced.utils.logFor
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.map
import com.github.michaelbull.result.onFailure
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import java.util.UUID

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-09-26
 */

@DgsComponent
class OrganisationController(
    private val eventManager: EventManager,
    private val mapper: OrganisationMapper
) {
    private val logger = logFor<OrganisationController>()

    @DgsQuery
    fun persons(dfe: DataFetchingEnvironment): DataFetcherResult<Collection<PersonDto>> {
        logger.debug("persons()")
        return eventManager.send(
            GetAllPersons(dfe.selectionSet.toDataSelection())
        )
            .map(mapper::toPersonDtos)
            .onFailure { e -> logger.error(e.stackTraceToString()) }
            .getDataFetcher()
    }

    @DgsQuery
    fun companies(dfe: DataFetchingEnvironment): DataFetcherResult<Collection<CompanyDto>> {
        logger.debug("companies()")
        return eventManager.send(
            GetAllCompanies(dfe.selectionSet.toDataSelection())
        )
            .map(mapper::toCompanyDtos)
            .onFailure { e -> logger.error(e.stackTraceToString()) }
            .getDataFetcher()
    }
}