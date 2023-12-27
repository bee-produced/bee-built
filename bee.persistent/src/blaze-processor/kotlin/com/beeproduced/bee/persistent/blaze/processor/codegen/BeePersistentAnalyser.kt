package com.beeproduced.bee.persistent.blaze.processor.codegen

import com.beeproduced.bee.persistent.blaze.processor.info.EmbeddedInfo
import com.beeproduced.bee.persistent.blaze.processor.info.EntityInfo
import com.google.devtools.ksp.processing.KSPLogger

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-12-27
 */

data class EntityViewInfo(
    val name: String,
    val entity: EntityInfo,
    val superClassName: String? = null,
    val relations: MutableMap<String, String> = mutableMapOf()
) {
    val entityRelations get() = entity.relations
    val qualifiedName get() = entity.qualifiedName!!
}

data class EmbeddedViewInfo(
    val name: String,
    val embedded: EmbeddedInfo
)

data class ViewInfo(
    val entityViews: Map<String, EntityViewInfo>,
    val embeddedViews: Map<String, EmbeddedViewInfo>
) {
    val entityViewCoreNames get() = entityViews.keys.filter { it.endsWith("Core") }
}

class BeePersistentAnalyser(
    private val logger: KSPLogger,
    config: BeePersistentBlazeConfig
) {
    private val depth = config.depth
    private lateinit var entitiesMap: Map<String, EntityInfo>

    fun processEntities(entities: List<EntityInfo>): ViewInfo {
        entitiesMap = entities.associateBy { it.qualifiedName!! }
        return entities
            .map { processEntity(it) }
            .reduce { acc, next ->
                ViewInfo(
                    acc.entityViews + next.entityViews,
                    acc.embeddedViews + next.embeddedViews
                )
            }
    }

    private fun processEntity(root: EntityInfo): ViewInfo {
        // Key -> ViewName
        val result = mutableMapOf<String, EntityViewInfo>()
        val embeddedViews = mutableMapOf<String, EmbeddedViewInfo>()
        // Key -> QualifiedName
        val count = mutableMapOf<String, Int>()

        val extend = mutableListOf<EntityViewInfo>()

        // Visited -> QualifiedName
        fun dfs(start: EntityInfo, startViewName: String, visited: MutableSet<String>, isCore: Boolean = false) {
            val info = EntityViewInfo(startViewName, start)

            for (property in start.columns + start.lazyColumns + start.id) {
                if (!property.isEmbedded) continue
                val embeddedInfo = requireNotNull(property.embedded)
                val embeddedViewName = viewName(embeddedInfo)
                val embeddedViewInfo = EmbeddedViewInfo(embeddedViewName, embeddedInfo)
                embeddedViews[embeddedViewName] = embeddedViewInfo
            }

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

                for (property in subEntity.columns + subEntity.lazyColumns + subEntity.id) {
                    if (!property.isEmbedded) continue
                    val embeddedInfo = requireNotNull(property.embedded)
                    val embeddedViewName = viewName(embeddedInfo)
                    val embeddedViewInfo = EmbeddedViewInfo(embeddedViewName, embeddedInfo)
                    embeddedViews[embeddedViewName] = embeddedViewInfo
                }

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
            return ViewInfo(emptyMap(), emptyMap())
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

        return ViewInfo(result, embeddedViews)
    }


    companion object {
        fun viewName(entity: EntityInfo, root: EntityInfo, count: Int? = null): String {
            return "${entity.simpleName}__View__${root.simpleName}__${count ?: "Core"}"
        }

        fun viewName(embedded: EmbeddedInfo): String {
            return "${embedded.declaration.simpleName.asString()}__View"
        }
    }


}