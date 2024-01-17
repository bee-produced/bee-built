<div align="center">
  <p><i>Proof of Concept</i></p>  
  <h1><code>bee.persistent.blaze</code></h1>
  <p>
     <strong>Easier data handling for GraphQL + JPA</strong>
  </p>
</div>



## 💡 Motivation

* *Query what you need*: Automatic query translation from e.g. GraphQL
* *Idiomatic*: Focus on idiomatic Kotlin (e.g. allow data classes)
* *Functional*: Prefer immutable records & minimize side effects

But this comes at a cost by design: `bee.persistent`'s JPA flavour is more a sophisticated database mapping utility than a fully fletched ORM.

> Each approach has its advantages and disadvantages; it is therefore not surprising that Object-Relational Mapping is referred to as the [Vietnam of computer science](https://blog.codinghorror.com/object-relational-mapping-is-the-vietnam-of-computer-science/). There is probably no universally good solution, only compromises.

This project is a proof of concept for a successor to [bee.persistent.jpa](./JPA-README.md), as the entity graph backend has some limitations that should be resolved with [blaze-persistence](https://persistence.blazebit.com/documentation/1.6/core/manual/en_US/index.html) as the underlying db adapter. More specifically, blaze persistence views are used in the background together with fetch joins.

Essentially, `bee.persistence` in the `blaze` flavour generates at compile time a graph of all the possibilities of how an entity and its relationships can be fetched. The current rule is that the graph continues until it finds relationships of already visited entities (other than the root entity) and then expands in depth by a predefined value (2 by default).

Not only are views generated in the background; DSL objects are created for each entity to create type-safe queries that are particularly useful for use in where clauses.

## 🚀 Quickstart

### Define Entities

If one has worked with JPA, the following will be familiar. `bee.persistent` is a JPA superset which ironically limits functionality by design.

> No JPA experience? Start with some [fundamentals](https://www.baeldung.com/jpa-entities) before continuing  your `bee.persistent` journey!

In contrast to JPA one can use and is even encouraged to use data classes for entities. Immutability is not a problem for `bee.persistent` as all changes to entities are flushed at any step or [even directly mapped to database circumventing the persistence context](https://thorben-janssen.com/criteria-updatedelete-easy-way-to/).

However, there are some important gotchas that can be tolerated in JPA but not in `bee.persistent`:

* Each relation entity/collection marked with `FetchType.LAZY` must be nullable

* For each relation model the foreign key property explicitly and make the relation entity not insertable nor updateable

  ```kotlin
  // Bad
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "works_id", referencedColumnName = "id")
  val works: WorkCollection
  // Good
  @Column(name = "works_id")
  val worksKey: Long,
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "works_id", referencedColumnName = "id", insertable = false, updatable = false)
  val works: WorkCollection? = null
  ```

* Use `@EmbeddedId` instead of `@IdClass` for composite keys

  > ⚠️  `@IdClass` is not supported by blaze-persistence.

In view of this, a simple one-to-many association with a composite key can be modelled as follows.

> 🪧 The code is taken from the examples in the folder `bee.persistent.test`.

```kotlin
data class WorkId(
    @Column(name = "work_id")
    val id: Long = -1,
    @Column(name = "works_id")
    val worksKey: Long = -1
) : Serializable

@Entity
@Table(name = "works")
data class Work(
    @EmbeddedId
    val id: WorkId,
    val txt: String,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "works_id", referencedColumnName = "id", insertable = false, updatable = false)
    val workCollection: WorkCollection? = null
)

@Entity
@Table(name = "work_collections")
data class WorkCollection(
    @Id
    @GeneratedValue
    val id: Long = -1,
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "workCollection")
    val works: Set<Work>? = null
)
```

### Create Repositories

`bee.persistent` uses custom repositories for data access and not standard ones like Spring Data's `CrudRepository`. 

But no worries, they are quite comparable and easy to define. Just define an interface that implements `BeeBlazeRepository` to specify the entity type and its id type as generic  parameters. At compile time the repositories will be generated.

> ⚠️ Do not forget the `@BeeRepository` annotation as it is crucial for code generation.

```kotlin
@BeeRepository
interface WorkRepository: BeeBlazeRepository<Work, WorkId>

@BeeRepository
interface WorkCollectionRepository: BeeBlazeRepository<WorkCollection, Long>
```

### Configuration

The following shows the easiest way to incorporate `bee.persistent` into a project.

`settings.gradle.kts`

```kotlin
pluginManagement {
    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "bee.generative" -> useModule("com.beeproduced:bee.generative:<BEE_BUILT_VERSION>")
            }
        }
    }
}
```

> ⚠️ As `bee.generative` is currently not published to the gradle plugin portal, the publication on maven central has no [plugin marker](https://docs.gradle.org/current/userguide/plugins.html#sec:plugin_markers) and thus requires this [workaround](https://github.com/GoogleCloudPlatform/app-gradle-plugin/issues/397#issuecomment-1484070866).

`build.gradle.kts`:

```kotlin
plugins {
    id("bee.generative")
    id("com.google.devtools.ksp") version "1.9.10-1.0.13"
}

dependencies {
    implementation("com.beeproduced:bee.persistent:<BEE_BUILT_VERSION>") {
    beeGenerative("com.beeproduced:bee.persistent:<BEE_BUILT_VERSION>", "blaze")
}

// DGS codegen
tasks.withType<GenerateJavaTask> {
    packageName = "<package-name>"
    subPackageNameTypes = "<dto-package-name>"
    ...
}

// bee.persistent codegen
beeGenerative {
    arg("persistentPackageName", "<entity-package-name>")
    arg("persistentSubPackageRepository", "repositories")
    arg("persistentSubPackageView", "views")
    arg("persistentSubPackageDSL", "dsl")
}
```

> 🪧 To see complete `bee.persistent` logs append `--info` to a gradle run task like `kspKotlin --rerun-tasks --info`.

Lastly, define a configuration component that sets the entity manager, transaction manager and blaze components.

> 🪧 This part will be simplified in the future.

An example can be found [here](https://github.com/bee-produced/bee-built/blob/main/bee.persistent.test/datasource.test/src/main/kotlin/com/beeproduced/datasource/test/DbConfig.kt).  Annotate the configuration with `@EnableBeeRepositories` for correct code generation. Leave values empty when only one bean of such type exists. 

```kotlin
@EnableBeeRepositories(
    basePackages = ["com.beeproduced.datasource.test"],
    entityManagerFactoryRef = "testEM",
    criteriaBuilderFactoryRef = "testCBF",
    entityViewManagerRef = "testEVM"
)
```

### Create Simple Queries

`bee.persistent`'s repository API is quite comparable to Spring Data's [`CrudRepository`](https://docs.spring.io/spring-data/data-commons/docs/current/api/org/springframework/data/repository/CrudRepository.html). A notable difference, however, is that there is no universal `save`  method for persisting and updating; `bee.persistent` explicitly provides a  `persist` method for entering new entities and an `update` method for  the operation of the same name.

```kotlin
var collectionId1: Long = -1
var workId1 = WorkId()

transaction.executeWithoutResult {
    val collection1 = collectionRepo.persist(WorkCollection())
    collectionId1 = collection1.id
    val work1 = workRepo.persist(Work(WorkId(42, collectionId1), "Hey!"))
    workId1 = work1.id
}

transaction.executeWithoutResult {
    val work = workRepo.selectById(workId1)
    assertNotNull(work)
    val workUpdate = work.copy(txt = "Update!")
    workRepo.update(workUpdate)
}

transaction.executeWithoutResult {
    val workUpdate = workRepo.selectById(workId1)

    assertNotNull(workUpdate)
    assertEquals("Update!", workUpdate.txt)
}
```

> 🪧 Transactions can be modelled implicitly via the `@TransactionalResult`  annotations. For more information, refer to section *Transactions*.

#### Fun with Selections or: Query within the Query

In JPA, relation properties marked with `FetchType.LAZY` are returned as proxies. When accessing a field or method of such a proxy, a database query is executed to retrieve the data for that entity. Since `bee.persistent` strives to  minimise side effects, no proxies are returned in such cases, but a  `null` value is returned.

```kotlin
var collectionId: Long = -1
transaction.executeWithoutResult {
    val collection = collectionRepo.persist(WorkCollection())
    collectionId = collection.id
    val work1 = workRepo.persist(Work(WorkId(1, collectionId), "Hey!"))
    val work2 = workRepo.persist(Work(WorkId(2, collectionId), "Moin!"))
}
val collection = collectionRepo.selectById(collectionId)
// Works will be null
println(collection.works)
```

To load a lazy relation in `bee.persistent`, it must be explicitly mentioned in a *selection*. Each select method takes an additional optional  parameter of the `BeeSelection` interface, which is a graph listing all the relations to be eagerly loaded for the query.

> 🪧 `bee.persistent` internally uses the blaze-persistence fetch joins, [more specifically the `fetch` method from the `EntityViewSetting`](https://persistence.blazebit.com/documentation/1.6/entity-view/manual/en_US/index.html#fetching-a-data-subset).

On default, all select methods default to `BeeSelection.empty()` which loads no relations. To load the `works` relation from the previous example, one can use a `SelectionBuilder` like the following:

```kotlin
val selection = BeeSelection.create {
    field("works")
}
```

However, a better approach is to use the generated DLS object which is created for each entity. With it, one can specify relations and lazy fields in a typesafe manner:

```kotlin
var collectionId: Long = -1
transaction.executeWithoutResult {
    val collection = collectionRepo.persist(WorkCollection())
    collectionId = collection.id
    val work1 = workRepo.persist(Work(WorkId(1, collectionId), "Hey!"))
    val work2 = workRepo.persist(Work(WorkId(2, collectionId), "Moin!"))
}
val selection = WorkCollectionDSL.select {
    this.works { }
}
val collection = collectionRepo.selectById(collectionId, selection)
// Works will be loaded
println(collection.works)
```

### Transactions

Most JPA implementations such as Hibernate provide Automatic Dirty checking whereby changes to a managed entity are automatically saved to the database when the session is flushed or the transaction is committed. 

In `bee.persistent`, all entities are automatically detached after persistence. One reason for this is that the Criteria API (used by blaze-persistence internally) bypasses the persistence context for update or delete operations, leaving managed entities in an inconsistent state. Also, queries to the Criteria API can reuse the entity cache, which can result in missing relationships that were explicitly marked in the load graph converted from a data selection. In summary, this means that in `bee.persistent` every entity is free of side effects; every operation must be performed explicitly. This should not be an obstacle, as `bee.persistent` recommends, in the spirit of immutable design, to perform operations with updated copies of an existing entities.

When working with [kotlin result](https://github.com/michaelbull/kotlin-result) for error handling, one can use `@TransactionalResult` on methods that return `Result<V, E>` to implicitly wrap method execution in a database transaction. This should be familiar if one has already used [`@Transactionl` from Spring Data](https://www.baeldung.com/transaction-configuration-with-jpa-and-spring); in fact, the `@TransactionalResult` API is based heavily on its Spring counterpart.

```kotlin
@TransactionalResult(
	"orderTransactionManager",
	exceptionDescription = "Could not fetch orders", 
	readOnly = true
)
```

However, there is a difference in how exceptions are handled. `@Transactional` only rolls back on runtime exceptions, `@TransactionalResult` rolls back on *all* exceptions, including checked ones. All exceptions are caught and returned as `AppError` with an error message from the value of the `exceptionDescription` from the `@TransactionalResult` annotation. Error messages from `Result` errors retain their message and are not overwritten with `exceptionDescription`.

### Model `m:n` Relations

In `bee.persistent` relations should be modelled explicitly therefore it is not advised to propagate (cascade) operations to related entities. In JPA one can persist an entity with included relation entities in one step, in `bee.persistent` each relation is inserted on its own via its own repository.

```kotlin
@Entity
@Table(name = "foos")
data class Foo(
    @Id
    @GeneratedValue
    val id: Long = -1,
    @ManyToMany(fetch = FetchType.LAZY, cascade = [CascadeType.DETACH])
    @JoinTable(
        name = "foo_bar_relations",
        joinColumns = [JoinColumn(name = "foo")],
        inverseJoinColumns = [JoinColumn(name = "bar")]
    )
    val bars: Set<Bar>? = null
)

@Entity
@Table(name = "bars")
data class Bar(
    @Id
    @GeneratedValue
    val id: Long = -1,
    @ManyToMany(mappedBy="bars", fetch = FetchType.LAZY, cascade = [CascadeType.DETACH])
    val foos: Set<Foo>? = null
)

@Embeddable
data class FooBarId(
    val foo: Long = -1,
    val bar: Long = -1,
) : Serializable

@Entity
@Table(name = "foo_bar_relations")
data class FooBarRelation(
    @EmbeddedId
    val id: FooBarId
)
```

To establish a relation between a `Foo` & `Bar` entity, one just persists the corresponding relation entity with the primary keys of both entities.

```kotlin
var barId: Long = -1
var foo1Id: Long = -1

transaction.executeWithoutResult {
    val bar = barRepo.persist(Bar())
    barId = bar.id
    val foo1 = fooRepo.persist(Foo())
    foo1Id = foo1.id
}

transaction.executeWithoutResult {
    fooBarRepo.persist(FooBarRelation(FooBarId(foo1Id, barId)))
}

val barSelection = BarDSL.select { this.foos { this.bars { this.foos { } } } }
val fooSelection = FooDSL.select { this.bars { this.foos { this.bars { } } } }
transaction.executeWithoutResult {
    val bar = barRepo.selectById(barId, barSelection)
    assertNotNull(bar)
    assertBar(bar, setOf(barId), setOf(foo1Id), 3)
    val foo1 = fooRepo.selectById(foo1Id, fooSelection)
    assertNotNull(foo1)
    assertFoo(foo1, setOf(foo1Id), setOf(barId), 3)
}
```

### Advanced Queries

In the previous examples, only simple, predefined operations were shown. It has also been mentioned that `bee.persistent` is built on top of the blaze-persistence, but no Criteria Builder Queries have been shown so far.

Repository operations that provide extensibility of Criteria Builder Queries contain a parameter `dsl` that represents a lambda. User-defined queries can be inserted into these lambdas.

```kotlin
fun select(
    selection: BeeSelection = BeeSelection.empty(),
    dsl: SelectQuery<T>.() -> Selection<T> = { this }
): List<T>
```

> ⚠️ `bee.persistance` does not support currently all blaze-persistance clauses/operators. They will be gradually implemented.

The following example shows how to query with a `WHERE` clause referencing an entity from a  `JOINED` table.

```kotlin
var collectionId1: Long = -1
var collectionId2: Long = -1
var workId1 = WorkId()
var workId2 = WorkId()

transaction.executeWithoutResult {
    val collection1 = collectionRepo.persist(WorkCollection())
    collectionId1 = collection1.id
    val collection2 = collectionRepo.persist(WorkCollection())
    collectionId2 = collection2.id
    val work1 = workRepo.persist(Work(WorkId(1, collectionId1), "Hey!"))
    workId1 = work1.id
    val work2 = workRepo.persist(Work(WorkId(2, collectionId2), "Moin!"))
    workId2 = work2.id
}

transaction.executeWithoutResult {
    val selection = WorkCollectionDSL.select {
        this.works { this.workCollection { this.works {  } } }
    }

    // Query `WorkCollection` where `works` includes entity with id `work2Id`
    val collection = collectionRepo.select(selection) {
        where(WorkCollectionDSL.works.id.eq(workId2))
    }.firstOrNull()

    assertNotNull(collection)
    assertWorkCollection(collection, collectionId2, setOf(workId2), 3)
}
```

> ⚠️ If the value that should be compared is an [inline value class](https://kotlinlang.org/docs/inline-classes.html), use for example `eqInline` instead of `eq`. All operators have `inline` equivalents that take boxed values. 

---

## Future Implementation Roadmap

Following features will be implemented after benchmarking this proof of concept againts `bee.persistent.jpa`:

- [ ] `BeeBlazeRepository`: `delete` methods
- [ ] `BeeBlazeRepository`: `exists` methods
- [ ] `BeeBlazeRepository`: `count` methods
- [ ] `Predicate`: Implement [basic predicates (`greater`, `greaterThan`, ...)](https://persistence.blazebit.com/documentation/1.6/core/manual/en_US/index.html#expressions)
- [ ] `Predicate`: Implement [string functions](https://persistence.blazebit.com/documentation/1.6/core/manual/en_US/index.html#string-functions)
- [ ] `Predicate`: Provide [subquery support](https://persistence.blazebit.com/documentation/1.6/core/manual/en_US/index.html#subquery-expressions)
- [ ] `Instantiators`: Map primitive nullable Foreign Keys (e.g. `Long?`) to `null` instead of `0`
- [ ] `Instantiators`: Map empty `1:n` relations to `null` instead of empty collection
- [ ] `Pagination`: Provide pagination support via blaze-persistence keyset pagination
- [ ] `SelectQuery`: Provide `unsafe` access to `CriteriaBuilder`
- [ ] `Config`: Generate spring config that creates blaze beans
- [ ] `DGS`: Provide DGS adapters (alternative names / skip overs)