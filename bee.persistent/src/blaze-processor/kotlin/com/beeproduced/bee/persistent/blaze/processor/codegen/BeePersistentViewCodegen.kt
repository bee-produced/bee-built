package com.beeproduced.bee.persistent.blaze.processor.codegen

import com.beeproduced.bee.generative.util.PoetMap
import com.beeproduced.bee.generative.util.addNStatementBuilder
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentAnalyser.Companion.viewName
import com.beeproduced.bee.persistent.blaze.processor.info.*
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-12-16
 */
class BeePersistentViewCodegen(
    private val codeGenerator: CodeGenerator,
    private val dependencies: Dependencies,
    private val logger: KSPLogger,
    private val entities: List<EntityInfo>,
    private val config: BeePersistentBlazeConfig
) {
    private val packageName: String = config.viewPackageName
    private val fileName: String = "GeneratedViews"

    private val entitiesMap = entities.associateBy { it.qualifiedName }

    private val poetMap: PoetMap = mutableMapOf()
    private fun FunSpec.Builder.addNStatement(format: String)
        = addNStatementBuilder(format, poetMap)
    @Suppress("ConstPropertyName")
    object PoetConstants {

    }

    fun processViews(views: ViewInfo) {
        FileSpec
            .builder(packageName, fileName)
            .apply {
                addAnnotation(
                    AnnotationSpec.builder(ClassName("", "Suppress"))
                        .addMember("%S, %S", "ClassName", "RedundantVisibilityModifier")
                        .build()
                )
                for (entityView in views.entityViews.values) {
                    addType(TypeSpec.classBuilder(entityView.name)
                        .run {
                            if (entityView.superClassName != null)
                                buildSubEntityView(entityView)
                            else buildEntityView(entityView)
                        }
                        .build()
                    )
                }
                for (embeddedView in views.embeddedViews.values) {
                    addType(TypeSpec.classBuilder(embeddedView.name)
                       .buildEmbeddedView(embeddedView)
                       .build()
                    )
                }
            }
            .build()
            .writeTo(codeGenerator, dependencies)
    }

    private fun TypeSpec.Builder.buildEntityView(info: EntityViewInfo): TypeSpec.Builder {
        val entity = info.entity

        // Entity view annotation
        val entityViewAnnotation = AnnotationSpec
            .builder(ClassName("com.blazebit.persistence.view", "EntityView"))
            .addMember("%T::class", entity.declaration.toClassName())
            .build()
        addAnnotation(entityViewAnnotation)
        addModifiers(KModifier.ABSTRACT)
        if (entity.subClasses != null) {
            val inheritanceAnnotation = AnnotationSpec
                .builder(ClassName("com.blazebit.persistence.view", "EntityViewInheritance"))
                .build()
            addAnnotation(inheritanceAnnotation)
        }

        // Id column
        buildProperty(entity.id)

        // (Lazy) columns
        for (column in entity.lazyColumns + entity.columns) {
            buildProperty(column)
        }

        // Relation mappings
        for (relation in info.entityRelations) {

            val viewTypeStr = info.relations[relation.simpleName] ?: continue
            val viewType = ClassName(packageName, viewTypeStr)
            val isCollection = relation.type.declaration.qualifiedName?.asString()?.startsWith(
                "kotlin.collections."
            ) ?: false

            val propType = if (isCollection) {
                ClassName("kotlin.collections", "Collection")
                    .parameterizedBy(viewType)
            } else viewType

            addProperty(
                PropertySpec.builder(relation.simpleName, propType)
                    .addModifiers(KModifier.ABSTRACT)
                    .mutable(true)
                    .build()
            )
        }

        return this
    }

    private fun TypeSpec.Builder.buildSubEntityView(info: EntityViewInfo): TypeSpec.Builder {
        val entity = info.entity
        val superEntity = entitiesMap[info.entity.superClass!!]!!

        val superFields = superEntity.properties.mapTo(HashSet()) { it.simpleName }

        // Entity view annotation
        val entityViewAnnotation = AnnotationSpec
            .builder(ClassName("com.blazebit.persistence.view", "EntityView"))
            .addMember("%T::class", entity.declaration.toClassName())
            .build()
        addAnnotation(entityViewAnnotation)
        addModifiers(KModifier.ABSTRACT)
        superclass(ClassName(packageName, info.superClassName!!))

        // (Lazy) columns
        for (column in entity.lazyColumns + entity.columns) {
            if (superFields.contains(column.simpleName)) continue
            buildProperty(column)
        }

        // Relation mappings
        for (relation in info.entityRelations) {
            if (superFields.contains(relation.simpleName)) continue

            val viewTypeStr = info.relations[relation.simpleName] ?: continue
            val viewType = ClassName(packageName, viewTypeStr)
            val isCollection = relation.type.declaration.qualifiedName?.asString()?.startsWith(
                "kotlin.collections."
            ) ?: false

            val propType = if (isCollection) {
                ClassName("kotlin.collections", "Collection")
                    .parameterizedBy(viewType)
            } else viewType

            addProperty(
                PropertySpec.builder(relation.simpleName, propType)
                    .addModifiers(KModifier.ABSTRACT)
                    .mutable(true)
                    .build()
            )
        }

        return this
    }

    private val blazeIdMapping = AnnotationSpec.builder(ClassName("com.blazebit.persistence.view", "IdMapping"))
        .useSiteTarget(AnnotationSpec.UseSiteTarget.GET)
        .build()

    private fun TypeSpec.Builder.buildProperty(property: Property) {
        val propType = if (property.isValueClass) {
            requireNotNull(property.innerValue).type.toClassName()
        } else if(property.isEmbedded) {
            val embedded = property.embedded!!
            val name = viewName(embedded)
            ClassName(packageName, name)
        } else property.type.toClassName()
        addProperty(PropertySpec.builder(property.simpleName, propType)
            .addModifiers(KModifier.ABSTRACT)
            .mutable(true)
            .also {
                if (property !is IdProperty) return@also
                it.addAnnotation(blazeIdMapping)
            }
            .build())
    }

    private fun TypeSpec.Builder.buildEmbeddedView(view: EmbeddedViewInfo): TypeSpec.Builder {
        val info = view.embedded
        // Entity view annotation
        val entityViewAnnotation = AnnotationSpec
            .builder(ClassName("com.blazebit.persistence.view", "EntityView"))
            .addMember("%T::class", info.declaration.toClassName())
            .build()
        addAnnotation(entityViewAnnotation)
        addModifiers(KModifier.ABSTRACT)

        // (Lazy) columns
        for (column in info.lazyColumns + info.columns) {
            buildProperty(column)
        }

        return this
    }
}

typealias ViewCount = MutableMap<String, Int>