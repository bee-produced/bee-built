package com.beeproduced.service.media.film.feature

import com.beeproduced.bee.persistent.jpa.repository.BaseDataRepository
import com.beeproduced.bee.persistent.jpa.repository.extensions.Cursor
import com.beeproduced.bee.persistent.jpa.repository.extensions.Pagination
import com.beeproduced.bee.persistent.jpa.repository.extensions.PaginationException
import com.beeproduced.bee.persistent.jpa.repository.extensions.PaginationResult
import com.beeproduced.bee.persistent.selection.DataSelection
import com.beeproduced.service.media.entities.Film
import com.beeproduced.service.media.entities.FilmId
import com.linecorp.kotlinjdsl.query.spec.expression.ColumnSpec
import com.linecorp.kotlinjdsl.query.spec.expression.EntitySpec
import jakarta.persistence.EntityManager
import java.time.Instant
import java.util.*
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

/**
 * @author Kacper Urbaniec
 * @version 2023-09-27
 */
@Component
class FilmRepository(@Qualifier("mediaEntityManager") em: EntityManager) :
  BaseDataRepository<Film, FilmId>(em) {

  companion object {
    private fun decodeCursor(c: String): Instant {
      val tmp = String(Base64.getDecoder().decode(c))
      val data = tmp.split("X")
      val seconds = data.first().toLong()
      val nano = data.last().toLong()
      return Instant.ofEpochSecond(seconds, nano)
    }

    private fun encodeCursor(v: Film): String {
      val c = v.addedOn
      val tmp = "${c.epochSecond}X${c.nano}"
      return Base64.getEncoder().encodeToString(tmp.toByteArray())
    }
  }

  private val recentlyAddedPagination =
    Pagination<Film, Instant, String, Any?>(
      repository = this,
      orderBy = ColumnSpec(EntitySpec(Film::class.java), Film::addedOn.name),
      cursor = Cursor(decode = ::decodeCursor, encode = ::encodeCursor),
    )

  fun recentlyAdded(
    first: Int?,
    after: String?,
    last: Int?,
    before: String?,
    selection: DataSelection,
  ): PaginationResult<Film, String> {
    return if (first != null) recentlyAddedPagination.forward(first, after, null, selection)
    else if (last != null) recentlyAddedPagination.backward(last, before, null, selection)
    else throw PaginationException("Invalid parameters")
  }
}
