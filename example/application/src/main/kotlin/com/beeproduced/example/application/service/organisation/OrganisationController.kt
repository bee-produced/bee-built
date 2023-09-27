package com.beeproduced.example.application.service.organisation

import com.beeproduced.lib.events.manager.EventManager
import com.beeproduced.result.AppResult
import com.beeproduced.result.errors.InternalAppError
import com.beeproduced.result.extensions.dgs.getDataFetcher
import com.beeproduced.utils.logFor
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.onFailure
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import graphql.execution.DataFetcherResult
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
    fun persons(): DataFetcherResult<Collection<PersonDto>> {
        logger.debug("persons()")

        val persons: AppResult<List<PersonDto>>
            = Err(InternalAppError("Something went wrong..."))

        return persons
            .onFailure { e -> logger.error(e.stackTraceToString()) }
            .getDataFetcher()
    }

    @DgsQuery
    fun companies(): DataFetcherResult<Collection<CompanyDto>> {
        logger.debug("companies()")

        val companies: AppResult<List<CompanyDto>> = Ok(listOf(
            CompanyDto(
                id = UUID.randomUUID().toString(),
                name = "MyCompany",
                employees = null,
                address = null
            )
        ))

        return companies
            .onFailure { e -> logger.error(e.stackTraceToString()) }
            .getDataFetcher()
    }
}