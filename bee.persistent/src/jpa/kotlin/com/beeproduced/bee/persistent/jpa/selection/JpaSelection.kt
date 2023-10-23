package com.beeproduced.bee.persistent.jpa.selection

import com.beeproduced.bee.persistent.jpa.meta.MetaModel
import com.beeproduced.bee.persistent.jpa.meta.Relations
import com.beeproduced.bee.persistent.selection.DataSelection
import com.beeproduced.bee.persistent.selection.FullNonRecursiveSelection
import jakarta.persistence.EntityGraph
import jakarta.persistence.EntityManager
import jakarta.persistence.Subgraph

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-01-30
 */
object JpaSelection {

    fun <T : Any> dataSelectionToEntityGraph(
        type: Class<T>,
        selection: DataSelection,
        em: EntityManager
    ): EntityGraph<T> {
        // FullSelection is used to traverse the entire JPA entity, this however can lead
        // to endless recursion, that is why one needs to check if a relation was already
        // traversed.
        // Other selection types (e.g., GraphQL) cannot be (endless) recursive, thus visited checks
        // are not needed.
        return if (selection is FullNonRecursiveSelection) fullSelectionToEntityGraph(type, selection, em)
        else finiteSelectionToEntityGraph(type, selection, em)
    }

    private fun <T : Any> finiteSelectionToEntityGraph(
        type: Class<T>,
        selection: DataSelection,
        em: EntityManager
    ): EntityGraph<T> {
        val graph = em.createEntityGraph(type)

        val relations = MetaModel.getMembersInfo(type).relations
        if (relations.map.isEmpty()) return graph

        // Iterate over metamodel
        // Note: Duplicate code because graph type != subgraph type (no common interface)
        @Suppress("DuplicatedCode")
        for ((name, relation) in relations.map) {
            if (selection.contains(name)) {
                val memberName = relation.field.name
                val relationType = relation.nonCollectionType
                val subGraph = graph.addSubgraph(memberName, relationType)
                val subSelection = selection.subSelect(name) ?: continue
                val subRelations = MetaModel.getMembersInfo(relationType).relations

                val skipFieldTarget = selection.skipOvers.skipOver(name, type)
                if (skipFieldTarget != null) {
                    val skipType = subRelations[skipFieldTarget]?.nonCollectionType ?: continue
                    val skipGraph = subGraph.addSubgraph(skipFieldTarget, skipType)
                    val skipRelations = MetaModel.getMembersInfo(skipType).relations
                    traverseSelection(skipType, skipGraph, subSelection, skipRelations)
                } else {
                    traverseSelection(relationType, subGraph, subSelection, subRelations)
                }
            }
        }

        return graph
    }

    private fun traverseSelection(
        type: Class<*>,
        subgraph: Subgraph<*>,
        selection: DataSelection,
        relations: Relations,
    ) {
        if (relations.map.isEmpty()) return
        for ((name, relation) in relations.map) {
            if (selection.contains(name)) {
                val memberName = relation.field.name
                val relationType = relation.nonCollectionType
                val subGraph = subgraph.addSubgraph(memberName, relationType)
                val subSelection = selection.subSelect(name) ?: continue
                val subRelations = MetaModel.getMembersInfo(relationType).relations

                val skipFieldTarget = selection.skipOvers.skipOver(name, type)
                if (skipFieldTarget != null) {
                    val skipType = subRelations[skipFieldTarget]?.nonCollectionType ?: continue
                    val skipGraph = subGraph.addSubgraph(skipFieldTarget, skipType)
                    val skipRelations = MetaModel.getMembersInfo(skipType).relations
                    traverseSelection(skipType, skipGraph, subSelection, skipRelations)
                } else {
                    traverseSelection(relationType, subGraph, subSelection, subRelations)
                }

            }
        }
    }

    private class Visited {
        private val visited = mutableMapOf<Class<*>, MutableSet<String>>()
        fun hasVisited(type: Class<*>, relationName: String): Boolean {
            val entry = visited[type] ?: return false
            return entry.contains(relationName)
        }

        fun addVisited(type: Class<*>, relationName: String) {
            if (visited.containsKey(type)) {
                visited.getValue(type).add(relationName)
            } else {
                visited[type] = mutableSetOf(relationName)
            }
        }
    }

    private fun <T : Any> fullSelectionToEntityGraph(
        type: Class<T>,
        selection: FullNonRecursiveSelection,
        em: EntityManager
    ): EntityGraph<T> {
        val graph = em.createEntityGraph(type)

        val relations = MetaModel.getMembersInfo(type).relations
        if (relations.map.isEmpty()) return graph

        // Iterate over metamodel
        for ((name, relation) in relations.map) {
            if (selection.contains(name)) {
                val memberName = relation.field.name
                val relationType = relation.nonCollectionType
                val subGraph = graph.addSubgraph(memberName, relationType)
                val subSelection = selection.subSelect(name) ?: continue
                val subRelations = MetaModel.getMembersInfo(relationType).relations
                traverseFullSelection(relationType, subGraph, subSelection, subRelations, Visited())
            }
        }

        return graph
    }


    private fun traverseFullSelection(
        type: Class<*>,
        subgraph: Subgraph<*>,
        selection: DataSelection,
        relations: Relations,
        visited: Visited
    ) {
        if (relations.map.isEmpty()) return
        for ((name, relation) in relations.map) {
            // If relation has been already visited (at maybe other depth)
            // add it to subgraph but not continue traversal.
            // This makes it possible to select related entity types on at least the first level:
            // Root
            //  -   branchA: Branch
            //      -   branchA: Branch
            //      -   branchB: Branch
            //  -   branchB: Branch
            //      -   branchA: Branch
            //      -   branchB: Branch
            if (selection.contains(name) && visited.hasVisited(type, name)) {
                val memberName = relation.field.name
                val relationType = relation.nonCollectionType
                val subGraph = subgraph.addSubgraph(memberName, relationType)
            } else if (selection.contains(name) && !visited.hasVisited(type, name)) {
                visited.addVisited(type, name)
                val memberName = relation.field.name
                val relationType = relation.nonCollectionType
                val subGraph = subgraph.addSubgraph(memberName, relationType)
                val subSelection = selection.subSelect(name) ?: continue
                val subRelations = MetaModel.getMembersInfo(relationType).relations
                traverseFullSelection(relationType, subGraph, subSelection, subRelations, visited)
            }
        }
    }
}
