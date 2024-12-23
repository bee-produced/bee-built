package com.beeproduced.bee.persistent.fieldconverter

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class StringSetConverter : AttributeConverter<Set<String>, String> {
  override fun convertToDatabaseColumn(attribute: Set<String>?): String {
    if (attribute == null) return ""
    return attribute.joinToString(",")
  }

  override fun convertToEntityAttribute(dbData: String?): Set<String> {
    if (dbData.isNullOrEmpty()) return emptySet()
    return dbData.split(",").toSet()
  }
}
