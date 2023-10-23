package com.beeproduced.data.selection

import java.io.File
import java.nio.file.FileSystems
import java.nio.file.PathMatcher
import java.nio.file.Paths

/**
 * Inspired by https://github.com/graphql-java/graphql-java/blob/master/src/main/java/graphql/schema/DataFetchingFieldSelectionSetImpl.java
 *
 * @author Kacper Urbaniec
 * @version 2023-02-02
 */
class SimpleSelection internal constructor(
    private val immediateFields: MutableList<FieldNodeDefinition>,
    private val fields: MutableMap<String, FieldNodeDefinition>,
    private val fieldGlobPaths: MutableSet<String>,
    override val skipOvers: SkipOverCollection
) : DataSelection {

    companion object {
        const val SEP = "/"
        val UNIX_TARGET = SEP == File.separator

        private fun osField(field: String): String {
            return if (UNIX_TARGET) field
            else field.replace(SEP, "\\")
        }

        private fun removeLeadingSlash(fieldGlobPattern: String): String {
            return if (fieldGlobPattern.startsWith(SEP)) fieldGlobPattern.substring(1)
            else fieldGlobPattern
        }

        private fun globMatcher(fieldGlobPattern: String): PathMatcher {
            val osFieldGlobPattern = removeLeadingSlash(fieldGlobPattern)
            return FileSystems.getDefault().getPathMatcher("glob:$osFieldGlobPattern")
        }
    }

    constructor(fields: Set<FieldNodeDefinition>, skips: Collection<SkipOver> = emptyList()) : this(
        mutableListOf(),
        mutableMapOf(),
        mutableSetOf(),
        SkipOverCollection(skips)
    ) {
        setupFields(fields, immediateFields, this.fields, fieldGlobPaths)
    }

    private fun setupFields(
        start: Set<FieldNodeDefinition>,
        immediateFields: MutableList<FieldNodeDefinition>,
        fields: MutableMap<String, FieldNodeDefinition>,
        fieldGlobPaths: MutableSet<String>
    ) {
        for (node in start) {
            immediateFields.add(node)
            val path = node.field
            fields[path] = node
            fieldGlobPaths.add(path)

            node.fields?.let { nodeFields ->
                nodeFields.forEach { child ->
                    setupFields(child, path, fields, fieldGlobPaths)
                }
            }
        }
    }

    private fun setupFields(
        node: FieldNodeDefinition,
        parentPath: String,
        fields: MutableMap<String, FieldNodeDefinition>,
        fieldGlobPaths: MutableSet<String>
    ) {
        val path = "$parentPath$SEP${node.field}"
        fields[path] = node
        fieldGlobPaths.add(path)

        node.fields?.let { nodeFields ->
            nodeFields.forEach { child -> setupFields(child, path, fields, fieldGlobPaths) }
        }
    }

    data class FieldNode(
        override val field: String,
        override val fields: Set<FieldNodeDefinition>? = null
    ) : FieldNodeDefinition

    private data class MutableFieldNode(
        override val field: String,
        override var fields: MutableSet<MutableFieldNode>? = null
    ) : FieldNodeDefinition

    override fun contains(fieldGlobPattern: String): Boolean {
        if (fieldGlobPattern.isEmpty()) return false

        val globMatcher = globMatcher(fieldGlobPattern)
        for (field in fieldGlobPaths) {
            val path = Paths.get(osField(field))
            if (globMatcher.matches(path)) return true
        }
        return false
    }

    override fun subSelect(fieldGlobPattern: String): DataSelection? {
        if (fieldGlobPattern.isEmpty()) return null

        val start = mutableSetOf<FieldNodeDefinition>()
        val globMatcher = globMatcher(fieldGlobPattern)
        for ((field, node) in fields) {
            val nodeFields = node.fields
            val path = Paths.get(osField(field))
            if (globMatcher.matches(path) && !nodeFields.isNullOrEmpty()) {
                start.addAll(nodeFields)
            }
        }

        return if (start.isEmpty()) null
        else SimpleSelection(start, skipOvers.remainingSkips())
    }

    override fun merge(vararg selections: SimpleSelection): DataSelection {
        return merge(selections.asList())
    }

    override fun merge(selections: Collection<SimpleSelection>): DataSelection {
        if (selections.isEmpty()) return this

        val immediateFieldsTmp: MutableMap<String, FieldNodeDefinition> = mutableMapOf()
        val fieldsTmp: MutableMap<String, FieldNodeDefinition> = mutableMapOf()
        val fieldGlobPathsTmp: MutableSet<String> = mutableSetOf()

        val allSelections = selections
            .toMutableList()
            .also { it.add(this) }

        val immediateFields = allSelections
            .map { it.immediateFields }
            .flatten()

        for (node in immediateFields) {
            val mutableNode = if (!immediateFieldsTmp.contains(node.field)) {
                val mutableNode = MutableFieldNode(node.field)
                immediateFieldsTmp[node.field] = mutableNode
                fieldsTmp[node.field] = mutableNode
                fieldGlobPathsTmp.add(mutableNode.field)
                mutableNode
            } else immediateFieldsTmp.getValue(node.field) as MutableFieldNode

            node.fields?.let { nodeFields ->
                nodeFields.forEach { child ->
                    merge(child, mutableNode, node.field, fieldsTmp, fieldGlobPathsTmp)
                }
            }
        }

        val skipOverEntries = SkipOverCollection(
            allSelections
                .map { it.skipOvers.remainingSkips() }
                .flatten()
        )

        return SimpleSelection(
            immediateFields = immediateFieldsTmp.values.toMutableList(),
            fields = fieldsTmp,
            fieldGlobPaths = fieldGlobPathsTmp,
            skipOverEntries
        )
    }

    private fun merge(
        node: FieldNodeDefinition,
        parent: MutableFieldNode,
        parentPath: String,
        fieldsTmp: MutableMap<String, FieldNodeDefinition>,
        fieldGlobPathsTmp: MutableSet<String>
    ) {
        val path = "$parentPath$SEP${node.field}"
        val mutableFieldNode = if (fieldsTmp.contains(path)) {
            fieldsTmp.getValue(path)
        } else {
            val currentNode = MutableFieldNode(node.field)
            fieldsTmp[path] = currentNode
            currentNode
        } as MutableFieldNode

        parent.fields
            ?.add(mutableFieldNode)
            ?: run { parent.fields = mutableSetOf(mutableFieldNode) }

        fieldGlobPathsTmp.add(path)

        node.fields?.let { nodeFields ->
            nodeFields.forEach { child ->
                merge(child, mutableFieldNode, path, fieldsTmp, fieldGlobPathsTmp)
            }
        }
    }
}