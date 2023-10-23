package com.beeproduced.bee.persistent.jpa.entity.annotations

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-01-30
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class SelectionName(val field: String)


// TODO: DataSelectionContinue
// GraphQL: A -> C
// JPA : A1 -> A2 -> C
// Add annotation "DataSelectionContinue" on A1 member to continue selection search
// With current selection & NOT subselect

// @Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER)
// @Retention(AnnotationRetention.RUNTIME)
// annotation class DataSelectionContinue(val field: String)