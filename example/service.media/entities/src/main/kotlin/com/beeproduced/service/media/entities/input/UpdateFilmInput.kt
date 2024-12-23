package com.beeproduced.service.media.entities.input

import com.beeproduced.service.media.entities.FilmId
import com.beeproduced.service.organisation.entities.CompanyId
import com.beeproduced.service.organisation.entities.PersonId

/**
 * @author Kacper Urbaniec
 * @version 2023-10-02
 */
data class UpdateFilmInput(
  val id: FilmId,
  val title: String?,
  val year: Int?,
  val synopsis: String?,
  val runtime: Int?,
  val studios: Collection<CompanyId>?,
  val directors: Collection<PersonId>?,
  val cast: Collection<PersonId>?,
)
