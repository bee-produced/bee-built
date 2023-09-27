package com.beeproduced.service.organisation.address.feature

import com.beeproduced.data.jpa.repository.BaseDataRepository
import com.beeproduced.service.organisation.entities.Address
import com.beeproduced.service.organisation.entities.AddressId
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
class AddressRepository(
    @Qualifier("organisationEntityManager") em: EntityManager
) : BaseDataRepository<Address, AddressId>(em)