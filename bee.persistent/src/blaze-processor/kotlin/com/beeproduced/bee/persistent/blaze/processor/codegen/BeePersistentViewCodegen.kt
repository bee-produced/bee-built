package com.beeproduced.bee.persistent.blaze.processor.codegen

import com.beeproduced.bee.generative.util.PoetMap
import com.beeproduced.bee.generative.util.addNStatementBuilder
import com.beeproduced.bee.persistent.blaze.processor.info.EntityInfo
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
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

    private fun viewCount(): MutableMap<String, Int> = entities
        .map { it.qualifiedName!! }
        .associateWithTo(HashMap()) { 0 }

    private fun MutableMap<String, Int>.viewCountReached(entity: EntityInfo): Boolean {
        val count = this[entity.qualifiedName!!] ?: return true
        return count >= config.depth
    }

    private fun MutableMap<String, Int>.incrementViewCount(entity: EntityInfo) {
        val count = this[entity.qualifiedName!!] ?: config.depth
        this[entity.qualifiedName!!] = count + 1
    }

    private fun MutableMap<String, Int>.viewName(entity: EntityInfo, parent: EntityInfo?): String {
        val count = this[entity.qualifiedName!!] ?: config.depth
        return "${entity.simpleName}__View__${parent?.simpleName ?: "Root"}__$count"
    }

    fun processEntities() {
        FileSpec
            .builder(packageName, fileName)
            .also {
                entities.forEach { entity ->
                    it.processEntity(entity, viewCount())
                }
                for ((info, props) in debugInfo) {
                    logger.info(info)
                    for (p in props) {
                        logger.info("  $p")
                    }
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

    private fun FileSpec.Builder.processEntity(
        entity: EntityInfo, viewCount: ViewCount
    ) {
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
        val result = proc(entity, config.depth)
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
        val relations: MutableMap<String, String> = mutableMapOf()
    ) {
        val entityRelations get() = entity.relations
        val qualifiedName get() = entity.qualifiedName!!
    }

    fun viewName(entity: EntityInfo, root: EntityInfo, count: Int? = null): String {
        return "${entity.simpleName}__View__${root.simpleName}__${count ?: "Core"}"
    }

    private fun FileSpec.Builder.proc(
        root: EntityInfo, depth: Int
    ): Map<String, EntityViewInfo> {
        // Key -> ViewName
        val result = mutableMapOf<String, EntityViewInfo>()
        // Key -> QualifiedName
        val count = mutableMapOf<String, Int>()

        val extend = mutableListOf<EntityViewInfo>()

        // Visited -> QualifiedName
        fun dfs(start: EntityInfo, startViewName: String, visited: MutableSet<String>) {
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
        }

        dfs(root, viewName(root, root), mutableSetOf(root.qualifiedName!!))

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

        return result
    }



    data class ProcessE(
        val entity: EntityInfo,
        val parent: EntityInfo,
        val parentViewName: String
    )

    // TODO: Rewrite this more performant and better readable

    private fun FileSpec.Builder.processEntities(
        entities: List<ProcessE>, viewCount: ViewCount, root: EntityInfo
    ) {
        val entities = entities.filterNot { (entity, parent) ->
            viewCount.viewCountReached(entity)
        }
        if (entities.isEmpty()) return


        val uniqueEntities = entities.map { it.entity }.toSet()
        for (entity in uniqueEntities) {
            viewCount.incrementViewCount(entity)
        }

        for ((entity, parent, name) in entities) {
            if (!debugInfo.containsKey(name)) {
                debugInfo[name] = mutableSetOf()
            }
            val entityViewName = viewCount.viewName(entity, root)
            debugInfo[name]?.add(entityViewName)
        }

        val newEntities = entities.map { (entity, parent) ->
            val viewName = viewCount.viewName(entity, root)
            val relationEntities = mutableListOf<ProcessE>()
            for (relation in entity.relations) {
                val relationEntity = entitiesMap[relation.qualifiedName!!] ?: continue
                relationEntities.add(ProcessE(relationEntity, entity, viewName))
            }
            relationEntities
        }.flatten()

        // TODO: Clear duplicates...

        processEntities(newEntities, viewCount, root)
    }

    private fun FileSpec.Builder.processEntity(
        entity: EntityInfo, viewCount: ViewCount, root: EntityInfo
    ): String? {
        if (viewCount.viewCountReached(entity)) return null
        viewCount.incrementViewCount(entity)
        val viewName = viewCount.viewName(entity, root)

        if (!debugInfo.containsKey(viewName)) {
            debugInfo[viewName] = mutableSetOf()
        }

        //logger.info("View $viewName")
        for (relation in entity.relations) {

            // logger.info(relation.toString())
            // logger.info("${relation.simpleName} X ${relation.qualifiedName}")

            val relationEntity = entitiesMap[relation.qualifiedName!!] ?: continue
            val relationView = processEntity(relationEntity, viewCount.toMutableMap(), root)
            //logger.info("  View $viewName | ${relation.simpleName} => $relationView")

            if (relationView != null)
                debugInfo[viewName]?.add(relationView)
        }

        return viewName
    }

    private fun TypeSpec.Builder.buildEntityView(entity: EntityInfo): TypeSpec.Builder {


        return this
    }

}

typealias ViewCount = MutableMap<String, Int>