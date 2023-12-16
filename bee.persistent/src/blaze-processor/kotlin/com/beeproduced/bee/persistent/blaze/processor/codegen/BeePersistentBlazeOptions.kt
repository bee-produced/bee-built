package com.beeproduced.bee.persistent.blaze.processor.codegen

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-12-14
 */
object BeePersistentBlazeOptions {
    const val scanPackage = "persistentScanPackage"
    const val packageName = "persistentPackageName"
    const val depth = "persistentDepth"
}

data class BeePersistentBlazeConfig(
    val packageName: String,
    val depth: Int,
)