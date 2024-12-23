package com.beeproduced.bee.functional.dgs.result.fetcher

import graphql.execution.DataFetcherExceptionHandler
import org.springframework.context.annotation.Bean

/**
 * @author Kacper Urbaniec
 * @version 2023-11-04
 */
abstract class ResultFetcherConfiguration {
  @Bean
  open fun beeFunctionalResultFetcherExceptionHandler(): DataFetcherExceptionHandler {
    return ResultFetcherExceptionHandler()
  }
}
