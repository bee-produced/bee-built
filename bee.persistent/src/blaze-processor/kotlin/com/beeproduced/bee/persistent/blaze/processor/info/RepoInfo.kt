package com.beeproduced.bee.persistent.blaze.processor.info

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType

/**
 * @author Kacper Urbaniec
 * @version 2023-12-28
 */
data class RepoInfo(
  val repoInterface: KSClassDeclaration,
  val entityType: KSType,
  val idType: KSType,
  val config: RepoConfig?,
)

data class RepoConfig(
  val basePackages: List<String>,
  val entityManagerFactoryRef: String,
  val criteriaBuilderFactoryRef: String,
  val entityViewManagerRef: String,
)
