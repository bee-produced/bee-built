package com.beeproduced.bee.persistent.blaze.repository

import com.beeproduced.bee.persistent.blaze.meta.SelectionInfo
import com.beeproduced.bee.persistent.blaze.selection.BeeSelection
import com.beeproduced.bee.persistent.blaze.selection.DefaultBeeSelection
import com.blazebit.persistence.view.EntityViewSetting

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-12-31
 */

fun EntityViewSetting<*, *>.fetchSelection(info: SelectionInfo, selection: BeeSelection) {
    fetchSelection("", info, selection)
}

private fun EntityViewSetting<*, *>.fetchSelection(path: String, info: SelectionInfo, selection: BeeSelection) {
    if (info.id != null)
        fetch<Any>("$path${info.id}")
    for (column in info.columns)
        fetch<Any>("$path$column")
    for (lazyColumn in info.lazyColumns)
        if (selection.contains(lazyColumn))
            fetch<Any>("$path$lazyColumn")
    fetchSubselection(path, info.embedded, selection)
    fetchLazySubselection(path, info.lazyEmbedded, selection)
    fetchLazySubselection(path, info.relations, selection)
}

private fun EntityViewSetting<*, *>.fetchSubselection(path: String, subInfo: Map<String, SelectionInfo>, selection: BeeSelection) {
    for ((property, info) in subInfo) {
        val subSelection = selection.subSelect(property)
            ?: DefaultBeeSelection(emptySet())
        fetchSelection("$path$property.", info, subSelection)
    }
}

private fun EntityViewSetting<*, *>.fetchLazySubselection(path: String, subInfo: Map<String, SelectionInfo>, selection: BeeSelection) {
    for ((property, info) in subInfo) {
        val subSelection = selection.subSelect(property)
            ?: continue
        fetchSelection("$path$property.", info, subSelection)
    }
}