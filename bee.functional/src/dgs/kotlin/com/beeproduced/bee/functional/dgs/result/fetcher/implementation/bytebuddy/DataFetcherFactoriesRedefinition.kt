package com.beeproduced.bee.functional.dgs.result.fetcher.implementation.bytebuddy

import com.beeproduced.bee.functional.dgs.result.fetcher.helper.DataFetcherErrThrower
import com.beeproduced.bee.functional.dgs.result.fetcher.helper.DataFetcherResultHelper
import com.beeproduced.bee.functional.result.errors.BadRequestError
import com.beeproduced.bee.functional.result.errors.InternalAppError
import com.beeproduced.bee.functional.result.errors.ResultError
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrThrow
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetcher
import graphql.schema.DataFetcherFactories
import graphql.schema.DataFetchingEnvironment
import java.util.concurrent.CompletionStage
import java.util.function.BiFunction
import net.bytebuddy.ByteBuddy
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy
import net.bytebuddy.implementation.MethodDelegation
import net.bytebuddy.matcher.ElementMatchers

/**
 * Allows data fetchers to return `com.github.michaelbull.result.Result` natively with the help of
 * ByteBuddy.
 *
 * Based on:
 * https://github.com/graphql-java/graphql-java/blob/5eb2ab743bf7afc05e2243d3dc0bcf22a0384366/src/main/java/graphql/schema/DataFetcherFactories.java
 *
 * @author Kacper Urbaniec
 * @version 2023-11-04
 */
open class DataFetcherFactoriesRedefinition {

  companion object {

    fun makeWrapDataFetcherFunctional(byteBuddy: ByteBuddy) {
      byteBuddy
        .redefine(DataFetcherFactories::class.java)
        .method(ElementMatchers.named("wrapDataFetcher"))
        .intercept(MethodDelegation.to(DataFetcherFactoriesRedefinition::class.java))
        .make()
        .load(
          DataFetcherFactories::class.java.classLoader,
          ClassReloadingStrategy.fromInstalledAgent(),
        )
    }

    @JvmStatic
    @Suppress("unused")
    fun wrapDataFetcherFunctional(
      delegateDataFetcher: DataFetcher<*>,
      mapFunction: BiFunction<DataFetchingEnvironment?, Any?, Any?>,
    ): DataFetcher<*> {
      return DataFetcher { environment: DataFetchingEnvironment? ->
        val value = delegateDataFetcher[environment]
        if (value is CompletionStage<*>) {
          @Suppress("UNCHECKED_CAST")
          (value as CompletionStage<Any?>).thenApply<Any?> { v: Any? ->
            mapFunction.applyResultValue(environment, v)
          }
        } else {
          mapFunction.applyResultValue(environment, value)
        }
      }
    }

    private fun <T : DataFetchingEnvironment?, U : Any?, R : Any?> BiFunction<T, U, R>
      .applyResultValue(t: T, u: U): R {
      @Suppress("UNCHECKED_CAST")
      val value =
        when (u) {
          is Result<*, *> -> handleResult(u)
          is DataFetcherResult<*> -> handleDataFetcherResult(u)
          else -> u
        }
          as U
      return apply(t, value)
    }

    private fun <U : Any?> handleResult(u: Result<U, *>): U {
      return u.getOrThrow { e ->
        when (e) {
          is BadRequestError ->
            DataFetcherResultHelper.BadRequestErr(e.description(), e.debugInfo())
          is InternalAppError -> DataFetcherResultHelper.InternalErr(e.description(), e.debugInfo())
          is ResultError -> DataFetcherResultHelper.InternalErr(e.description(), e.debugInfo())
          else -> DataFetcherResultHelper.InternalErr<U>(e.toString())
        }.let(::DataFetcherErrThrower)
      }
    }

    private fun handleDataFetcherResult(u: DataFetcherResult<*>): DataFetcherResult<*> {
      if (u.errors.isNotEmpty() && u.data == null) throw DataFetcherErrThrower(u)
      return u
    }
  }
}
