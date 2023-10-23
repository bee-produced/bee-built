package com.beeproduced.lib.data.pagination

import com.beeproduced.data.jpa.repository.BaseDataRepository
import com.beeproduced.data.jpa.repository.extensions.Cursor
import com.beeproduced.data.jpa.repository.extensions.Pagination
import com.beeproduced.data.jpa.repository.extensions.PaginationException
import com.beeproduced.data.jpa.repository.extensions.PaginationResult
import com.beeproduced.data.selection.DataSelection
import com.linecorp.kotlinjdsl.query.spec.expression.ColumnSpec
import com.linecorp.kotlinjdsl.query.spec.expression.EntitySpec
import com.linecorp.kotlinjdsl.query.spec.predicate.EqualValueSpec
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*

@Component
class PaginatedFooRepository(
    @Qualifier("orderEntityManager") em: EntityManager,
) : BaseDataRepository<PaginatedFoo, Long>(em) {
    companion object {
        fun decodeCursor(c: String): Instant {
            val tmp = String(Base64.getDecoder().decode(c))
            val data = tmp.split("X")
            val seconds = data.first().toLong()
            val nano = data.last().toLong()
            return Instant.ofEpochSecond(seconds, nano)
        }

        fun encodeCursor(v: PaginatedFoo): String {
            val c = v.createdOn
            val tmp = "${c.epochSecond}X${c.nano}"
            return Base64.getEncoder().encodeToString(tmp.toByteArray())
        }
    }

    data class PaginatedFooParameter(
        val createdBy: String,
        val first: Int? = null,
        val after: String? = null,
        val last: Int? = null,
        val before: String? = null
    )

    private val fooPagination = Pagination(
        repository = this,
        orderBy = ColumnSpec(EntitySpec(PaginatedFoo::class.java), PaginatedFoo::createdOn.name),
        cursor = Cursor(
            decode = ::decodeCursor,
            encode = ::encodeCursor
        ),
        where = { w: String ->
            EqualValueSpec(
                ColumnSpec(EntitySpec(PaginatedFoo::class.java), PaginatedFoo::createdBy.name),
                w
            )
        }
    )

    fun pagination(
        input: PaginatedFooParameter,
        selection: DataSelection
    ): PaginationResult<PaginatedFoo, String> {
        val (createdBy, first, after, last, before) = input

        if (first != null) return fooPagination.forward(first, after, createdBy, selection)
        if (last != null) return fooPagination.backward(last, before, createdBy, selection)

        throw PaginationException("Invalid parameters $input")
    }
}