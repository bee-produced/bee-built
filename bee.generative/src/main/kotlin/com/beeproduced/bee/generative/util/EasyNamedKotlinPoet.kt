package com.beeproduced.bee.generative.util

import com.squareup.kotlinpoet.*

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-10-15
 */

class PoetMap {
    private val poetMappings = mutableMapOf<String, Any>()
    private val originalMappings = mutableMapOf<String, Any>()
    private val literalMappings = mutableMapOf<String, String>()
    private val stringMappings = mutableMapOf<String, String>()
    private val classMappings = mutableMapOf<String, ClassName>()

    val args get(): Map<String, Any> = poetMappings
    val mappings get(): Map<String, Any> = originalMappings

    fun literalMapping(key: String) = literalMappings.getValue(key)
    fun stringMapping(key: String) = stringMappings.getValue(key)
    fun classMapping(key: String) = classMappings.getValue(key)

    fun addMapping(key: String, mapping: Any) {
        originalMappings[key] = mapping
        addSpecialisedMapping(key, mapping)
        poetMappings[mappingKey(key)] = mapping
    }

    fun addMappings(map: Map<String, Any>) {
        originalMappings.putAll(map)
        map.forEach(::addSpecialisedMapping)
        map
            .mapKeys { (k) -> mappingKey(k) }
            .let { m -> poetMappings.putAll(m) }
    }

    private fun addSpecialisedMapping(key: String, mapping: Any) {
        if (key.endsWith(":L") && mapping is String) literalMappings[key] = mapping
        else if (key.endsWith(":S") && mapping is String) stringMappings[key] = mapping
        else if (key.endsWith(":T") && mapping is ClassName) classMappings[key] = mapping
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

    companion object {
        fun FunSpec.Builder.addNStatementBuilder(format: String, map: PoetMap): FunSpec.Builder {
            return addNamedCode(format, map.args).addStatement("")
        }

        fun CodeBlock.Builder.addNStatementBuilder(format: String, map: PoetMap): CodeBlock.Builder {
            return addNamed(format, map.args).addStatement("")
        }

        fun FileSpec.Builder.addNStatementBuilder(format: String, map: PoetMap): FileSpec.Builder {
            return addStatement(format, map.args).addStatement("")
        }
    }
}





