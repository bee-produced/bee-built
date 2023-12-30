package com.beeproduced.bee.persistent.blaze.processor.utils

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-12-30
 */

fun buildUniqueClassName(packageName: String, simpleName: String): String {
    // Split the package name into its components
    val packageParts = packageName.split(".")

    // Capitalize each part of the package name
    val capitalizedPackageParts = packageParts.map { it.capitalize() }

    // Capitalize the simple name
    val capitalizedSimpleName = simpleName.capitalize()

    // Concatenate all parts together
    return capitalizedPackageParts.joinToString("") + capitalizedSimpleName
}