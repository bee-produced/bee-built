package com.beeproduced.service.organisation.events

import com.beeproduced.data.selection.DataSelection
import com.beeproduced.lib.events.Request
import com.beeproduced.service.organisation.entities.Company
import com.beeproduced.service.organisation.entities.Person
import com.beeproduced.service.organisation.entities.input.CreateCompanyInput
import com.beeproduced.service.organisation.entities.input.CreatePersonInput

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-09-26
 */

data class CreateCompany(
    val create: CreateCompanyInput,
    val selection: DataSelection,
) : Request<Company>

data class GetAllCompanies(
    val selection: DataSelection
) : Request<Collection<Company>>

data class CreatePerson(
    val create: CreatePersonInput,
    val selection: DataSelection,
) : Request<Person>

data class GetAllPersons(
    val selection: DataSelection
) : Request<Collection<Person>>

