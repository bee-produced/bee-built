package com.beeproduced.service.organisation.entities

import com.beeproduced.bee.persistent.jpa.entity.DataEntity
import jakarta.persistence.*
import java.io.Serializable

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-09-27
 */

@Embeddable
data class CompanyMemberId(
    @Column(name = "company_id")
    val companyId: CompanyId,
    @Column(name = "person_id")
    val personId: PersonId
) : Serializable

@Entity
@IdClass(CompanyMemberId::class)
@Table(name = "company_members")
data class CompanyMember(
    @Id
    @Column(name = "company_id")
    val companyId: CompanyId,
    @Id
    @Column(name = "person_id")
    val personId: PersonId,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", referencedColumnName = "id", insertable = false, updatable = false)
    val company: Company? = null,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", referencedColumnName = "id", insertable = false, updatable = false)
    val person: Person? = null,
) : DataEntity<CompanyMember> {
    override fun clone(): CompanyMember = this.copy()
}