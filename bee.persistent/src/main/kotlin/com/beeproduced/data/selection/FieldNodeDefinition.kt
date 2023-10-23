package com.beeproduced.data.selection

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-05-04
 */
interface FieldNodeDefinition {
    val field: String
    val fields: Set<FieldNodeDefinition>?
}