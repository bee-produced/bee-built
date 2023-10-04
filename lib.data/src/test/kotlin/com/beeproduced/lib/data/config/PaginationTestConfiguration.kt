package com.beeproduced.lib.data.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.ComponentScan

@TestConfiguration
@ComponentScan(
    basePackages = [
        "com.beeproduced.lib.data.config",
        "com.beeproduced.lib.data.pagination"
    ]
)
class PaginationTestConfiguration {
}