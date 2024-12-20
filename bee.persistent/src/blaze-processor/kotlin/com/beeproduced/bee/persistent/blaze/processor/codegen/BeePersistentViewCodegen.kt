package com.beeproduced.bee.persistent.blaze.processor.codegen

import com.beeproduced.bee.generative.util.PoetMap
import com.beeproduced.bee.generative.util.PoetMap.Companion.addNStatementBuilder
import com.beeproduced.bee.generative.util.toPoetClassName
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentAnalyser.Companion.viewName
import com.beeproduced.bee.persistent.blaze.processor.info.EntityInfo
import com.beeproduced.bee.persistent.blaze.processor.info.IdProperty
import com.beeproduced.bee.persistent.blaze.processor.info.Property
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo

/**
 * @author Kacper Urbaniec
 * @version 2023-12-16
 */
class BeePersistentViewCodegen(
  private val codeGenerator: CodeGenerator,
  private val dependencies: Dependencies,
  private val logger: KSPLogger,
  private val entities: List<EntityInfo>,
  private val config: BeePersistentBlazeConfig,
) {
  private val packageName: String = config.viewPackageName
  private val fileName: String = "GeneratedViews"

  private val entitiesMap = entities.associateBy { it.qualifiedName }

  private val poetMap: PoetMap = PoetMap()

  private fun FunSpec.Builder.addNStatement(format: String) = addNStatementBuilder(format, poetMap)

  @Suppress("ConstPropertyName") object PoetConstants {}

  fun processViews(views: ViewInfo) {
    FileSpec.builder(packageName, fileName)
      .apply {
        addAnnotation(
          AnnotationSpec.builder(ClassName("", "Suppress"))
            .addMember("%S, %S", "ClassName", "RedundantVisibilityModifier")
            .build()
        )
        for (entityView in views.entityViews.values) {
          addType(
            TypeSpec.classBuilder(entityView.name)
              .run {
                if (entityView.superClassName != null) buildSubEntityView(entityView, views)
                else buildEntityView(entityView, views)
              }
              .build()
          )
        }
        for (embeddedView in views.embeddedViews.values) {
          addType(TypeSpec.classBuilder(embeddedView.name).buildEmbeddedView(embeddedView).build())
        }
      }
      .build()
      .writeTo(codeGenerator, dependencies)
  }

  // TODO: unify & streamline buildEntityView / buildSubEntityView

  private fun TypeSpec.Builder.buildEntityView(
    info: EntityViewInfo,
    views: ViewInfo,
  ): TypeSpec.Builder {
    val entity = info.entity

    // Entity view annotation
    val entityViewAnnotation =
      AnnotationSpec.builder(ClassName("com.blazebit.persistence.view", "EntityView"))
        .addMember("%T::class", entity.declaration.toClassName())
        .build()
    addAnnotation(entityViewAnnotation)
    addModifiers(KModifier.ABSTRACT)
    if (entity.subClasses != null) {
      val inheritanceAnnotation =
        AnnotationSpec.builder(ClassName("com.blazebit.persistence.view", "EntityViewInheritance"))
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
      val propTypeQualifiedName = relation.type.declaration.qualifiedName?.asString()
      val isCollection = propTypeQualifiedName?.startsWith("kotlin.collections.") ?: false

      val propType =
        if (isCollection && propTypeQualifiedName != null) {
          propTypeQualifiedName.toPoetClassName().parameterizedBy(viewType)
        } else viewType

      addProperty(
        PropertySpec.builder(relation.simpleName, propType)
          .addModifiers(KModifier.ABSTRACT)
          .also {
            // Multiple m:n relations lead to incomplete entities when joined
            // After first m:n relation, every following of the same type will
            // be fetched by individual select statements
            val viewInfo = views.entityViews.getValue(viewTypeStr)
            if (viewInfo.isExtended && isCollection) {
              it.addAnnotation(blazeFetchMapping)
            }
            // 1:1 relations to the same type do not trigger joins
            // which leads to wrongful results (this object is reused)
            else if (info.qualifiedName == viewInfo.qualifiedName) {
              it.addAnnotation(blazeFetchMapping)
            }
          }
          .mutable(true)
          .build()
      )
    }

    return this
  }

  private val fetchStrategySelect = ClassName("com.blazebit.persistence.view", "FetchStrategy")
  private val blazeFetchMapping =
    AnnotationSpec.builder(ClassName("com.blazebit.persistence.view", "Mapping"))
      .useSiteTarget(AnnotationSpec.UseSiteTarget.GET)
      .addMember("fetch = %T.SELECT", fetchStrategySelect)
      .build()

  private fun TypeSpec.Builder.buildSubEntityView(
    info: EntityViewInfo,
    views: ViewInfo,
  ): TypeSpec.Builder {
    val entity = info.entity
    val superEntity = entitiesMap[info.entity.superClass!!]!!

    val superFields = superEntity.properties.mapTo(HashSet()) { it.simpleName }

    // Entity view annotation
    val entityViewAnnotation =
      AnnotationSpec.builder(ClassName("com.blazebit.persistence.view", "EntityView"))
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
      val propTypeQualifiedName = relation.type.declaration.qualifiedName?.asString()
      val isCollection = propTypeQualifiedName?.startsWith("kotlin.collections.") ?: false

      val propType =
        if (isCollection && propTypeQualifiedName != null) {
          propTypeQualifiedName.toPoetClassName().parameterizedBy(viewType)
        } else viewType

      addProperty(
        PropertySpec.builder(relation.simpleName, propType)
          .addModifiers(KModifier.ABSTRACT)
          .also {
            val viewInfo = views.entityViews.getValue(viewTypeStr)
            if (viewInfo.isExtended && isCollection) {
              it.addAnnotation(blazeFetchMapping)
            } else if (info.qualifiedName == viewInfo.qualifiedName) {
              it.addAnnotation(blazeFetchMapping)
            }
          }
          .mutable(true)
          .build()
      )
    }

    return this
  }

  private val blazeIdMapping =
    AnnotationSpec.builder(ClassName("com.blazebit.persistence.view", "IdMapping"))
      .useSiteTarget(AnnotationSpec.UseSiteTarget.GET)
      .build()

  private fun TypeSpec.Builder.buildProperty(property: Property) {
    val propType =
      if (property.isValueClass) {
        requireNotNull(property.innerValue).type.toClassName()
      } else if (property.isEmbedded) {
        val embedded = property.embedded!!
        val name = viewName(embedded)
        ClassName(packageName, name)
      } else property.type.toClassName()
    addProperty(
      PropertySpec.builder(property.simpleName, propType)
        .addModifiers(KModifier.ABSTRACT)
        .mutable(true)
        .also {
          if (property !is IdProperty) return@also
          it.addAnnotation(blazeIdMapping)
        }
        .build()
    )
  }

  private fun TypeSpec.Builder.buildEmbeddedView(view: EmbeddedViewInfo): TypeSpec.Builder {
    val info = view.embedded
    // Entity view annotation
    val entityViewAnnotation =
      AnnotationSpec.builder(ClassName("com.blazebit.persistence.view", "EntityView"))
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
