package com.beeproduced.bee.persistent.blaze.processor.info

import com.beeproduced.bee.generative.util.DummyKSPropertyDeclaration
import com.beeproduced.bee.generative.util.DummyKSType
import com.beeproduced.bee.persistent.blaze.processor.utils.buildUniqueClassName
import com.google.devtools.ksp.symbol.*

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-12-16
 */

data class EntityInfo(
    val declaration: KSClassDeclaration,
    val annotations: List<ResolvedAnnotation>,
    val properties: List<EntityProperty>,
    val jpaProperties: List<EntityProperty>,
    val id: IdProperty,
    val columns: List<ColumnProperty>,
    val lazyColumns: List<ColumnProperty>,
    val relations: List<ColumnProperty>,
    val superClass: String?,
    val subClasses: Set<String>?
) {
    val simpleName: String = declaration.simpleName.asString()
    val qualifiedName: String = requireNotNull(declaration.qualifiedName).asString()
    val uniqueName: String = buildUniqueClassName(declaration.packageName.asString(), simpleName)
}

data class EmbeddedInfo(
    val declaration: KSClassDeclaration,
    val columns: List<ColumnProperty>,
    val lazyColumns: List<ColumnProperty>,
) {
    val simpleName: String = declaration.simpleName.asString()
    val qualifiedName: String = requireNotNull(declaration.qualifiedName).asString()
    val uniqueName: String = buildUniqueClassName(declaration.packageName.asString(), simpleName)
}

data class ResolvedAnnotation(
    val annotation: KSAnnotation,
    val declaration: KSDeclaration,
    val type: KSType
) {
    val simpleName: String get() = annotation.shortName.asString()
    val qualifiedName: String? get() = declaration.qualifiedName?.asString()
}

data class ResolvedValue(
    val declaration: KSDeclaration,
    val type: KSType
) {
    val simpleName: String get() = declaration.simpleName.asString()
    val qualifiedName: String? get() = declaration.qualifiedName?.asString()
}

interface AbstractProperty {
    val declaration: KSPropertyDeclaration
    val type: KSType
    val nonCollectionType: KSType
    val annotations: List<ResolvedAnnotation>
    val simpleName: String get() = declaration.simpleName.asString()
    val qualifiedName: String? get() = nonCollectionType.declaration.qualifiedName?.asString()
}

interface ValueClassProperty {
    val innerValue: ResolvedValue?
    val isValueClass: Boolean get() = innerValue != null
}

interface EmbeddedProperty {
    val embedded: EmbeddedInfo?
    val isEmbedded: Boolean get() = embedded != null
}

interface Property : AbstractProperty, ValueClassProperty, EmbeddedProperty

data class EntityProperty(
    override val declaration: KSPropertyDeclaration,
    override val type: KSType,
    override val annotations: List<ResolvedAnnotation>
) : AbstractProperty {
    override val nonCollectionType: KSType = getNonNullableSingleRepresentation(type)
}

data class IdProperty(
    override val declaration: KSPropertyDeclaration,
    override val type: KSType,
    override val annotations: List<ResolvedAnnotation>,
    override val innerValue: ResolvedValue?,
    val isGenerated: Boolean,
    override val embedded: EmbeddedInfo?
) : Property {
    override val nonCollectionType: KSType = getNonNullableSingleRepresentation(type)

    fun generatedDefaultValueLiteral(): String {
        if (type.isMarkedNullable) return "null"
        val typeName = nonCollectionType.declaration.simpleName.asString()
        return when (typeName) {
            "Double" -> "-1.0"
            "Float" -> "-1.0F"
            "Long" -> "-1L"
            "Int", "Short", "Byte" -> "-1"
            "Boolean" -> "false"
            "Char" -> "'\\u0000'"
            else -> "null"
        }
    }

    companion object {
        val PLACEHOLDER = IdProperty(
            declaration = DummyKSPropertyDeclaration(),
            type = DummyKSType(),
            annotations = emptyList(),
            innerValue = null,
            isGenerated = false,
            embedded = null
        )
    }
}

data class ColumnProperty(
    override val declaration: KSPropertyDeclaration,
    override val type: KSType,
    override val annotations: List<ResolvedAnnotation>,
    override val innerValue: ResolvedValue?,
    override val embedded: EmbeddedInfo?
) : Property {
    override val nonCollectionType: KSType = getNonNullableSingleRepresentation(type)
}

fun getNonNullableSingleRepresentation(type: KSType): KSType {
    // Check if the type is a generic type with type arguments (like Set<T>)
    if (type.arguments.isNotEmpty()) {
        // Get the first type argument, e.g., T in Set<T>
        val typeArgument = type.arguments.first().type

        // Check if the type argument is non-null
        return typeArgument?.resolve()?.let { typeArg ->
            if (typeArg.isMarkedNullable) {
                // If typeArg is nullable, get its non-nullable counterpart
                typeArg.makeNotNullable()
            } else {
                // If typeArg is already non-nullable, return it as is
                typeArg
            }
        } ?: type
    }
    return type
}