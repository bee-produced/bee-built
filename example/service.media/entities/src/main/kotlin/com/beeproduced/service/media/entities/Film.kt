package com.beeproduced.service.media.entities

import com.beeproduced.data.jpa.entity.DataEntity
import com.beeproduced.service.organisation.entities.CompanyId
import com.beeproduced.service.organisation.entities.PersonId
import com.beeproduced.utils.UUIDSetConverter
import jakarta.persistence.*
import java.util.UUID

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-09-27
 */

typealias FilmId = UUID

@Entity
@Table(name = "films")
data class Film(
    @Id
    @GeneratedValue
    val id: FilmId,
    val title: String,
    // Reserved keyword http://www.h2database.com/html/advanced.html#keywords
    @Column(name = "film_year")
    val year: Int,
    val synopsis: String,
    val runtime: Int,
    @Column(columnDefinition = "TEXT")
    @Convert(converter = UUIDSetConverter::class)
    val studios: Set<CompanyId>,
    @Column(columnDefinition = "TEXT")
    @Convert(converter = UUIDSetConverter::class)
    val directors: Set<PersonId>,
    // Reserved keyword http://www.h2database.com/html/advanced.html#keywords
    @Column(name = "film_cast", columnDefinition = "TEXT")
    @Convert(converter = UUIDSetConverter::class)
    val cast: Set<PersonId>,
) : DataEntity<Film> {
    override fun clone(): Film = this.copy()
}