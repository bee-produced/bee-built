package com.beeproduced.data.jpa.exceptions

import kotlin.reflect.KType

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-02-21
 */
class UnsupportedRelationType(type: KType, count: Int) : RuntimeException(
    "Unsupported relation type [$type] with $count generic parameters found\nCurrently only `List` & `Set` are supported"
)