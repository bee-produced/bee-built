package com.beeproduced.service.media.events

import com.beeproduced.data.selection.DataSelection
import com.beeproduced.lib.events.Request
import com.beeproduced.service.media.entities.Film
import com.beeproduced.service.media.entities.input.CreateFilmInput

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-09-26
 */

data class CreateFilm(
    val create: CreateFilmInput,
    val selection: DataSelection
): Request<Film>

data class GetAllFilms(
    val selection: DataSelection
): Request<Collection<Film>>