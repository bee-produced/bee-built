package com.beeproduced.data.jpa.meta

import com.beeproduced.data.jpa.exceptions.UnsupportedRelationType
import java.lang.reflect.Field
import java.util.*
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.jvmErasure

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-01-30
 */
class MemberInfo(
    val member: KProperty<*>,
    val field: Field,
) {
    val type: Class<*>
    val isCollection: Boolean
    val nonCollectionType: Class<*>

    init {
        // Member type
        type = field.type
        // Determine member is a collection
        val retType = member.returnType
        val retClass = retType.jvmErasure.java
        // https://stackoverflow.com/a/4584552/12347616
        if (Collection::class.java.isAssignableFrom(retClass) ||
            Collections::class.java.isAssignableFrom(retClass)
        ) {
            // TODO: Support Maps/Dictionaries
            if (retType.arguments.count() != 1) {
                throw UnsupportedRelationType(retType, retType.arguments.count())
            }

            val genericClass = requireNotNull(retType.arguments.first().type).jvmErasure.java
            isCollection = true
            nonCollectionType = genericClass
        } else {
            isCollection = false
            nonCollectionType = type
        }
        // Performance optimization
        // https://stackoverflow.com/a/12996072/12347616
        // https://stackoverflow.com/a/10638943/12347616
        // As there are fundamentally the same, one call *should* be enough
        // but better safe than sorry
        field.isAccessible = true
        member.isAccessible = true
    }

    fun set(entity: Any, value: Any?) {
        field.set(entity, value)
    }

    fun get(entity: Any): Any? {
        return field.get(entity)
    }


}