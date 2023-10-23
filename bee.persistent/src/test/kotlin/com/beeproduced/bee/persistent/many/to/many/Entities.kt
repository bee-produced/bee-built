package com.beeproduced.bee.persistent.many.to.many

import com.beeproduced.bee.persistent.jpa.entity.DataEntity
import java.io.Serializable
import jakarta.persistence.*

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-02-15
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
) : DataEntity<Foo> {
    override fun clone() = copy()

    override fun toString(): String {
        val bars = bars?.let { it.map { it.id }}
        return "Foo(id=$id, bars=$bars)"
    }

    // Override hashcode (and also equals) that EXCLUDES collections
    // to omit "org.hibernate.HibernateException: collection was evicted"
    // https://stackoverflow.com/a/65176911/12347616

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Foo

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

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

    override fun clone() = copy()
    override fun toString(): String {
        val foos = foos?.let { it.map { it.id }}
        return "Bar(id=$id, foos=$foos)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Bar

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }


}


data class FooBarId(
    val foo: Long,
    val bar: Long
) : Serializable {
    // Empty constructor is needed for hibernate
    // TODO: no-args constructor JPA
    constructor(): this(-1, -1)
}


@Entity
@IdClass(FooBarId::class)
@Table(name = "foo_bar_relations")
data class FooBarRelation(
    @Id val foo: Long,
    @Id val bar: Long,
) : DataEntity<FooBarRelation> {
    override fun clone() = copy()
}
