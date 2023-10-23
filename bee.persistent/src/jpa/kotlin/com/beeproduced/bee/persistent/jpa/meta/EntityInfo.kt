package com.beeproduced.bee.persistent.jpa.meta

import jakarta.persistence.EmbeddedId
import jakarta.persistence.IdClass
import java.lang.reflect.Field
import java.lang.reflect.Method


/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-02-01
 */
class EntityInfo(
    val type: Class<*>,
    val idType: Class<*>,
    val members: MembersInfo
) {
    val nonPrimitiveIdType: Class<*>

    data class EntityToIdField(val entityField: Field, val idField: Field)

    val compositeKeyMapping: Set<EntityToIdField>?

    // https://www.baeldung.com/jpa-entity-table-names#defaultNames
    // https://stackoverflow.com/a/634629/12347616
    // val table: String


    init {
        // Check if type has "IdClass" annotation
        if (type.isAnnotationPresent(IdClass::class.java)) {
            // Map id properties if composite key
            nonPrimitiveIdType = idType
            compositeKeyMapping = idType.declaredFields.mapTo(HashSet()) { idField ->
                idField.isAccessible = true
                val entityField = type.getDeclaredField(idField.name)
                EntityToIdField(entityField, idField)
            }
        } else {
            nonPrimitiveIdType = wrapPrimitive(idType)
            compositeKeyMapping = null
        }
    }

    companion object {
        // Analyse helpers
        // ===============
        fun idClass(type: Class<*>): Class<*>? {
            return type.getAnnotation(IdClass::class.java)?.value!!.java
        }
    }

    // https://stackoverflow.com/questions/1704634/simple-way-to-get-wrapper-class-type-in-java
    // https://stackoverflow.com/a/50810905/12347616
    // https://discuss.kotlinlang.org/t/java-reflection-and-kotlin-primitive-types/18346/2
    private fun wrapPrimitive(type: Class<*>): Class<*> {
        if (!type.isPrimitive) return type
        if (type == Integer.TYPE) return Int::class.javaObjectType
        if (type == java.lang.Long.TYPE) return Long::class.javaObjectType
        if (type == java.lang.Boolean.TYPE) return Boolean::class.javaObjectType
        if (type == java.lang.Byte.TYPE) return Byte::class.javaObjectType
        if (type == Character.TYPE) return Char::class.javaObjectType
        if (type == java.lang.Float.TYPE) return Float::class.javaObjectType
        if (type == java.lang.Double.TYPE) return Double::class.javaObjectType
        if (type == java.lang.Short.TYPE) return Short::class.javaObjectType
        return if (type == Void.TYPE) Void::class.javaObjectType else type
    }

}
