package com.beeproduced.bee.persistent.blaze.processor.utils

import com.beeproduced.bee.persistent.blaze.processor.codegen.EntityViewInfo
import com.beeproduced.bee.persistent.blaze.processor.codegen.ViewInfo
import com.beeproduced.bee.persistent.blaze.processor.info.RepoInfo

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-13
 */

fun ViewInfo.findViewFromRepo(repo: RepoInfo) : EntityViewInfo {
    val qualifiedName = repo.entityType.declaration.qualifiedName?.asString()
    return coreEntityViewsByQualifiedName[qualifiedName]
        ?: throw IllegalArgumentException("No view for [$qualifiedName] found")
}