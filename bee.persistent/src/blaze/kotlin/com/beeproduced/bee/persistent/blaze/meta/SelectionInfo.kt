package com.beeproduced.bee.persistent.blaze.meta

/**
 * @author Kacper Urbaniec
 * @version 2023-12-31
 */
data class SelectionInfo(
  val view: String,
  val relations: Map<String, SelectionInfo>,
  val id: String?, // Embedded entities have no id
  val columns: Set<String>,
  val lazyColumns: Set<String>,
  val embedded: Map<String, SelectionInfo>,
  val lazyEmbedded: Map<String, SelectionInfo>,
)
