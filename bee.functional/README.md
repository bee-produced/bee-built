<div align="center">
  <h1><code>bee.functional</code></h1>
  <p>
     <strong>Functional Kotlin bindings, integration with DGS, bee.persistent & more</strong>
  </p>
</div>



## bee.functional

Predefined error types, extensions & more for [`com.github.michaelbull.result.Result`](https://github.com/michaelbull/kotlin-result).

```kotlin
typealias AppResult<V> = Result<V, AppError>

open class BadRequestError : AppError
open class InternalAppError : AppError
```

## bee.functional.dgs

Adds support for result data fetchers.

```kotlin
@DgsQuery
fun bar(): Bar {
    return Bar(listOf("Bar"))
}

@DgsData(
    parentType = DgsConstants.BAR.TYPE_NAME,
    field = DgsConstants.BAR.Waldos
)
fun barWaldos(): AppResult<List<Waldo>> {
    return Err(BadRequestError("Bar"))
}
```

Redefines certain graphql invocations at runtime via ByteBuddy & provides custom exception handler in order to work.

> ⚠️ When Spring is started, an attempt is made to install a ByteBuddy agent. If this fails and is not supported by the JVM platform used, one can use the older error handling model.
>
> One needs to provide a configuration based on the `ResultFetcherAspectConfiguration` class. This will create an aspect that handles `DataFetcherResults` like typical `Results`. Please note that the data fetcher must also return `DataFetcherResult`. One can convert a `Result` into one using the `getDataFetcher()` extension method. Directly returing a `Result` type is not supported in this model.

> ⚠️ Usage with data loader is currently evaluated and not supported. Please transform `Results` manually to [`Try`](https://netflix.github.io/dgs/data-loaders/#implementing-a-data-loader-with-try) if needed.

## bee.functional.persistent

Provides `@TransactionalResult` annotation & corresponding aspect that provides JPA transactions support for methods that return a `Result`.

Based on the timeless `@Transactional` annotation from Spring.
