package com.beeproduced.bee.persistent.extensions.graphql.schema

import com.beeproduced.bee.persistent.selection.DataSelection
import com.beeproduced.bee.persistent.selection.SimpleSelection
import graphql.schema.DataFetchingEnvironment
import graphql.schema.DataFetchingFieldSelectionSet

/**
 * @author Kacper Urbaniec
 * @version 2023-01-30
 */
fun DataFetchingFieldSelectionSet.toDataSelection(): DataSelection {
  val fields =
    immediateFields.mapTo(HashSet()) {
      com.beeproduced.bee.persistent.dgs.selection.toDataSelection(it)
    }

  return SimpleSelection(fields)
}

fun DataFetchingEnvironment.toDataSelection(): DataSelection {
  return selectionSet.toDataSelection()
}
