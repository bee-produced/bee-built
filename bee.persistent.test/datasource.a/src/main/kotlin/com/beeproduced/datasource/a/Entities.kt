package com.beeproduced.datasource.a

import jakarta.persistence.*
import java.io.Serializable
import java.util.*

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-12-14
 */

@Entity
@Table(name = "songs")
data class Song(
    @Id
    @GeneratedValue
    val id: UUID,
    val name: String,
    @Column(name = "interpret_id")
    val interpretId: UUID,
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interpret_id", insertable = false, updatable = false)
    val interpret: Person?,
    @Column(name = "producer_id")
    val producerId: UUID,
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producer_id", insertable = false, updatable = false)
    val producer: Company?,
) {
    @Transient
    var altName: String = ""
    @Column(name = "secret_info")
    private val secretInfo = "secret!"
}

@Entity
@Table(name = "persons")
data class Person(
    @Id
    @GeneratedValue
    val id: UUID,
    val firstname: String,
    val lastname: String,
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "person")
    val companies: Set<CompanyPerson>? = null
)

@Entity
@Table(name = "companies")
data class Company(
    @Id
    @GeneratedValue
    val id: UUID,
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "company")
    val employees: Set<CompanyPerson>?
)

@Embeddable
data class CompanyPersonId(
    @Column(name = "company_id")
    val companyId: UUID,
    @Column(name = "person_id")
    val personId: UUID
) : Serializable

@Entity
@Table(name = "company_persons")
data class CompanyPerson(
    @EmbeddedId
    val id: CompanyPersonId,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", referencedColumnName = "id", insertable = false, updatable = false)
    val company: Company?,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", referencedColumnName = "id", insertable = false, updatable = false)
    val person: Person?,
)

data class FooBar(
    val foo: String,
    val bar: String
)

@Converter
class FooBarConverter : AttributeConverter<FooBar, String> {
    override fun convertToDatabaseColumn(attribute: FooBar?): String? {
        return if (attribute == null) null
        else "${attribute.foo}$$${attribute.bar}"
    }

    override fun convertToEntityAttribute(dbData: String?): FooBar? {
        if (dbData.isNullOrEmpty()) return null
        val data = dbData.split("$$")
        if (data.count() < 2) return null
        return FooBar(data[0], data[1])
    }
}

@JvmInline
value class Foxtrot(private val s: String)

// https://youtrack.jetbrains.com/issue/KT-50518/Boxing-Unboxing-methods-for-JvmInline-value-classes-should-be-public-accessible
fun unwrapInline(v: Any): Any = v.javaClass.getMethod("unbox-impl").invoke(v)

@Entity
data class WeirdClass(
    @Id
    val id: UUID,
    @Convert(converter = FooBarConverter::class)
    val fooBar: FooBar,
    val foxtrot: Foxtrot
)