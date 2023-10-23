package com.beeproduced.bee.persistent.jpa.meta

import com.beeproduced.bee.persistent.jpa.entity.annotations.SelectionName
import jakarta.persistence.*
import java.lang.reflect.Field

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-01-30
 */

class MembersInfo(
    val ids: Ids,
    val fields: Fields,
    val relations: Relations,
    val generated: Generated
    // TODO: separate between relations and selection relations to improve unproxy performance?
) {
    companion object {

        // Analyse helpers
        // ===============
        fun isFieldId(field: Field): Boolean {
            // Note: Does not work with: member: KProperty<*> => member.hasAnnotation<Id>()
            // as annotation has "AnnotationTarget.FIELD" and not "AnnotationTarget.PROPERTY"
            return field.isAnnotationPresent(Id::class.java)
        }

        fun isFieldRelation(field: Field): Boolean {
            return field.isAnnotationPresent(OneToOne::class.java) ||
                    field.isAnnotationPresent(OneToMany::class.java) ||
                    field.isAnnotationPresent(ManyToOne::class.java) ||
                    field.isAnnotationPresent(ManyToMany::class.java)
        }

        fun alternativeSelectionName(field: Field): String? {
            return field.getAnnotation(SelectionName::class.java)?.field
        }

        fun isGenerated(field: Field): Boolean {
            return field.isAnnotationPresent(GeneratedValue::class.java)
        }

        fun isTransient(field: Field): Boolean {
            return field.isAnnotationPresent(Transient::class.java)
        }

        fun isPrimitive(field: Field): Boolean {
            return field.type.isPrimitive
        }

    }
}

@JvmInline
value class Ids(private val ids: MutableMap<String, MemberInfo>) {
    val map get() = ids
    val keys get() = ids.keys
    val values get() = ids.values

    operator fun set(key: String, member: MemberInfo) {
        map[key] = member
    }

    operator fun get(key: String) = map[key]
}

@JvmInline
value class Fields(private val fields: MutableMap<String, MemberInfo>) {
    val map get() = fields
    val keys get() = fields.keys
    val values get() = fields.values

    operator fun set(key: String, member: MemberInfo) {
        map[key] = member
    }

    operator fun get(key: String) = map[key]
}

@JvmInline
value class Relations(private val relations: MutableMap<String, MemberInfo>) {
    val map get() = relations
    val keys get() = relations.keys
    val values get() = relations.values

    operator fun set(key: String, member: MemberInfo) {
        map[key] = member
    }

    operator fun get(key: String) = map[key]
}

@JvmInline
value class Generated(private val generated: MutableMap<String, GeneratedInfo>) {
    val map get() = generated
    val keys get() = generated.keys
    val values get() = generated.values

    operator fun set(key: String, generated: GeneratedInfo) {
        map[key] = generated
    }

    operator fun get(key: String) = map[key]
}


