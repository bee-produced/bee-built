package com.beeproduced.example.application.configuration

import com.beeproduced.result.dgs.DgsErrorHandlingConfiguration
import com.netflix.graphql.dgs.DgsScalar
import graphql.language.StringValue
import graphql.schema.Coercing
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2022-10-10
 */
@Configuration
@EnableAspectJAutoProxy
class DgsConfiguration : DgsErrorHandlingConfiguration()

/**
 * Based on: https://netflix.github.io/dgs/scalars/
 *
 * @author Kacper Urbaniec
 * @version 2022-09-15
 */
@DgsScalar(name = "DateTime")
class DateTimeScalar : Coercing<Instant?, String?> {
    companion object {
        // See: https://java2blog.com/format-instant-to-string-java/#Format_Instant_to_String_in_ISO-8601_format
        val formatter: DateTimeFormatter =
            DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.from(ZoneOffset.UTC))
    }

    @Throws(CoercingSerializeException::class)
    override fun serialize(dataFetcherResult: Any): String {
        return if (dataFetcherResult is Instant) {
            formatter.format(dataFetcherResult)
        } else {
            throw CoercingSerializeException("Not a valid Instant")
        }
    }

    @Throws(CoercingParseValueException::class)
    override fun parseValue(input: Any): Instant {
        return Instant.parse(input.toString())
    }

    @Throws(CoercingParseLiteralException::class)
    override fun parseLiteral(input: Any): Instant {
        if (input is StringValue) {
            return Instant.parse(input.value)
        }
        throw CoercingParseLiteralException("Value is not a valid ISO date time")
    }
}