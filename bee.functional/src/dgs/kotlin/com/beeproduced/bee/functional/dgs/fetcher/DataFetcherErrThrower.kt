package com.beeproduced.bee.functional.dgs.fetcher

import graphql.execution.DataFetcherResult

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2022-10-11
 */
class DataFetcherErrThrower(val error: DataFetcherResult<*>) : RuntimeException()
