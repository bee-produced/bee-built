package com.beeproduced.service.organisation.person.feature

import com.beeproduced.data.selection.DataSelection
import com.beeproduced.result.AppResult
import com.beeproduced.result.errors.BadRequestError
import com.beeproduced.result.jpa.transactional.TransactionalResult
import com.beeproduced.service.organisation.address.feature.AddressService
import com.beeproduced.service.organisation.entities.Address
import com.beeproduced.service.organisation.entities.Person
import com.beeproduced.service.organisation.entities.PersonId
import com.beeproduced.service.organisation.entities.input.CreateAddressInput
import com.beeproduced.service.organisation.entities.input.CreatePersonInput
import com.beeproduced.utils.logFor
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.map
import org.springframework.stereotype.Service
import java.util.*

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-09-27
 */
@Service
class PersonService(
    private val repository: PersonRepository,
    private val addressService: AddressService
) {
    private val logger = logFor<PersonRepository>()

    @TransactionalResult(
        "organisationTransactionManager",
        exceptionDescription = "Could not create person",
    )
    fun create(
        create: CreatePersonInput,
        selection: DataSelection
    ): AppResult<Person> {
        logger.debug("create({}, {})", create, selection)
        return createAddress(create.address).map { address ->
            val person = Person(
                id = UUID.randomUUID(),
                firstname = create.firstname,
                lastname = create.lastname,
                memberOf = null,
                addressId = address?.id,
                address = address
            )
            repository.persist(person)
        }
    }

    private fun createAddress(createAddressInput: CreateAddressInput?): AppResult<Address?> {
        if (createAddressInput == null) return Ok(null)
        return addressService.create(createAddressInput)
    }

    @TransactionalResult(
        "organisationTransactionManager",
        exceptionDescription = "Could not fetch all persons",
        readOnly = true
    )
    fun getAll(selection: DataSelection): AppResult<List<Person>> {
        logger.debug("getAll({})", selection)
        return Ok(repository.select(selection))
    }

    @TransactionalResult(
        "organisationTransactionManager",
        exceptionDescription = "Could not fetch all persons",
        readOnly = true
    )
    fun getByIds(ids: Collection<PersonId>, selection: DataSelection): AppResult<List<Person>> {
        logger.debug("getByIds({}, {})", ids, selection)
        val uniqueIds = ids.toSet()
        val persons = repository.selectByIds(uniqueIds)
        if (persons.count() == uniqueIds.count()) return Ok(persons)
        return Err(BadRequestError("Could not find all persons"))
    }

    @TransactionalResult(
        "organisationTransactionManager",
        exceptionDescription = "Could not check persons",
        readOnly = true
    )
    fun exists(ids: Collection<PersonId>): AppResult<Unit> {
        return if (repository.existsAll(ids)) Ok(Unit)
        else Err(BadRequestError("Could not find some persons"))
    }
}