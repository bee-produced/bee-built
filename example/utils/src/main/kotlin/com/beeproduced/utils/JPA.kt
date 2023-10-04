package com.beeproduced.utils

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.util.*
import kotlin.collections.HashSet

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-09-27
 */
@Converter
class UUIDSetConverter : AttributeConverter<Set<UUID>, String> {
    override fun convertToDatabaseColumn(attribute: Set<UUID>?): String {
        if (attribute == null) return ""
        return attribute.joinToString(",")
    }

    override fun convertToEntityAttribute(dbData: String?): Set<UUID> {
        if (dbData.isNullOrEmpty()) return emptySet()
        return dbData.split(",").mapTo(HashSet()) { UUID.fromString(it) }
    }

}