package com.beeproduced.lib.data.one.to.many

import com.beeproduced.data.jpa.repository.BaseDataRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import jakarta.persistence.EntityManager

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-02-13
 */

@Component
class WorkRepository(
    @Qualifier("orderEntityManager") em: EntityManager
) : BaseDataRepository<Work, WorkId>(em)

@Component
class WorkCollectionRepository(
    @Qualifier("orderEntityManager") em: EntityManager
) : BaseDataRepository<WorkCollection, Long>(em)
