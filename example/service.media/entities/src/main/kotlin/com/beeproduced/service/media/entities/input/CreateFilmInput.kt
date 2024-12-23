package com.beeproduced.service.media.entities.input

import com.beeproduced.service.organisation.entities.CompanyId
import com.beeproduced.service.organisation.entities.PersonId

/**
 * @author Kacper Urbaniec
 * @version 2023-09-27
 */
data class CreateFilmInput(
  val title: String,
  val year: Int,
  val synopsis: String,
  val runtime: Int,
  val studios: Collection<CompanyId>,
  val directors: Collection<PersonId>,
  val cast: Collection<PersonId>,
)
