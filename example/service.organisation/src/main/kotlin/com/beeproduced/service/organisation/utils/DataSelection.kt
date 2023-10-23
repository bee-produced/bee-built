package com.beeproduced.service.organisation.utils

import com.beeproduced.bee.persistent.selection.DataSelection
import com.beeproduced.bee.persistent.selection.SkipOverAll
import com.beeproduced.service.organisation.entities.Company
import com.beeproduced.service.organisation.entities.CompanyMember
import com.beeproduced.service.organisation.entities.Person

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-09-27
 */

fun DataSelection.organisationAdapter(): DataSelection {
    skipOvers.add(
        SkipOverAll(
            field = Person::memberOf.name,
            targetField = CompanyMember::company.name,
            type = Person::class.java
        )
    )
    skipOvers.add(
        SkipOverAll(
            field = Company::employees.name,
            targetField = CompanyMember::person.name,
            type = Company::class.java
        )
    )
    return this
}