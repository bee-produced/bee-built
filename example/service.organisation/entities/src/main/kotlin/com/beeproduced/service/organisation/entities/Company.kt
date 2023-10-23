package com.beeproduced.service.organisation.entities

import com.beeproduced.bee.persistent.jpa.entity.DataEntity
import jakarta.persistence.*
import java.util.UUID

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-09-27
 */

typealias CompanyId = UUID

@Entity
@Table(name = "companies")
data class Company(
    @Id
    @GeneratedValue
    val id: CompanyId,
    val name: String,
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "company")
    val employees: Set<CompanyMember>?,
    @Column(name = "address_id")
    val addressId: AddressId?,
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id", referencedColumnName = "id", insertable = false, updatable = false)
    val address: Address?
) : DataEntity<Company> {
    override fun clone(): Company = this.copy()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Company) return false

        if (id != other.id) return false
        if (name != other.name) return false
        if (addressId != other.addressId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + (addressId?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "Company(id=$id, name='$name', employees=${employees?.map { it.personId }}, address=$addressId, address=$address)"
    }
}