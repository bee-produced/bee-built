package com.beeproduced.bee.fetched.annotations

import kotlin.reflect.KClass

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-08-07
 */
@Target(AnnotationTarget.CLASS)
annotation class BeeFetched(
    val mappings: Array<FetcherMapping> = [],
    val ignore: Array<FetcherIgnore> = [],
    val safeMode: Boolean = true
)

annotation class FetcherMapping(
    val target: KClass<*>,
    val property: String,
    val idProperty: String
)

annotation class FetcherIgnore(
    val target: KClass<*>,
    val property: String = "",
)