package com.beeproduced.bee.persistent.blaze.processor.utils

import com.beeproduced.bee.persistent.blaze.processor.codegen.EntityViewInfo
import com.beeproduced.bee.persistent.blaze.processor.codegen.ViewInfo
import com.beeproduced.bee.persistent.blaze.processor.info.ColumnProperty
import com.beeproduced.bee.persistent.blaze.processor.info.Property

/**
 * @author Kacper Urbaniec
 * @version 2024-01-12
 */
typealias AllRelations = MutableMap<String, String>

typealias AllColumns = MutableList<ColumnProperty>

typealias AllLazyColumns = MutableList<ColumnProperty>

fun viewLazyColumnsWithSubclasses(
  view: EntityViewInfo,
  views: ViewInfo,
): Triple<AllRelations, AllColumns, AllLazyColumns> {
  val entity = view.entity
  val allRelations: MutableMap<String, String> = view.relations.toMutableMap()
  val allColumns: MutableList<ColumnProperty> = entity.columns.toMutableList()
  val allLazyColumns: MutableList<ColumnProperty> = entity.lazyColumns.toMutableList()

  val subViews = views.subclassEntityViewsBySuperClass[view.name]
  subViews?.forEach { subView ->
    for ((relationViewName, relation) in subView.relations) if (
      !allRelations.containsKey(relationViewName)
    )
      allRelations[relationViewName] = relation

    val columnKeys = allColumns.mapTo(HashSet()) { it.simpleName }
    for (column in subView.entity.columns) if (!columnKeys.contains(column.simpleName))
      allColumns.add(column)

    val lazyColumnKeys = allLazyColumns.mapTo(HashSet()) { it.simpleName }
    for (column in subView.entity.lazyColumns) if (!lazyColumnKeys.contains(column.simpleName))
      allLazyColumns.add(column)
  }

  return Triple(allRelations, allColumns, allLazyColumns)
}

typealias AllSubRelation = MutableMap<String, SubRelation>

typealias AllSubColumnsWithId = MutableList<SubProperty>

data class SubRelation(val relationView: String, val subView: String? = null)

data class SubProperty(val property: Property, val subView: String? = null)

fun viewColumnsWithSubclasses(
  view: EntityViewInfo,
  views: ViewInfo,
): Pair<AllSubRelation, AllSubColumnsWithId> {
  val entity = view.entity
  val allRelations: MutableMap<String, SubRelation> =
    view.relations.mapValuesTo(HashMap()) { (_, v) -> SubRelation(v) }
  val allColumns: MutableList<SubProperty> = entity.columns.mapTo(ArrayList()) { SubProperty(it) }
  allColumns.addAll(entity.lazyColumns.map { SubProperty(it) })
  allColumns.add(SubProperty(entity.id))

  val subViews = views.subclassEntityViewsBySuperClass[view.name]
  subViews?.forEach { subView ->
    for ((relationViewName, relation) in subView.relations) if (
      !allRelations.containsKey(relationViewName)
    )
      allRelations[relationViewName] = SubRelation(relation, subView.entity.simpleName)

    val columnKeys = allColumns.mapTo(HashSet()) { it.property.simpleName }
    val subViewAllColumns = subView.entity.columns + subView.entity.lazyColumns
    for (column in subViewAllColumns) if (!columnKeys.contains(column.simpleName))
      allColumns.add(SubProperty(column, subView.entity.simpleName))
  }

  return Pair(allRelations, allColumns)
}
