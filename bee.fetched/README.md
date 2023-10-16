<div align="center">
  <h1><code>bee.fetched</code></h1>
  <p>
     <strong>Automatically generate nested data fetchers for usage with data loaders</strong>
  </p>
</div>



## 💡 Motivation

Calling data loaders using nested data fetchers is pretty straightforward but often requires writing of nearly identical boilerplate code. 

Also, these types of data fetchers can be pretty easy to miss while implementing. This leads to incomplete results even if the data loader was correctly defined.

This code generation library tries to solve this problem by automatically generating such nested data fetchers from DGS DTOs & data loader definitions.

## 🚀 Quickstart

### 🛠️ Configuration

Following shows the easiest way to incorporate `bee.fetched` into a project.

`build.gradle.kts`:

```kotlin
plugins {
    id("bee.generative")
}

dependencies {
    beeGenerative("com.beeproduced:bee.fetched")
}

// DGS codegen
tasks.withType<GenerateJavaTask> {
    packageName = "<package-name>"
    subPackageNameTypes = "<dto-package-name>"
    ...
}

// bee.fetched codegen
// Fetched scan packege must match DGS codegen path
// Fetched package name is where the generated nested datafetchers will be placed
beeGenerative {
    arg("fetchedScanPackage", "<package-name>.<dto-package-name>")
    arg("fetchedPackageName", "<package-name>.fetcher")
}
```

> 🪧 To see complete `bee.fetched` logs append `--info` to a gradle run task like `kspKotlin --rerun-tasks --info`.

### ⭐ Usage

Let's assume one has the following schema and wants to load the `Waldo` type via a data loader.

```crystal
extend type Query {
    foo: Foo!
    bar: Bar!
    qux: Qux!
    quux: Quux!
    corge: Corge!
    grault: Grault!
    fred: Fred!
    plugh: Plugh!
    xyzzy: Xyzzy!
    garply: Garply!
}

type Waldo {
    waldo: String!
}
```

```kotlin
@BeeFetched(
    mappings = [
        FetcherMapping(Corge::class, DgsConstants.CORGE.Waldo, DgsConstants.CORGE.CorgeToWaldoId)
    ],
    internalTypes = [
        FetcherInternalType(Grault::class, TestController.MyGrault::class, DgsConstants.GRAULT.Waldo),
        FetcherInternalType(Fred::class, TestController.MyFred::class, DgsConstants.FRED.Waldo),
        FetcherInternalType(Plugh::class, TestController.MyPlugh::class, DgsConstants.PLUGH.Waldos),
        FetcherInternalType(Xyzzy::class, TestController.MyXyzzy::class, DgsConstants.XYZZY.Waldos),
    ],
    ignore = [
        FetcherIgnore(Garply::class, DgsConstants.GARPLY.Waldo),
        FetcherIgnore(Waldo::class)
    ],
    safeMode = true,
    safeModeOverrides = []
)
@DgsDataLoader(name = "Waldo")
class WaldoDataLoader : MappedBatchLoaderWithContext<String, Waldo> {
    override fun load(
        keys: Set<String>,
        environment: BatchLoaderEnvironment,
    ): CompletionStage<Map<String, Waldo>> {
        return CompletableFuture.supplyAsync {
            keys.associateWith { Waldo(it) }
        }
    }
}
```

> ⚠️ Please do not forget to annotate the data loader with `@BeeFetcher`  if one wants to utilise code generation.

> 🪧 Ignore `@BeeFetched` for the moment, the annotation will be explained in the following step by step.

With the help of `bee.fetched` all of the corresponding nested data fetchers including their data loader invocations can be automatically generated.

#### Using well-formed DTOs - No configuration needed

In the following schema `waldo` and `waldos` are the fields that should be loaded via a data loader. The library needs to identify the fields that contain the keys for the fields to be loaded.

A simple approach is used:

* Entities like `waldo` => Search for `waldoId`
* Collections like `waldos` => Search for `waldoIds` or `waldosIds`
* Not modelled but possible: A collection called `waldo` => Search for `waldoIds` or `waldosIds`

```crystal
type Foo {
    # Simple case for singular id
    waldoId: ID!
    waldo: Waldo
}

type Bar {
    # Simple case for plural ids
    waldoIds: [ID!]!
    waldos: [Waldo!]
}

type Qux {
    # Singular nullable id
    waldoId: ID
    waldo: Waldo
}

type Quux {
    # Plural nullable id
    waldoIds: [ID!]
    waldos: [Waldo!]
}
```

When this approach is applicable to a DTO the library automatically generates following nested data fetchers without additional configuration.

```kotlin
@DgsData(
    parentType = "Foo",
    field = "waldo",
)
public fun fooWaldo(dfe: DataFetchingEnvironment): CompletableFuture<Waldo?> {
    val data = dfe.getSource<Foo>()
    if (data.waldo != null) return CompletableFuture.completedFuture(data.waldo)
    val dataLoader: DataLoader<String, Waldo> = dfe.getDataLoader("Waldo")
    val id = data.waldoId
    return dataLoader.load(id)
}

@DgsData(
    parentType = "Bar",
    field = "waldos",
)
public fun barWaldos(dfe: DataFetchingEnvironment): CompletableFuture<List<Waldo>?> {
    val data = dfe.getSource<Bar>()
    if (!data.waldos.isNullOrEmpty()) return CompletableFuture.completedFuture(data.waldos)
    val dataLoader: DataLoader<String, Waldo> = dfe.getDataLoader("Waldo")
    val ids = data.waldoIds
    return dataLoader.loadMany(ids)
}

@DgsData(
    parentType = "Qux",
    field = "waldo",
)
public fun quxWaldo(dfe: DataFetchingEnvironment): CompletableFuture<Waldo?> {
    val data = dfe.getSource<Qux>()
    if (data.waldo != null) return CompletableFuture.completedFuture(data.waldo)
    val dataLoader: DataLoader<String, Waldo> = dfe.getDataLoader("Waldo")
    val id = data.waldoId
    if (id == null) return CompletableFuture.completedFuture(data.waldo)
    return dataLoader.load(id)
}
  
@DgsData(
    parentType = "Quux",
    field = "waldos",
)
public fun quuxWaldos(dfe: DataFetchingEnvironment): CompletableFuture<List<Waldo>?> {
    val data = dfe.getSource<Quux>()
    if (!data.waldos.isNullOrEmpty()) return CompletableFuture.completedFuture(data.waldos)
    val dataLoader: DataLoader<String, Waldo> = dfe.getDataLoader("Waldo")
    val ids = data.waldoIds
    if (ids.isNullOrEmpty()) return CompletableFuture.completedFuture(data.waldos)
    return dataLoader.loadMany(ids)
}
```

#### Feeling special today - Map identifier in not well-formed DTOs

In the following case the library cannot determine the identifier and needs some manual assistance.

```crystal
type Corge {
    # Completely unrelated naming
    corgeToWaldoId: ID!
    waldo: Waldo
}
```

To do so, one must provide a `FetcherMapping` via `@BeeFetched` that maps `Corge`'s `waldo` field to the id `corgeToWaldoId`.

```kotlin
@BeeFetched(
    mappings = [
        FetcherMapping(Corge::class, DgsConstants.CORGE.Waldo, DgsConstants.CORGE.CorgeToWaldoId)
    ],
    ...
)
@DgsDataLoader(name = "Waldo")
class WaldoDataLoader : MappedBatchLoaderWithContext<String, Waldo>
```

> 🪧 One could also write `FetcherMapping(Corge::class, "waldo", "corgeToWaldoId")`, however this approach is not safe for changes.

This results in following generated code.

```kotlin
@DgsData(
    parentType = "Corge",
    field = "waldo",
)
public fun corgeWaldo(dfe: DataFetchingEnvironment): CompletableFuture<Waldo?> {
    val data = dfe.getSource<Corge>()
    if (data.waldo != null) return CompletableFuture.completedFuture(data.waldo)
    val dataLoader: DataLoader<String, Waldo> = dfe.getDataLoader("Waldo")
    val id = data.corgeToWaldoId
    return dataLoader.load(id)
}
```

#### No identifier - Use an internal type

DGS supports the usage of [internal types](https://netflix.github.io/dgs/advanced/context-passing/#no-showid-use-an-internal-type) which means that identifiers may not be exposed in the schema. The library needs in these cases to know for which field the internal type is used to get the identifier. Identifier rules & mappings discussed in the last section also apply for internal types.

> 🪧 This also means that internal types can be not well-formed and may require additional `FetcherMappings`.

```crystal
type Grault {
    # Should be resolved with internal type id
    waldo: Waldo
}

type Fred {
    # Should be resolved with internal nullable id
    waldo: Waldo
}

type Plugh {
    # Should be resolved with internal type ids
    waldos: [Waldo!]
}

type Xyzzy {
    # Should be resolved with internal type nullable ids
    waldos: [Waldo!]
}
```

In these cases, one must provide a `FetcherInternalType` via `@BeeFetched` that maps DTOs to their internal representation. Internal types can substitute the DTO for all of their fields or for just one specific field (in this case for example `DgsConstants.GRAULT.Waldo`).

```kotlin
@BeeFetched(
    internalTypes = [
        FetcherInternalType(Grault::class, TestController.MyGrault::class, DgsConstants.GRAULT.Waldo),
        FetcherInternalType(Fred::class, TestController.MyFred::class, DgsConstants.FRED.Waldo),
        FetcherInternalType(Plugh::class, TestController.MyPlugh::class, DgsConstants.PLUGH.Waldos),
        FetcherInternalType(Xyzzy::class, TestController.MyXyzzy::class, DgsConstants.XYZZY.Waldos),
    ],
    ...
)
@DgsDataLoader(name = "Waldo")
class WaldoDataLoader : MappedBatchLoaderWithContext<String, Waldo>
```

> 🪧 If `Grault` would have another field `waldo2: Waldo` the library would use the `Grault` DTO and not the `TestController.MyGrault` internal type as it is only configured for `DgsConstants.GRAULT.Waldo`. Leaving `DgsConstants.GRAULT.Waldo` empty or adding an additional `FetcherInternalType` for `DgsConstants.GRAULT.Waldo2` would result in usage of the internal type.

This results in following generated code.

```kotlin
@DgsData(
    parentType = "Grault",
    field = "waldo",
)
public fun graultWaldo(dfe: DataFetchingEnvironment): CompletableFuture<Waldo?> {
    val data = dfe.getSource<MyGrault>()
    val dataLoader: DataLoader<String, Waldo> = dfe.getDataLoader("Waldo")
    val id = data.waldoId
    return dataLoader.load(id)
}

@DgsData(
    parentType = "Fred",
    field = "waldo",
)
public fun fredWaldo(dfe: DataFetchingEnvironment): CompletableFuture<Waldo?> {
    val data = dfe.getSource<MyFred>()
    val dataLoader: DataLoader<String, Waldo> = dfe.getDataLoader("Waldo")
    val id = data.waldoId
    if (id == null) throw IllegalStateException(
        "Tried to load nullable key into non-nullable data loader"
    )
    return dataLoader.load(id)
}

@DgsData(
    parentType = "Plugh",
    field = "waldos",
)
public fun plughWaldos(dfe: DataFetchingEnvironment): CompletableFuture<List<Waldo>?> {
    val data = dfe.getSource<MyPlugh>()
    val dataLoader: DataLoader<String, Waldo> = dfe.getDataLoader("Waldo")
    val ids = data.waldoIds
    return dataLoader.loadMany(ids)
}

@DgsData(
    parentType = "Xyzzy",
    field = "waldos",
)
public fun xyzzyWaldos(dfe: DataFetchingEnvironment): CompletableFuture<List<Waldo>?> {
    val data = dfe.getSource<MyXyzzy>()
    val dataLoader: DataLoader<String, Waldo> = dfe.getDataLoader("Waldo")
    val ids = data.waldoIds
    if (ids == null) throw IllegalStateException(
        "Tried to load nullable keys into non-nullable data loader"
    )
    return dataLoader.loadMany(ids)
}
```

> ⚠️ Be aware that trying to load nullable keys into a non-nullable data loader can result in an exception as it is undefined / undesirable behaviour.

#### No hard feelings - Do not generate nested data fetcher

If one needs a tailored nested data fetcher for a special case one can disable generation on a per type basis. 

```crystal
type Garply {
    # Implementation should NOT be generated
    waldo: Waldo
}
```

In the following, the generation of a nested data fetcher for `Garply`'s `waldo` is disabled. Also the `Waldo` type is generally disabled as it has no relevant fields and does not need to be analysed. 

```kotlin
@BeeFetched(
    ignore = [
        FetcherIgnore(Garply::class, DgsConstants.GARPLY.Waldo),
        FetcherIgnore(Waldo::class)
    ],
    ...
)
@DgsDataLoader(name = "Waldo")
class WaldoDataLoader : MappedBatchLoaderWithContext<String, Waldo>
```

> 🪧 As with `FetcherInternalType`, leaving `DgsConstants.GARPLY.Waldo` empty would disallow the generation of all `Waldo` fields on this type (which in this case makes no difference).

#### Safety first - Do not load what is already present

