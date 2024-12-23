package com.beeproduced.service.organisation.entities.input

import com.beeproduced.service.organisation.entities.PersonId

/**
 * @author Kacper Urbaniec
 * @version 2023-09-27
 */
data class CreateCompanyInput(
  val name: String,
  val address: CreateAddressInput? = null,
  val employees: Collection<PersonId>? = null,
)
