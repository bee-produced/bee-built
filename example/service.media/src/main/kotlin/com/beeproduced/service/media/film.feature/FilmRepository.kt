package com.beeproduced.service.media.film.feature

import com.beeproduced.data.jpa.repository.BaseDataRepository
import com.beeproduced.service.media.entities.Film
import com.beeproduced.service.media.entities.FilmId
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-09-27
 */

@Component
class FilmRepository(
    @Qualifier("mediaEntityManager") em: EntityManager
) : BaseDataRepository<Film, FilmId>(em)