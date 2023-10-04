package com.beeproduced.service.organisation.company.feature

import com.beeproduced.data.jpa.repository.BaseDataRepository
import com.beeproduced.service.organisation.entities.Company
import com.beeproduced.service.organisation.entities.CompanyId
import com.beeproduced.service.organisation.entities.CompanyMember
import com.beeproduced.service.organisation.entities.CompanyMemberId
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-09-27
 */
@Component
class CompanyRepository(
    @Qualifier("organisationEntityManager") em: EntityManager
) : BaseDataRepository<Company, CompanyId>(em)

@Component
class CompanyMemberRepository(
    @Qualifier("organisationEntityManager") em: EntityManager
) : BaseDataRepository<CompanyMember, CompanyMemberId>(em)