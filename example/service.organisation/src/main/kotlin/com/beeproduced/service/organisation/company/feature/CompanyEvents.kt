package com.beeproduced.service.organisation.company.feature

import com.beeproduced.bee.buzz.manager.EventManager
import com.beeproduced.bee.buzz.requestHandler
import com.beeproduced.result.AppResult
import com.beeproduced.service.organisation.entities.Company
import com.beeproduced.service.organisation.events.CompaniesExist
import com.beeproduced.service.organisation.events.CreateCompany
import com.beeproduced.service.organisation.events.GetAllCompanies
import com.beeproduced.service.organisation.events.GetCompaniesByIds
import com.beeproduced.service.organisation.utils.organisationAdapter
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-09-27
 */
@Configuration
class CompanyEvents(
    private val eventManager: EventManager,
    private val service: CompanyService
) {
    @PostConstruct
    private fun register() {
        eventManager.register(requestHandler(::create))
        eventManager.register(requestHandler(::getAll))
        eventManager.register(requestHandler(::getByIds))
        eventManager.register(requestHandler(::exists))
    }

    private fun create(request: CreateCompany): AppResult<Company>
        = service.create(request.create, request.selection.organisationAdapter())
    private fun getAll(request: GetAllCompanies): AppResult<Collection<Company>>
        = service.getAll(request.selection.organisationAdapter())
    private fun getByIds(request: GetCompaniesByIds): AppResult<Collection<Company>>
        = service.getByIds(request.ids, request.selection.organisationAdapter())
    private fun exists(request: CompaniesExist): AppResult<Unit>
        = service.exists(request.ids)
}