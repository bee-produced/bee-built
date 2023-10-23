package com.beeproduced.bee.persistent.selection

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-05-04
 */
interface FieldNodeDefinition {
    val field: String
    val type: String?
    val fields: Set<FieldNodeDefinition>?
}