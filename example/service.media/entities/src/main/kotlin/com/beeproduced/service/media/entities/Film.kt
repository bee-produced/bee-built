package com.beeproduced.service.media.entities

import com.beeproduced.bee.persistent.jpa.entity.DataEntity
import com.beeproduced.service.organisation.entities.CompanyId
import com.beeproduced.service.organisation.entities.PersonId
import com.beeproduced.utils.UUIDSetConverter
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/**
 * @author Kacper Urbaniec
 * @version 2023-09-27
 */
typealias FilmId = UUID

@Entity
@Table(name = "films")
data class Film(
  @Id @GeneratedValue val id: FilmId,
  val title: String,
  // Reserved keyword http://www.h2database.com/html/advanced.html#keywords
  @Column(name = "film_year") val year: Int,
  val synopsis: String,
  val runtime: Int,
  @Column(name = "studio_ids", columnDefinition = "TEXT")
  @Convert(converter = UUIDSetConverter::class)
  val studioIds: Set<CompanyId>,
  @Column(name = "director_ids", columnDefinition = "TEXT")
  @Convert(converter = UUIDSetConverter::class)
  val directorIds: Set<PersonId>,
  @Column(name = "cast_ids", columnDefinition = "TEXT")
  @Convert(converter = UUIDSetConverter::class)
  val castIds: Set<PersonId>,
  @Column(name = "added_on") val addedOn: Instant,
) : DataEntity<Film> {
  override fun clone(): Film = this.copy()
}
