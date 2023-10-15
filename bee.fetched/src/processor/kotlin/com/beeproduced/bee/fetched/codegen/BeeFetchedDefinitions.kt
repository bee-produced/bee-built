package com.beeproduced.bee.fetched.codegen

import com.beeproduced.bee.generative.util.toPoetClassName
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-10-14
 */

data class DgsDto(
    val name: String,
    val properties: List<PropertyDetails>
)

data class PropertyDetails(
    val name: String,
    val nonCollectionType: String,
    val isCollection: Boolean,
    val isNullable: Boolean
) {
    fun toPoetTypename(): TypeName {
        val cls = if (isCollection) {
            ClassName("kotlin.collections", "List")
                .parameterizedBy(nonCollectionType.toPoetClassName())
        } else nonCollectionType.toPoetClassName()
        return cls.copy(isNullable)
    }
}

data class DataLoaderDefinition(
    val keyType: String,
    val dtoType: String,
    val nullableKey: Boolean,
    val nullableDto: Boolean,
    val autoFetcher: AutoFetcherDefinition,
    // Using annotations directly is not a good idea...
    // org.jetbrains.kotlin.backend.common.BackendException: Backend Internal error: Exception during IR lowering
    // File being compiled: C:/.../lib.data/src/dgs/kotlin/com/beeproduced/data/dgs/processor/AutoFetcherVisitor.kt
    // The root cause java.lang.NullPointerException was thrown at: org.jetbrains.kotlin.backend.jvm.lower.JvmAnnotationImplementationTransformer
    // val dataLoader: DgsDataLoader
    val dataLoader: String,
)

// Mimics annotations as instancing them here leads to troubles
// E.g. KClass is hard to get from KSType and error-prone
// https://github.com/google/ksp/issues/1038 (generated dgs code)
data class AutoFetcherDefinition(
    val mappings: List<FetcherMappingDefinition>,
    val internalTypes: List<FetcherInternalTypeDefinition>,
    val ignore: List<FetcherIgnoreDefinition>,
    val safeMode: Boolean,
)

data class FetcherMappingDefinition(
    val target: String,
    val property: String,
    val idProperty: String,
)

data class FetcherIgnoreDefinition(
    val target: String,
    val property: String?,
)

data class FetcherInternalTypeDefinition(
    val target: String,
    val internal: String,
    val property: String?
)
