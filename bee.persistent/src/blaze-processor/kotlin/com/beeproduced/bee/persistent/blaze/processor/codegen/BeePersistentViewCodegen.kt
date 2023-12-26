package com.beeproduced.bee.persistent.blaze.processor.codegen

import com.beeproduced.bee.generative.util.PoetMap
import com.beeproduced.bee.generative.util.addNStatementBuilder
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
    private val packageName: String = "com.beeproduced.bee.persistent.generated"
    private val fileName: String = "GeneratedViews"

    private val entitiesMap = entities.associateBy { it.qualifiedName!! }

    private val poetMap: PoetMap = mutableMapOf()
    private fun FunSpec.Builder.addNStatement(format: String)
        = addNStatementBuilder(format, poetMap)
    @Suppress("ConstPropertyName")
    object PoetConstants {

    }

    fun processEntities() {
        FileSpec
            .builder(packageName, fileName)
            .also {
                it.addAnnotation(
                    AnnotationSpec.builder(ClassName("", "Suppress"))
                        .addMember("%S, %S", "ClassName", "RedundantVisibilityModifier")
                        .build()
                )
                entities.forEach { entity ->
                    it.processEntity(entity)
                }
                for ((info, props) in debugInfo) {
                    logger.info(info)
                    for (p in props) {
                        logger.info("  $p")
                    }
                }
                generatedEmbedded.forEach { (name, info) ->
                   it.addType(TypeSpec.classBuilder(name)
                       .buildEmbeddedView(info)
                       .build()
                    )
                }
            }
            .build()
            .writeTo(codeGenerator, dependencies)
    }


    private val debugInfo = mutableMapOf<String, MutableSet<String>>()

    // private fun FileSpec.Builder.processEntity(
    //     entity: EntityInfo, viewCount: ViewCount
    // ): String {
    //     val viewName = viewCount.viewName(entity, null)
    //
    //     if (!debugInfo.containsKey(viewName)) {
    //         debugInfo[viewName] = mutableSetOf()
    //     }
    //
    //     //logger.info("View $viewName")
    //     for (relation in entity.relations) {
    //
    //         // logger.info(relation.toString())
    //         // logger.info("${relation.simpleName} X ${relation.qualifiedName}")
    //
    //         val relationEntity = entitiesMap[relation.qualifiedName!!] ?: continue
    //         val relationView = processEntity(relationEntity, viewCount, entity)
    //         //logger.info("  View $viewName | ${relation.simpleName} => $relationView")
    //
    //         if (relationView != null)
    //             debugInfo[viewName]?.add(relationView)
    //     }
    //
    //     return viewName
    // }

    private fun FileSpec.Builder.processEntity(entity: EntityInfo) {
        // val viewName = viewCount.viewName(entity, null)
        //
        // if (!debugInfo.containsKey(viewName)) {
        //     debugInfo[viewName] = mutableSetOf()
        // }
        //
        // val relationEntities = mutableListOf<ProcessE>()
        // for (relation in entity.relations) {
        //     val relationEntity = entitiesMap[relation.qualifiedName!!] ?: continue
        //     relationEntities.add(ProcessE(relationEntity, entity, viewName))
        // }
        // processEntities(relationEntities, viewCount, entity)

        logger.info("Generating!")
        val result = processEntities(entity, config.depth)
        result.forEach { (key, value) ->
            logger.info(key)
            value.relations.forEach { r ->
                logger.info("  $r")
            }
        }
    }

    data class EntityViewInfo(
        val name: String,
        val entity: EntityInfo,
        val superClassName: String? = null,
        val relations: MutableMap<String, String> = mutableMapOf()
    ) {
        val entityRelations get() = entity.relations
        val qualifiedName get() = entity.qualifiedName!!
    }

    private fun viewName(entity: EntityInfo, root: EntityInfo, count: Int? = null): String {
        return "${entity.simpleName}__View__${root.simpleName}__${count ?: "Core"}"
    }

    private fun FileSpec.Builder.processEntities(
        root: EntityInfo, depth: Int
    ): Map<String, EntityViewInfo> {
        // Key -> ViewName
        val result = mutableMapOf<String, EntityViewInfo>()
        // Key -> QualifiedName
        val count = mutableMapOf<String, Int>()

        val extend = mutableListOf<EntityViewInfo>()

        // Visited -> QualifiedName
        fun dfs(start: EntityInfo, startViewName: String, visited: MutableSet<String>, isCore: Boolean = false) {
            val info = EntityViewInfo(startViewName, start)
            for (relation in info.entityRelations) {
                val relationEntity = entitiesMap[relation.qualifiedName!!] ?: continue
                // Copy visited already here to multiple relations to same entity via same view
                val nextVisited = visited.toMutableSet()

                if (!nextVisited.contains(relationEntity.qualifiedName)) {
                    nextVisited.add(relationEntity.qualifiedName!!)
                    val c = count.getOrDefault(relation.qualifiedName!!, 1)
                    count[relation.qualifiedName!!] = c + 1
                    val viewName = viewName(relationEntity, root, c)
                    info.relations[relation.simpleName] = viewName
                    dfs(relationEntity, viewName, nextVisited)
                } else if (depth > 0) {
                    val c = count.getOrDefault(relation.qualifiedName!!, 1)
                    val viewName = viewName(relationEntity, root, c)
                    count[relation.qualifiedName!!] = c + 1
                    info.relations[relation.simpleName] = viewName
                    extend.add(EntityViewInfo(viewName, relationEntity))
                }
            }

            result[startViewName] = info
            if (start.subClasses == null) return

            // TODO: Streamline this...
            for (subClass in start.subClasses) {
                val subEntity = entitiesMap[subClass] ?: continue
                val c = count.getOrDefault(subEntity.qualifiedName!!, 1)
                val viewName = if (isCore) viewName(subEntity, root)
                else viewName(subEntity, root, c)
                count[subEntity.qualifiedName!!] = c + 1
                val subInfo = EntityViewInfo(viewName, subEntity, startViewName)

                for (relation in subInfo.entityRelations) {
                    val cachedRelation = info.relations[relation.simpleName]
                    if (cachedRelation != null) {
                        subInfo.relations[relation.simpleName] = cachedRelation
                        continue
                    }

                    val relationEntity = entitiesMap[relation.qualifiedName!!] ?: continue
                    // Copy visited already here to multiple relations to same entity via same view
                    val nextVisited = visited.toMutableSet()

                    if (!nextVisited.contains(relationEntity.qualifiedName)) {
                        nextVisited.add(relationEntity.qualifiedName!!)
                        val c = count.getOrDefault(relation.qualifiedName!!, 1)
                        count[relation.qualifiedName!!] = c + 1
                        val viewName = viewName(relationEntity, root, c)
                        subInfo.relations[relation.simpleName] = viewName
                        dfs(relationEntity, viewName, nextVisited)
                    } else if (depth > 0) {
                        val c = count.getOrDefault(relation.qualifiedName!!, 1)
                        val viewName = viewName(relationEntity, root, c)
                        count[relation.qualifiedName!!] = c + 1
                        subInfo.relations[relation.simpleName] = viewName
                        extend.add(EntityViewInfo(viewName, relationEntity))
                    }
                    result[viewName] = subInfo
                }
            }

            // TODO: What when extending superClass
            // if (start.superClass == null) return
        }

        if (root.superClass != null)
            return mutableMapOf()
        dfs(root, viewName(root, root), mutableSetOf(root.qualifiedName!!), true)

        if (extend.isNotEmpty()) {
            for (i in 1..depth) {
                logger.info("Extend")
                logger.info(extend.map { it.name }.toString())
                val extendRound = extend.toList()
                extend.clear()
                for (info in extendRound) {
                    if (i < depth) {
                        for (relation in info.entityRelations) {
                            val relationEntity = entitiesMap[relation.qualifiedName!!] ?: continue
                            val c = count.getOrDefault(relation.qualifiedName!!, 1)
                            val viewName = viewName(relationEntity, root, c)
                            count[relation.qualifiedName!!] = c + 1
                            info.relations[relation.simpleName] = viewName
                            extend.add(EntityViewInfo(viewName, relationEntity))
                        }
                    }
                    result[info.name] = info
                }
            }
        }

        for (entityView in result.values) {
            addType(TypeSpec.classBuilder(entityView.name)
                .let {
                    if (entityView.superClassName != null) it.buildSubEntityView(entityView)
                    else it.buildEntityView(entityView)
                }
                .build()
            )
        }

        return result
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
            val name = "${embedded.declaration.simpleName.asString()}__View"
            generatedEmbedded[name] = embedded
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

    private val generatedEmbedded: MutableMap<String, EmbeddedInfo> = mutableMapOf()

    private fun TypeSpec.Builder.buildEmbeddedView(info: EmbeddedInfo): TypeSpec.Builder {
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