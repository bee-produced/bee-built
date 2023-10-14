package com.beeproduced.bee.fetched.codegen

import com.beeproduced.bee.fetched.annotations.BeeFetched

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-10-14
 */
object BeeFetchedOption {
    const val scanPackage = "fetchedScanPackage"
    const val packageName = "fetchedPackageName"
    val beeFetchedAnnotationName = BeeFetched::class.qualifiedName!!
}