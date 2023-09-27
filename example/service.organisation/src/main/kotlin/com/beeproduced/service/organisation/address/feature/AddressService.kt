package com.beeproduced.service.organisation.address.feature

import com.beeproduced.result.AppResult
import com.beeproduced.result.jpa.transactional.TransactionalResult
import com.beeproduced.service.organisation.entities.Address
import com.beeproduced.service.organisation.entities.input.CreateAddressInput
import com.beeproduced.utils.logFor
import com.github.michaelbull.result.Ok
import org.springframework.stereotype.Service
import java.util.*

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-09-27
 */
@Service
class AddressService(
    private val repository: AddressRepository
) {
    private val logger = logFor<AddressRepository>()

    @TransactionalResult(
        "organisationTransactionManager",
        exceptionDescription = "Could not create address",
        readOnly = true
    )
    fun create(create: CreateAddressInput): AppResult<Address> {
        logger.debug("create({})", create)
        val address = Address(
            id = UUID.randomUUID(),
            addressLine1 = create.addressLine1,
            addressLine2 = create.addressLine2,
            zipCode = create.zipCode,
            city = create.city
        )
        return Ok(repository.persist(address))
    }
}