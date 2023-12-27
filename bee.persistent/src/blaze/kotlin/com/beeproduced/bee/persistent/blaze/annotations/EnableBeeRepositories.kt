package com.beeproduced.bee.persistent.blaze.annotations

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-12-27
 */
@Target(AnnotationTarget.CLASS)
annotation class EnableBeeRepositories(
    val basePackages: Array<String> = [],
    val entityManagerFactoryRef: String = "",
    val criteriaBuilderFactoryRef: String = "",
    val entitiyViewManagerRef: String = ""
)
