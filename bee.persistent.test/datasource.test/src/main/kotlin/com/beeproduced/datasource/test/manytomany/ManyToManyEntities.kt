package com.beeproduced.datasource.test.manytomany

import com.beeproduced.bee.persistent.blaze.BeeBlazeRepository
import com.beeproduced.bee.persistent.blaze.annotations.BeeRepository
import jakarta.persistence.*
import java.io.Serializable

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-15
 */

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
    // Use sets instead of lists to allow fetching multiple collections eagerly at once
    // to omit "MultipleBagFetchException - cannot simultaneously fetch multiple bags"
    // https://stackoverflow.com/a/4335514/12347616
    val bars: Set<Bar>? = null
)


@BeeRepository
interface FooRepository : BeeBlazeRepository<Foo, Long>

@Entity
@Table(name = "bars")
data class Bar(
    @Id
    @GeneratedValue
    val id: Long = -1,
    @ManyToMany(mappedBy="bars", fetch = FetchType.LAZY, cascade = [CascadeType.DETACH])
    val foos: Set<Foo>? = null
)

@BeeRepository
interface BarRepository : BeeBlazeRepository<Bar, Long>

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

@BeeRepository
interface FooBarRepository : BeeBlazeRepository<FooBarRelation, FooBarId>