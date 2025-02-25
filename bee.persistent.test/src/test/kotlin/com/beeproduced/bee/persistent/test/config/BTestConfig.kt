package com.beeproduced.bee.persistent.test.config

import com.netflix.graphql.dgs.autoconfig.DgsExtendedScalarsAutoConfiguration
import com.netflix.graphql.dgs.pagination.DgsPaginationAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.graphql.GraphQlAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties

/**
 * @author Kacper Urbaniec
 * @version 2024-01-14
 */
@SpringBootApplication(
  scanBasePackages = ["com.beeproduced.datasource.b"],
  exclude =
    [
      GraphQlAutoConfiguration::class,
      DgsPaginationAutoConfiguration::class,
      DgsExtendedScalarsAutoConfiguration::class,
    ],
  excludeName =
    ["com.netflix.graphql.dgs.springgraphql.autoconfig.DgsSpringGraphQLAutoConfiguration"],
)
@EnableConfigurationProperties
class BTestConfig
