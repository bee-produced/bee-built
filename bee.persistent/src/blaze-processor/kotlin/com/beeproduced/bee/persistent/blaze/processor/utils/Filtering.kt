package com.beeproduced.bee.persistent.blaze.processor.utils

import com.beeproduced.bee.persistent.blaze.processor.codegen.EntityViewInfo
import com.beeproduced.bee.persistent.blaze.processor.codegen.ViewInfo
import com.beeproduced.bee.persistent.blaze.processor.info.ColumnProperty
import com.beeproduced.bee.persistent.blaze.processor.info.Property

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-12
 */

typealias AllRelations = MutableMap<String, String>
typealias AllColumns = MutableList<ColumnProperty>
typealias AllLazyColumns = MutableList<ColumnProperty>
typealias AllColumnsWithId = MutableList<Property>

fun viewLazyColumnsWithSubclasses(
    view: EntityViewInfo, views: ViewInfo
): Triple<AllRelations, AllColumns, AllLazyColumns> {
    val entity = view.entity
    val allRelations: MutableMap<String, String> = view.relations.toMutableMap()
    val allColumns: MutableList<ColumnProperty> = entity.columns.toMutableList()
    val allLazyColumns: MutableList<ColumnProperty> = entity.lazyColumns.toMutableList()

    val subViews = views.subclassEntityViewsBySuperClass[view.name]
    subViews?.forEach { subView ->

        for ((relationViewName, relation) in subView.relations)
            if (!allRelations.containsKey(relationViewName))
                allRelations[relationViewName] = relation

        val columnKeys = allColumns.mapTo(HashSet()) { it.simpleName }
        for (column in subView.entity.columns)
            if (!columnKeys.contains(column.simpleName))
                allColumns.add(column)

        val lazyColumnKeys = allLazyColumns.mapTo(HashSet()) { it.simpleName }
        for (column in subView.entity.lazyColumns)
            if (!lazyColumnKeys.contains(column.simpleName))
                allLazyColumns.add(column)

    }

    return Triple(allRelations, allColumns, allLazyColumns)
}

fun viewColumnsWithSubclasses(
    view: EntityViewInfo, views: ViewInfo
): Pair<AllRelations, AllColumnsWithId> {
    val entity = view.entity
    val allRelations: MutableMap<String, String> = view.relations.toMutableMap()
    val allColumns: MutableList<Property> = entity.columns.toMutableList()
    allColumns.addAll(entity.lazyColumns)
    allColumns.add(entity.id)

    val subViews = views.subclassEntityViewsBySuperClass[view.name]
    subViews?.forEach { subView ->

        for ((relationViewName, relation) in subView.relations)
            if (!allRelations.containsKey(relationViewName))
                allRelations[relationViewName] = relation

        val columnKeys = allColumns.mapTo(HashSet()) { it.simpleName }
        val subViewAllColumns = subView.entity.columns + subView.entity.lazyColumns
        for (column in subViewAllColumns)
            if (!columnKeys.contains(column.simpleName))
                allColumns.add(column)
    }

    return Pair(allRelations, allColumns)
}