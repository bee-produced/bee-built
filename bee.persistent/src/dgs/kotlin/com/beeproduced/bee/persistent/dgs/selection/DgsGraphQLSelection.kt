package com.beeproduced.bee.persistent.dgs.selection

import com.beeproduced.bee.persistent.selection.DataSelection
import com.beeproduced.bee.persistent.selection.FieldNodeDefinition
import com.beeproduced.bee.persistent.selection.SimpleSelection
import com.beeproduced.bee.persistent.selection.SimpleSelection.Companion.substringBeforeOrNull
import graphql.schema.DataFetchingEnvironment
import graphql.schema.DataFetchingFieldSelectionSet
import graphql.schema.SelectedField

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-01-30
 */

fun DataFetchingFieldSelectionSet.toDataSelection(): DataSelection {
    val fields = immediateFields
        .mapTo(HashSet()) { toDataSelection(it) }

    return SimpleSelection(fields)
}

fun DataFetchingEnvironment.toDataSelection(): DataSelection {
    return selectionSet.toDataSelection()
}

private fun toDataSelection(node: SelectedField): FieldNodeDefinition {

    val field = node.qualifiedName
    val fieldType = node.fullyQualifiedName.substringBeforeOrNull(".")
    val immediateFields = node.selectionSet.immediateFields

    val nodeFields = if (immediateFields.isNullOrEmpty()) null
    else {
        immediateFields.mapTo(HashSet()) { toDataSelection(it) }
    }

    return SimpleSelection.TypedFiledNode(field, fieldType, nodeFields)

}