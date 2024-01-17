package com.beeproduced.bee.persistent.blaze.selection

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-12-31
 */
interface FieldNodeDefinition {
    val field: String
    val type: String?
    val fields: Set<FieldNodeDefinition>?
}