package com.beeproduced.bee.persistent.blaze.processor.codegen

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-12-14
 */
object BeePersistentBlazeOptions {
    const val packageName = "persistentPackageName"
    const val depth = "persistentDepth"
    const val subPackageRepository = "persistentSubPackageRepository"
    const val subPackageView = "persistentSubPackageView"
    const val subPackageDSL = "persistentSubPackageDSL"
    const val subPackageBuilder = "persistentSubPackageBuilder"
}

data class BeePersistentBlazeConfig(
    val packageName: String,
    val depth: Int,
    val viewPackageName: String,
    val repositoryPackageName: String,
    val dslPackageName: String,
    val builderPackageName: String
)