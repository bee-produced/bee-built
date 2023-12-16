package com.beeproduced.bee.persistent.blaze.processor.info

import com.google.devtools.ksp.symbol.*

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-12-16
 */

data class EntityInfo(
    val declaration: KSClassDeclaration,
    val properties: List<EntityProperty>,
    val id: IdProperty,
    val columns: List<ColumnProperty>,
    val lazyColumns: List<ColumnProperty>,
    val relations: List<ColumnProperty>
)

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
    val annotations: List<ResolvedAnnotation>
    val simpleName: String get() = declaration.simpleName.asString()
}

interface ValueClassProperty {
    val innerValue: ResolvedValue?
    val isValueClass: Boolean get() = innerValue != null
}

data class EntityProperty(
    override val declaration: KSPropertyDeclaration,
    override val type: KSType,
    override val annotations: List<ResolvedAnnotation>
) : AbstractProperty

data class IdProperty(
    override val declaration: KSPropertyDeclaration,
    override val type: KSType,
    override val annotations: List<ResolvedAnnotation>,
    override val innerValue: ResolvedValue?,
    val isGenerated: Boolean,
    val isEmbedded: Boolean,
) : AbstractProperty, ValueClassProperty {
    fun generatedDefaultValueLiteral(): String {
        val typeName = declaration.simpleName.asString()
        if (type.isMarkedNullable) return "null"
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
}

data class ColumnProperty(
    override val declaration: KSPropertyDeclaration,
    override val type: KSType,
    override val annotations: List<ResolvedAnnotation>,
    override val innerValue: ResolvedValue?,
) : AbstractProperty, ValueClassProperty