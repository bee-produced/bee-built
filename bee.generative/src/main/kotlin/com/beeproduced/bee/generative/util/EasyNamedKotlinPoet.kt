package com.beeproduced.bee.generative.util

import com.squareup.kotlinpoet.FunSpec

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-10-15
 */

typealias PoetMap = MutableMap<String, Any>

fun PoetMap.addMapping(key: String, mapping: Any) {
    put(mappingKey(key), mapping)
}

fun PoetMap.addMappings(map: Map<String, Any>) {
    map
        .mapKeys { (k) -> mappingKey(k) }
        .let { m -> putAll(m) }
}

fun FunSpec.Builder.addNStatementBuilder(format: String, map: PoetMap): FunSpec.Builder {
    return addNamedCode(format, map).addStatement("")
}

private fun mappingKey(input: String): String {
    // Check if the string starts with "%" and remove it if true
    val withoutPrefix = if (input.startsWith("%")) input.substring(1) else input
    // Check if the string has a ":" followed by another character and remove them if true
    return if (withoutPrefix.contains(":")) withoutPrefix.substring(
        0,
        withoutPrefix.lastIndexOf(":")
    ) else withoutPrefix
}