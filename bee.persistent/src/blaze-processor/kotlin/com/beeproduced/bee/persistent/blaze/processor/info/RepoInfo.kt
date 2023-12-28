package com.beeproduced.bee.persistent.blaze.processor.info

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-12-28
 */

class RepoInfo(

)

data class RepoConfig(
    val basePackages: List<String>,
    val entityManagerFactoryRef: String,
    val criteriaBuilderFactoryRef: String,
    val entityViewManagerRef: String
)