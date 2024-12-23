package com.beeproduced.service.organisation.company.feature

import com.beeproduced.bee.functional.extensions.com.github.michaelbull.result.andThenToPair
import com.beeproduced.bee.functional.persistent.transactional.TransactionalResult
import com.beeproduced.bee.functional.result.AppResult
import com.beeproduced.bee.functional.result.errors.BadRequestError
import com.beeproduced.bee.persistent.selection.DataSelection
import com.beeproduced.bee.persistent.selection.EmptySelection
import com.beeproduced.service.organisation.address.feature.AddressService
import com.beeproduced.service.organisation.entities.*
import com.beeproduced.service.organisation.entities.input.CreateAddressInput
import com.beeproduced.service.organisation.entities.input.CreateCompanyInput
import com.beeproduced.service.organisation.person.feature.PersonService
import com.beeproduced.utils.logFor
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.map
import java.util.*
import org.springframework.stereotype.Service

/**
 * @author Kacper Urbaniec
 * @version 2023-09-27
 */
@Service
class CompanyService(
  private val repository: CompanyRepository,
  private val memberRepository: CompanyMemberRepository,
  private val personService: PersonService,
  private val addressService: AddressService,
) {
  private val logger = logFor<CompanyService>()

  @TransactionalResult(
    "organisationTransactionManager",
    exceptionDescription = "Could not create company",
  )
  fun create(create: CreateCompanyInput, selection: DataSelection): AppResult<Company> {
    logger.debug("create({}, {})", create, selection)
    return createAddress(create.address)
      .map { address ->
        val company = Company(UUID.randomUUID(), create.name, null, address?.id, address)
        repository.persist(company)
      }
      .andThenToPair { getEmployees(create.employees) }
      .map { (company, employees) -> assignPersonsToCompany(company, employees, selection) }
  }

  private fun getEmployees(personIds: Collection<PersonId>?): AppResult<List<Person>?> {
    if (personIds == null) return Ok(null)
    return personService.getByIds(personIds, EmptySelection())
  }

  private fun createAddress(createAddressInput: CreateAddressInput?): AppResult<Address?> {
    if (createAddressInput == null) return Ok(null)
    return addressService.create(createAddressInput)
  }

  private fun assignPersonsToCompany(
    company: Company,
    employees: List<Person>?,
    selection: DataSelection,
  ): Company {
    if (employees == null) return company
    // Persist Person ↔ Company relation
    for (employee in employees) memberRepository.persist(CompanyMember(company.id, employee.id))
    // Check if data needs to be re-fetched with employees loaded
    if (selection.contains("**{employees}")) {
      val companyWithEmployees = repository.selectById(company.id, selection)
      return companyWithEmployees ?: company
    }
    return company
  }

  @TransactionalResult(
    "organisationTransactionManager",
    exceptionDescription = "Could not fetch all companies",
    readOnly = true,
  )
  fun getAll(selection: DataSelection): AppResult<List<Company>> {
    logger.debug("getAll({})", selection)
    return Ok(repository.select(selection))
  }

  @TransactionalResult(
    "organisationTransactionManager",
    exceptionDescription = "Could not fetch all companies",
    readOnly = true,
  )
  fun getByIds(ids: Collection<PersonId>, selection: DataSelection): AppResult<List<Company>> {
    logger.debug("getByIds({}, {})", ids, selection)
    val uniqueIds = ids.toSet()
    val companies = repository.selectByIds(uniqueIds, selection)
    if (companies.count() == uniqueIds.count()) return Ok(companies)
    return Err(BadRequestError("Could not find all persons"))
  }

  @TransactionalResult(
    "organisationTransactionManager",
    exceptionDescription = "Could not check companies",
    readOnly = true,
  )
  fun exists(ids: Collection<CompanyId>): AppResult<Unit> {
    return if (repository.existsAll(ids)) Ok(Unit)
    else Err(BadRequestError("Could not find some companies"))
  }
}
