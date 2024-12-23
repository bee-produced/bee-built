package com.beeproduced.bee.persistent.test.config

import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration
import com.netflix.graphql.dgs.subscriptions.websockets.DgsWebSocketAutoConfig
import com.netflix.graphql.dgs.webmvc.autoconfigure.DgsWebMvcAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties

/**
 * @author Kacper Urbaniec
 * @version 2024-01-14
 */
@SpringBootApplication(
  scanBasePackages = ["com.beeproduced.datasource.a"],
  exclude =
    [DgsAutoConfiguration::class, DgsWebMvcAutoConfiguration::class, DgsWebSocketAutoConfig::class],
)
@EnableConfigurationProperties
class ATestConfig
