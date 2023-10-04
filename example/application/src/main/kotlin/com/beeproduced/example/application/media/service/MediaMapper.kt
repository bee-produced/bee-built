package com.beeproduced.example.application.media.service

import com.beeproduced.data.jpa.repository.extensions.PaginationResult
import com.beeproduced.service.media.entities.Film
import com.beeproduced.service.media.entities.input.CreateFilmInput
import com.beeproduced.service.media.entities.input.FilmPagination
import com.beeproduced.service.media.entities.input.UpdateFilmInput
import graphql.relay.Connection
import graphql.relay.DefaultConnection
import graphql.relay.DefaultConnectionCursor
import graphql.relay.DefaultEdge
import graphql.relay.DefaultPageInfo
import org.mapstruct.Mapper
import org.mapstruct.Mapping

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-10-02
 */
@Mapper
abstract class MediaMapper {

    @Mapping(target = "studios", ignore = true)
    @Mapping(target = "directors", ignore = true)
    @Mapping(target = "cast", ignore = true)
    abstract fun toDto(entity: Film): FilmDto

    fun toDto(entity: PaginationResult<Film, String>): Connection<FilmDto> {
        val pageInfo = DefaultPageInfo(
            /* startCursor = */ entity.pageInfo?.startCursor?.let { DefaultConnectionCursor(it) },
            /* endCursor = */ entity.pageInfo?.endCursor?.let { DefaultConnectionCursor(it) },
            /* hasPreviousPage = */ entity.pageInfo?.hasPreviousPage ?: false,
            /* hasNextPage = */ entity.pageInfo?.hasNextPage ?: false
        )

        val edges = entity.edges.map { edge ->
            val dto = toDto(edge.node)
            val cursor = DefaultConnectionCursor(edge.cursor)
            DefaultEdge(dto, cursor)
        }

        return DefaultConnection(edges, pageInfo)
    }

    fun toPagination(
        first: Int?,
        after: String?,
        last: Int?,
        before: String?
    ): FilmPagination {
        return FilmPagination(
            first = first,
            after = after,
            last = last,
            before = before
        )
    }

    @Mapping(target = "studios", source = "studioIds")
    @Mapping(target = "directors", source = "directorIds")
    @Mapping(target = "cast", source = "castIds")
    abstract fun toEntity(dto: AddFilmDto): CreateFilmInput
    @Mapping(target = "studios", source = "studioIds")
    @Mapping(target = "directors", source = "directorIds")
    @Mapping(target = "cast", source = "castIds")
    abstract fun toEntity(dto: EditFilmDto): UpdateFilmInput
}