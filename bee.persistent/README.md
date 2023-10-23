<div align="center">
  <h1><code>lib.data</code></h1>
  <p>
     <strong>Easier data handling for GraphQL + JPA</strong>
  </p>
</div>

## 💡 Motivation

* *Query what you need*: Automatic query translation from e.g. GraphQL
* *Idiomatic*: Focus on idiomatic Kotlin (e.g. allow data classes)
* *Functional*: Prefer immutable records & minimize side effects

But this comes at a cost by design: `lib.data`'s JPA flavour is more a sophisticated database mapping utility than a fully fletched ORM.

> Each approach has its advantages and disadvantages; it is therefore not surprising that Object-Relational Mapping is referred to as the [Vietnam of computer science](https://blog.codinghorror.com/object-relational-mapping-is-the-vietnam-of-computer-science/). There is probably no universally good solution, only compromises.

## 🚀 Quickstart

### Define Entities

If one has worked with JPA, the following will be familiar. `lib.data` is a JPA superset which ironically limits functionality by design.

> No JPA experience? Start with some [fundamentals](https://www.baeldung.com/jpa-entities) before continuing  your `lib.data` journey!

In contrast to JPA one can use and is even encouraged to use data classes for entities. Immutability is not a problem for `lib.data` as all changes to entities are flushed at any step or [even directly mapped to database circumventing the persistence context](https://thorben-janssen.com/criteria-updatedelete-easy-way-to/).

However, there are some important gotchas that can be tolerated in JPA but not in `lib.data`:

* Use `Set<T>` instead of `List<T>` for collections

  Omits ["MultipleBagFetchException - cannot simultaneously fetch multiple bags"](https://stackoverflow.com/a/4335514/12347616)

* Always override `hashcode` (and also `equals`) and exclude relational properties in them

  Omits ["org.hibernate.HibernateException: collection was evicted"](https://stackoverflow.com/a/65176911/12347616)

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

* Use `@IdClass` instead of `@EmbeddedId` for composite keys

  > ⚠️  `@EmbeddedId` support is not verified / tested yet. 

* Favour composition over inheritance

  > ⚠️ Inheritance support is not verified / tested yet. 

Also, all entities must implement the `DataEntity<E>` interface to be compatible with `lib.data`.

> 🪧 This requirement may be lifted in the future.

In view of this, a simple one-to-many association with a composite key can be modelled as follows.

> 🪧 The code is taken from the examples in the folder `lib.data` - `test`.

```kotlin
data class WorkId(
    val id: Long = -1,
    val worksKey: Long = -1
) : Serializable

@Entity
@IdClass(WorkId::class)
@Table(name = "works")
data class Work(
    @Id
    @GeneratedValue
    val id: Long,
    @Id
    @Column(name = "works_id")
    val worksKey: Long,
    val txt: String,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "works_id", referencedColumnName = "id", insertable = false, updatable = false)
    val works: WorkCollection? = null
) : DataEntity<Work> {

    constructor(works: Long, txt: String) : this(-1, works, txt)
    
    override fun clone(): Work = this.copy()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Work) return false

        if (id != other.id) return false
        if (worksKey != other.worksKey) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + worksKey.hashCode()
        return result
    }
}

@Entity
@Table(name = "work_collections")
data class WorkCollection(
    @Id
    @GeneratedValue
    val id: Long,
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "works")
    val works: Set<Work>?
) : DataEntity<WorkCollection> {
    constructor() : this(-1, null)

    override fun clone(): WorkCollection = this.copy()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WorkCollection) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
```

### Create Repositories

`lib.data` uses custom repositories for data access and not standard ones like Spring Data's `CrudRepository`. 

But no worries, they are quite comparable and easy to define. One major  difference might be that JPA's entity manager has to be passed  explicitly. This was introduced to easily implement support for multiple data sources with independent entity managers. Apart from that, one  only has to specify the entity type and its id type as generic  parameters.

```kotlin
@Component
class WorkRepository(
    @Qualifier("orderEntityManager") em: EntityManager
) : BaseDataRepository<Work, WorkId>(em)

@Component
class WorkCollectionRepository(
    @Qualifier("orderEntityManager") em: EntityManager
) : BaseDataRepository<WorkCollection, Long>(em)
```

> ⚠️ Favour `@Component` over the `@Repository` annotation as the latter one can introduce unexpected side effects on non Spring Data repositories.

> ⚠️ Each entity in `lib.data` requires a repository even when it is not explicitly used. Without it, the internal metamodel of `lib.data` will be incomplete,  which may lead to unexpected exceptions when the entity is referenced by another, for example by a relationship.

### Create Simple Queries

`lib.data`'s repository API is quite comparable to Spring Data's [`CrudRepository`](https://docs.spring.io/spring-data/data-commons/docs/current/api/org/springframework/data/repository/CrudRepository.html). A notable difference, however, is that there is no universal `save`  method for persisting and updating; `lib.data` explicitly provides a  `persist` method for entering new entities and an `update` method for  the operation of the same name.

```kotlin
var collectionId: Long = -1
var workId: Long = -1

transaction.executeWithoutResult {
    val collection = collectionRepo.persist(WorkCollection())
    collectionId = collection.id
    val work = workRepo.persist(Work(collectionId, "Hey!"))
    workId = work.id
}

transaction.executeWithoutResult {
    val work = workRepo.selectById(WorkId(workId, collectionId))
    assertNotNull(work)
    val workUpdate = work.copy(txt = "Update!")
    workRepo.update(workUpdate)
}

val workUpdate = workRepo.selectById(WorkId(workId, collectionId))

assertNotNull(workUpdate)
assertEquals("Update!", workUpdate.txt)
```

> 🪧 Transactions can be modelled implicitly via the `@TransactionalResult`  annotations. For more information, refer to section *Transactions*.

#### Fun with Selections or: Query within the Query

In JPA, relation properties marked with `FetchType.LAZY` are returned as proxies. When accessing a field or method of such a proxy, a database query is executed to retrieve the data for that entity. Since `lib.data` strives to  minimise side effects, no proxies are returned in such cases, but a  `null` value is returned.

```kotlin
var collectionId: Long = -1
transaction.executeWithoutResult {
    val collection = collectionRepo.persist(WorkCollection())
    collectionId = collection.id
    val work1 = workRepo.persist(Work(collectionId, "Hey!"))
    val work2 = workRepo.persist(Work(collectionId, "Moin!"))
}
val collection = collectionRepo.selectById(collectionId)
// Works will be null
println(collection.works)
```

To load a lazy relation in `lib.data`, it must be explicitly mentioned  in a *data selection*. Each select method takes an additional optional  parameter of the `DataSelection` interface, which is a graph listing all the relations to be eagerly loaded for the query.

> 🪧 `lib.data` internally uses the [Jakarta Persistence Entity Graph](https://docs.jboss.org/hibernate/orm/6.1/userguide/html_single/Hibernate_User_Guide.html#fetching-strategies-dynamic-fetching-entity-graph), specifically a load graph, in combination  with the Criteria API to achieve this functionality. Each data selection is converted into a load graph and passed as a query hint.

On default, all select methods default to `EmptySelection` which loads no relations. `lib.data` also features following `DataSelection` implementations:

* `SimpleSelection`: Graph of nested sets where relations to be loaded are marked with their property name

* `FullNonRecursiveSelection`: Tries to create a data selection that loads are relations until a recursive cycle is detected

  > ⚠️ This type of selection has additional processing overhead and can lead to incomplete results as cycle detection is quite primitive.

* `DgsGraphQLSelection`:  `DataSelection` representation of `DataFetchingFieldSelectionSet` from Java GraphQL/DGS 

  > 🪧 Easily convertible via `dfe: DataFetchingEnvironment -> dfe.selectionSet.toDataSelection()`.

To load the `works` relation from the previous example, one can use a `SimpleSelection` like the following:

```kotlin
var collectionId: Long = -1
transaction.executeWithoutResult {
    val collection = collectionRepo.persist(WorkCollection())
    collectionId = collection.id
    val work1 = workRepo.persist(Work(collectionId, "Hey!"))
    val work2 = workRepo.persist(Work(collectionId, "Moin!"))
}
val selection = SimpleSelection(setOf(SimpleSelection.FieldNode(WorkCollection::works.name)))
val collection = collectionRepo.selectById(collectionId, selection)
// Works will be loaded
println(collection.works)
```

### Transactions

Most JPA implementations such as Hibernate provide Automatic Dirty checking whereby changes to a managed entity are automatically saved to the database when the session is flushed or the transaction is committed. 

In `lib.data`, all entities are automatically detached after persistence. One reason for this is that the Criteria API bypasses the persistence context for update or delete operations, leaving managed entities in an inconsistent state. Also, queries to the Criteria API can reuse the entity cache, which can result in missing relationships that were explicitly marked in the load graph converted from a data selection. In summary, this means that in `lib.data` every entity is free of side effects; every operation must be performed explicitly. This should not be an obstacle, as `lib.data` recommends, in the spirit of immutable design, to perform operations with updated copies of an existing entities.

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

In `lib.data` relations should be modelled explicitly therefore it is not advised to propagate (cascade) operations to related entities. In JPA one can persist an entity with included relation entities in one step, in `lib.data` each relation is inserted on its own via its own repository.

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
) : DataEntity<Foo> {
    ...
}

@Entity
@Table(name = "bars")
data class Bar(
    @Id
    @GeneratedValue
    val id: Long = -1,
    @ManyToMany(mappedBy="bars", fetch = FetchType.LAZY, cascade = [CascadeType.DETACH])
    val foos: Set<Foo>? = null
) : DataEntity<Bar> {
    ...
}

data class FooBarId(
    val foo: Long,
    val bar: Long
) : Serializable { constructor(): this(-1, -1) }

@Entity
@IdClass(FooBarId::class)
@Table(name = "foo_bar_relations")
data class FooBarRelation(
    @Id val foo: Long,
    @Id val bar: Long,
) : DataEntity<FooBarRelation> { override fun clone() = copy() }
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
    fooBarRepo.persist(FooBarRelation(foo1Id, barId))
}

var bar = barRepo.selectById(barId, selection)
assertNotNull(bar)
assertEquals(foo1Id, bar.foos?.first()?.id)
var foo1 = fooRepo.selectById(foo1Id, selection)
assertNotNull(foo1)
assertEquals(barId, foo1.bars?.first()?.id)
```

### Advanced Queries

In the previous examples, only simple, predefined operations were shown. It has also been mentioned that `lib.data` is built on top of the Criteria API, but no Criteria Queries have been shown so far.

Repository operations that provide extensibility of Criteria Queries contain a parameter `dsl` that represents a lambda. User-defined queries can be inserted into these lambdas.

```kotlin
open fun select(
    selection: DataSelection = EmptySelection(),
    dsl: CriteriaQueryDsl<*>.() -> Unit = {}
): List<T> 
```

```kotlin
open fun delete(dsl: CriteriaDeleteQueryDsl.() -> Unit): DeletedRowsCount
```

> ⚠️ When the `dsl` lambda of `delete` is empty (e.g. no `WHERE` clause) *NO* operation will be executed. To delete all entities of a table use `deleteAll` instead. 

The `dsl` lambdas do not use the native Criteria Query API, but the [Kotlin JDSL](https://github.com/line/kotlin-jdsl) abstraction is used instead. It allows the JPA Criteria API to be written more easily with Kotlin. [Development and readability is often improved in comparison of calling the native Criteria API, which is not optimised for Kotlin](https://engineering.linecorp.com/en/blog/kotlinjdsl-jpa-criteria-api-with-kotlin).

The following example shows how to query with a `WHERE` clause referencing an entity from a  `JOINED` table.

```kotlin
var collection1Id: Long = -1
var collection2Id: Long = -1
var work1Id: Long = -1
var work2Id: Long = -1

transaction.executeWithoutResult {
    val collection1 = collectionRepo.persist(WorkCollection())
    collection1Id = collection1.id
    val collection2 = collectionRepo.persist(WorkCollection())
    collection2Id = collection2.id
    val work1 = workRepo.persist(Work(collection1Id, "Hey!"))
    work1Id = work1.id
    val work2 = workRepo.persist(Work(collection2Id, "Moin!"))
    work2Id = work2.id
}

// Query `WorkCollection` where `works` includes entity with id `work2Id`
val collectionWithWork2 = collectionRepo.select(FullNonRecursiveSelection()) {
    join(entity(Work::class), on(column(WorkCollection::id).equal(column(Work::worksKey))))
    where(column(Work::id).equal(work2Id))
    limit(1)
}.firstOrNull()

assertNotNull(collectionWithWork2)
assertEquals(collection2Id, collectionWithWork2.id)
assertTrue { collectionWithWork2.works?.first()?.id == work2Id }
```

The following examples show how to delete a specific relation from a table.

```kotlin
var barId: Long = -1
var foo1Id: Long = -1

transaction.executeWithoutResult {
    val bar = barRepo.persist(Bar())
    barId = bar.id
    val foo1 = fooRepo.persist(Foo())
    foo1Id = foo1.id
}

// Establish relation
transaction.executeWithoutResult {
    fooBarRepo.persist(FooBarRelation(foo1Id, barId))
}

// Delete relation
transaction.executeWithoutResult {
    fooBarRepo.delete {
        where(column(FooBarRelation::bar).equal(barId))
    }
}
```

The Criteria API offers even more possibilities. It is recommended to  work through the [Kotlin JDSL Quick Start](https://github.com/line/kotlin-jdsl) and learn all the different features.

## 🧪 Incubating Features

### SkipOver API

If there is a mismatch between the GraphQL schema and the JPA model,  especially for relation tables that are not present in GraphQL, automatic data selection may fail.

Suppose there is a `ClubMember` relationship that models an `m:n`  relationship with properties `member` of type `User` & `club` of  type `Club`. Each user has a `memberOf` property that points to the  `ClubMember` table.

However, the GraphQL schema has no representation for `ClubMember`, the `memberOf` property in the GraphQL world only specifies a list of  `Clubs`. The automatic data selection would not load the clubs of `ClumbMember` in the relational word.

To fix this, one can add *skips overs*, which has the effect of continuing a certain GraphQL selection in a subrelation. The following example fixes the problem outlined earlier.

```kotlin
selection.skipOverEntries.addSkipOver(
    SkipOverOnce(
        field = User::memberOf.name,
        targetField = ClubMember::club.name,
        type = User::class.java
    )
)
```

> ⚠️ API is not stabilised yet and can change in the future.

A skip over can be of type `SkipOverOnce` & `SkipOverAll`. Prefer the latter one when a graph contains multiple problematic relations at different depths as `SkipOverOnce` will only skip the first occurrence.



---

## Remarks

### About `@OneToOne`, `@ManyToOne` and `FetchType.LAZY`

With `lib.data` pre Hibernate 6.X lazy loading for single entities was working as expected. But since migrating to Hibernate 6.X, it seems that this feature is not supported and/or working as intended.

The [Hibernate 6.1 documentation](https://docs.jboss.org/hibernate/orm/6.1/userguide/html_single/Hibernate_User_Guide.html#fetching-strategies) specifically notes to mark all associations lazy and to use dynamic fetching strategies for eagerness.

The [bytecode enhancement section](https://docs.jboss.org/hibernate/orm/6.1/userguide/html_single/Hibernate_User_Guide.html#BytecodeEnhancement) mentions then following:

> As a hopefully temporary legacy hold-over, it is currently required that all lazy singular associations (many-to-one and one-to-one) also  include `@LazyToOne(LazyToOneOption.NO_PROXY)`. The plan is to relax that requirement later.

However, [the bytecode tooling enhacement runtime documentation](https://docs.jboss.org/hibernate/orm/6.2/userguide/html_single/Hibernate_User_Guide.html#tooling-enhancement-runtime) states following:

> ```
> hibernate.enhancer.enableLazyInitialization
> ```
>
> Whether to enhance the model for lazy loading at the attribute level.  This allows even basic types to be fetched lazily.  It also allows definition of fetch groups (`LazyGroup`). This setting is deprecated for removal without a replacement.

Therefore, the conclusion for now is to mark all relations with `FetchType.LAZY` until further investigation or updates in the documentation / Hibernate issue tracker, with the ulterior motive that individual relations will continue to be loaded eagerly. However, this could change again in the future with a new Hibernate version.

Again, this should not lead to significant performance downgrades and is in fact the standard behaviour for JPA:

> [... the Jakarta Persistence specification which defines that all one-to-one  and many-to-one associations should be eagerly fetched by default.](https://docs.jboss.org/hibernate/orm/6.1/userguide/html_single/Hibernate_User_Guide.html#fetching-strategies)