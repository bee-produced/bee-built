package com.beeproduced.bee.persistent.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.ComponentScan

@TestConfiguration
@ComponentScan(
  basePackages =
    ["com.beeproduced.bee.persistent.config", "com.beeproduced.bee.persistent.pagination"]
)
class PaginationTestConfiguration {}
