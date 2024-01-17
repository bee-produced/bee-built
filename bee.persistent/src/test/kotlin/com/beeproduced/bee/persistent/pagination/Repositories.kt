package com.beeproduced.bee.persistent.pagination

import com.beeproduced.bee.persistent.jpa.repository.BaseDataRepository
import com.beeproduced.bee.persistent.jpa.repository.extensions.Cursor
import com.beeproduced.bee.persistent.jpa.repository.extensions.Pagination
import com.beeproduced.bee.persistent.jpa.repository.extensions.PaginationException
import com.beeproduced.bee.persistent.jpa.repository.extensions.PaginationResult
import com.beeproduced.bee.persistent.selection.DataSelection
import com.linecorp.kotlinjdsl.query.spec.expression.ColumnSpec
import com.linecorp.kotlinjdsl.query.spec.expression.EntitySpec
import com.linecorp.kotlinjdsl.query.spec.predicate.EqualValueSpec
import com.linecorp.kotlinjdsl.querydsl.from.join
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

@Component
class PaginatedBarRepository(
    @Qualifier("orderEntityManager") em: EntityManager,
) : BaseDataRepository<PaginatedBar, PaginatedBarId>(em) {
    companion object {
        fun decodeCursor(c: String): Instant {
            val tmp = String(Base64.getDecoder().decode(c))
            val data = tmp.split("X")
            val seconds = data.first().toLong()
            val nano = data.last().toLong()
            return Instant.ofEpochSecond(seconds, nano)
        }

        fun encodeCursor(v: PaginatedBar): String {
            val c = v.createdOn
            val tmp = "${c.epochSecond}X${c.nano}"
            return Base64.getEncoder().encodeToString(tmp.toByteArray())
        }
    }

    data class PaginatedBarParameter(
        val createdBy: String,
        val first: Int? = null,
        val after: String? = null,
        val last: Int? = null,
        val before: String? = null
    )

    private val barPagination = Pagination(
        repository = this,
        orderBy = ColumnSpec(EntitySpec(PaginatedBar::class.java), PaginatedBar::createdOn.name),
        cursor = Cursor(
            decode = ::decodeCursor,
            encode = ::encodeCursor
        ),
        where = { w: String ->
            EqualValueSpec(
                ColumnSpec(EntitySpec(PaginatedBar::class.java), PaginatedBar::createdBy.name),
                w
            )
        }
    )

    fun pagination(
        input: PaginatedBarParameter,
        selection: DataSelection
    ): PaginationResult<PaginatedBar, String> {
        val (createdBy, first, after, last, before) = input

        if (first != null) return barPagination.forward(first, after, createdBy, selection)
        if (last != null) return barPagination.backward(last, before, createdBy, selection)

        throw PaginationException("Invalid parameters $input")
    }
}

@Component
class PaginatedFoxtrotRepository(
    @Qualifier("orderEntityManager") em: EntityManager,
) : BaseDataRepository<PaginatedFoxtrot, Long>(em) {
    companion object {
        fun decodeCursor(c: String): Instant {
            val tmp = String(Base64.getDecoder().decode(c))
            val data = tmp.split("X")
            val seconds = data.first().toLong()
            val nano = data.last().toLong()
            return Instant.ofEpochSecond(seconds, nano)
        }

        fun encodeCursor(v: PaginatedFoxtrot): String {
            val c = v.createdOn
            val tmp = "${c.epochSecond}X${c.nano}"
            return Base64.getEncoder().encodeToString(tmp.toByteArray())
        }
    }

    data class PaginatedFoxtrotParameter(
        val customCreatedBy: String,
        val first: Int? = null,
        val after: String? = null,
        val last: Int? = null,
        val before: String? = null
    )

    private val barPagination = Pagination(
        repository = this,
        orderBy = ColumnSpec(EntitySpec(PaginatedFoxtrot::class.java), PaginatedFoxtrot::createdOn.name),
        cursor = Cursor(
            decode = ::decodeCursor,
            encode = ::encodeCursor
        ),
        dsl = { join(PaginatedFoxtrot::infos) },
        where = { w: String ->
            EqualValueSpec(
                ColumnSpec(EntitySpec(FoxtrotInfo::class.java), FoxtrotInfo::customCreatedBy.name),
                w
            )
        }
    )

    fun pagination(
        input: PaginatedFoxtrotParameter,
        selection: DataSelection
    ): PaginationResult<PaginatedFoxtrot, String> {
        val (customCreatedBy, first, after, last, before) = input

        if (first != null) return barPagination.forward(first, after, customCreatedBy, selection)
        if (last != null) return barPagination.backward(last, before, customCreatedBy, selection)

        throw PaginationException("Invalid parameters $input")
    }
}

@Component
class FoxtrotInfoRepository(
    @Qualifier("orderEntityManager") em: EntityManager,
) : BaseDataRepository<FoxtrotInfo, Long>(em)