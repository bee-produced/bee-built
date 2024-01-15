package com.beeproduced.datasource.a

import com.beeproduced.bee.persistent.blaze.BeeBlazeRepository
import com.beeproduced.bee.persistent.blaze.annotations.BeeRepository
import jakarta.persistence.*
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
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
    val id: UUID = UUID.randomUUID(),
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

@BeeRepository
interface SongRepository : BeeBlazeRepository<Song, UUID>

@Entity
@Table(name = "persons")
data class Person(
    @Id
    @GeneratedValue
    val id: UUID = UUID.randomUUID(),
    val firstname: String,
    val lastname: String,
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "person")
    val companies: Set<CompanyPerson>? = null,
    val addressId: UUID? = null,
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "addressId", insertable = false, updatable = false)
    val address: Address? = null
)

@BeeRepository
interface PersonRepository : BeeBlazeRepository<Person, UUID>

@Entity
@Table(name = "addresses")
data class Address(
    @Id
    @GeneratedValue
    val id: UUID = UUID.randomUUID(),
    val street: String
)

@BeeRepository
interface AddressRepository : BeeBlazeRepository<Address, UUID>

@Entity
@Table(name = "companies")
data class Company(
    @Id
    @GeneratedValue
    val id: UUID = UUID.randomUUID(),
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "company")
    val employees: Set<CompanyPerson>?
)

@BeeRepository
interface CompanyRepository : BeeBlazeRepository<Company, UUID>

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

@BeeRepository
interface CompanyPersonRepository : BeeBlazeRepository<CompanyPerson, CompanyPersonId>

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

@BeeRepository
interface WeirdClassRepository : BeeBlazeRepository<WeirdClass, UUID>


@Entity
data class Circular(
    @Id
    val id: UUID,
    val cId: UUID?,
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cId", referencedColumnName = "id", insertable = false, updatable = false)
    val circular: Circular? = null
)

@BeeRepository
interface CircularRepository : BeeBlazeRepository<Circular, UUID>


@Entity
data class SemiCircular1(
    @Id
    val id: UUID,
    val cId: UUID?,
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cId", referencedColumnName = "id", insertable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    val circular: SemiCircular2? = null
)

@Entity
data class SemiCircular2(
    @Id
    val id: UUID,
    val cId: UUID?,
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cId", referencedColumnName = "id", insertable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    val circular: SemiCircular1? = null
)

@BeeRepository
interface SemiCircular1Repository : BeeBlazeRepository<SemiCircular1, UUID>

