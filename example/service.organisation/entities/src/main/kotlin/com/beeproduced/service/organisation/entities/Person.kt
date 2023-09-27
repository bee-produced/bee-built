package com.beeproduced.service.organisation.entities

import com.beeproduced.data.jpa.entity.DataEntity
import jakarta.persistence.*
import java.util.UUID

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-09-27
 */

typealias PersonId = UUID

@Entity
@Table(name = "persons")
data class Person(
    @Id
    @GeneratedValue
    val id: PersonId,
    val firstname: String,
    val lastname: String,
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "person")
    val memberOf: Set<CompanyMember>?,
    @Column(name = "address_id")
    val addressId: AddressId?,
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id", referencedColumnName = "id", insertable = false, updatable = false)
    val address: Address?
) : DataEntity<Person> {
    override fun clone(): Person = this.copy()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Person) return false

        if (id != other.id) return false
        if (firstname != other.firstname) return false
        if (lastname != other.lastname) return false
        if (addressId != other.addressId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + firstname.hashCode()
        result = 31 * result + lastname.hashCode()
        result = 31 * result + (addressId?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "Person(id=$id, firstname='$firstname', lastname='$lastname', memberOf=${memberOf?.map { it.companyId }}, addressId=$addressId, address=$address)"
    }
}