package com.beeproduced.bee.functional.dgs.autoconfig

import com.beeproduced.bee.functional.dgs.result.fetcher.implementation.aspect.ResultFetcherAspectConfiguration
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy

/**
 * @author Kacper Urbaniec
 * @version 2024-12-19
 */
@Configuration
@EnableAspectJAutoProxy
open class BeeFunctionalDgsAspectAutoConfiguration : ResultFetcherAspectConfiguration()
