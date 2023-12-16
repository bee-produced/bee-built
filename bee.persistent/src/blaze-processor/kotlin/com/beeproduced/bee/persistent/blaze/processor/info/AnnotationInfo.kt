package com.beeproduced.bee.persistent.blaze.processor.info

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-12-16
 */
object AnnotationInfo {
    const val ANNOTATION_TRANSIENT = "jakarta.persistence.Transient"
    const val ANNOTATION_ID = "jakarta.persistence.Id"
    const val ANNOTATION_EMBEDDED_ID = "jakarta.persistence.EmbeddedId"
    const val ANNOTATION_GENERATED_VALUE = "jakarta.persistence.GeneratedValue"
    const val ANNOTATION_JVM_INLINE = "kotlin.jvm.JvmInline"
    const val ANNOTATION_ONE_TO_ONE = "jakarta.persistence.OneToOne"
    const val ANNOTATION_ONE_TO_MANY = "jakarta.persistence.OneToMany"
    const val ANNOTATION_MANY_TO_ONE = "jakarta.persistence.ManyToOne"
    const val ANNOTATION_MANY_TO_MANY = "jakarta.persistence.ManyToMany"
    const val ANNOTATION_INHERITANCE = "jakarta.persistence.Inheritance"
    val ANNOTATIONS_RELATION = setOf(
        ANNOTATION_ONE_TO_ONE,
        ANNOTATION_ONE_TO_MANY,
        ANNOTATION_MANY_TO_ONE,
        ANNOTATION_MANY_TO_MANY
    )
}