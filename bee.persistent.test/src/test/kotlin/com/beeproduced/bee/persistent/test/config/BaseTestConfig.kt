package com.beeproduced.bee.persistent.test.config

import com.netflix.graphql.dgs.autoconfig.DgsExtendedScalarsAutoConfiguration
import com.netflix.graphql.dgs.pagination.DgsPaginationAutoConfiguration
import name.nkonev.multipart.springboot.graphql.server.MultipartGraphQlWebMvcAutoconfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.graphql.GraphQlAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties

/**
 * @author Kacper Urbaniec
 * @version 2024-01-14
 */
@SpringBootApplication(
  scanBasePackages = ["com.beeproduced.datasource.test"],
  exclude =
    [
      GraphQlAutoConfiguration::class,
      DgsPaginationAutoConfiguration::class,
      DgsExtendedScalarsAutoConfiguration::class,
      MultipartGraphQlWebMvcAutoconfiguration::class,
    ],
  excludeName =
    ["com.netflix.graphql.dgs.springgraphql.autoconfig.DgsSpringGraphQLAutoConfiguration"],
)
@EnableConfigurationProperties
class BaseTestConfig
