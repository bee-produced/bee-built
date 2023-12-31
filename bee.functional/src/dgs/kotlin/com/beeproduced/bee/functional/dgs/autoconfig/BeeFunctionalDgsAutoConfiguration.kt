package com.beeproduced.bee.functional.dgs.autoconfig

import com.beeproduced.bee.functional.dgs.result.fetcher.implementation.aspect.ResultFetcherAspectConfiguration
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-10-23
 */
@Configuration
@EnableAspectJAutoProxy
open class BeeFunctionalDgsAutoConfiguration : ResultFetcherAspectConfiguration()