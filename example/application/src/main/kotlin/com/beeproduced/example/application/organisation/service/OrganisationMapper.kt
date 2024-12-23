package com.beeproduced.example.application.organisation.service

import com.beeproduced.service.organisation.entities.Address
import com.beeproduced.service.organisation.entities.Company
import com.beeproduced.service.organisation.entities.CompanyMember
import com.beeproduced.service.organisation.entities.Person
import com.beeproduced.utils.CycleAvoidingMappingContext
import org.mapstruct.Builder
import org.mapstruct.Context
import org.mapstruct.Mapper
import org.mapstruct.Mapping

/**
 * @author Kacper Urbaniec
 * @version 2023-09-26
 */

// Using abstract class to allow default implementation
// https://github.com/mapstruct/mapstruct/issues/1577
@Mapper(builder = Builder(disableBuilder = true))
abstract class OrganisationMapper {

  @Mapping(
    target = "memberOf",
    expression =
      "java(toCompanyDtos(companyMemberToCompanies(entity.getMemberOf(), context), context))",
  )
  abstract fun toPersonDto(entity: Person, @Context context: CycleAvoidingMappingContext): PersonDto

  fun toPersonDto(entity: Person): PersonDto = toPersonDto(entity, CycleAvoidingMappingContext())

  fun toPersonDtos(entities: Collection<Person>): List<PersonDto> {
    return entities.map { toPersonDto(it, CycleAvoidingMappingContext()) }
  }

  fun toPersonDtos(
    entities: Collection<Person>?,
    context: CycleAvoidingMappingContext,
  ): List<PersonDto> {
    return entities?.map { toPersonDto(it, context) } ?: emptyList()
  }

  @Mapping(
    target = "employees",
    expression =
      "java(toPersonDtos(companyMemberToPerson(entity.getEmployees(), context), context))",
  )
  abstract fun toCompanyDto(
    entity: Company,
    @Context context: CycleAvoidingMappingContext,
  ): CompanyDto

  fun toCompanyDto(entity: Company) = toCompanyDto(entity, CycleAvoidingMappingContext())

  fun toCompanyDtos(entities: Collection<Company>): List<CompanyDto> {
    return entities.map { toCompanyDto(it, CycleAvoidingMappingContext()) }
  }

  fun toCompanyDtos(
    entities: Collection<Company>?,
    @Context context: CycleAvoidingMappingContext,
  ): List<CompanyDto> {
    return entities?.map { toCompanyDto(it, context) } ?: emptyList()
  }

  fun companyMemberToCompanies(
    entity: Collection<CompanyMember>?,
    context: CycleAvoidingMappingContext,
  ): List<Company> {
    if (entity == null) return emptyList()
    if (
      entity.any { context.hasVisited(CompanyMember::class.java, Pair(it.companyId, it.personId)) }
    )
      return emptyList()
    for (e in entity) context.addVisited(CompanyMember::class.java, Pair(e.companyId, e.personId))
    return entity.mapNotNull { it.company }
  }

  fun companyMemberToPerson(
    entity: Collection<CompanyMember>?,
    context: CycleAvoidingMappingContext,
  ): List<Person> {
    if (entity == null) return emptyList()
    if (
      entity.any { context.hasVisited(CompanyMember::class.java, Pair(it.personId, it.companyId)) }
    )
      return emptyList()
    for (e in entity) context.addVisited(CompanyMember::class.java, Pair(e.personId, e.companyId))
    return entity.mapNotNull { it.person }
  }

  abstract fun toAddressDto(entity: Address): AddressDto
}
