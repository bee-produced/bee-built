package com.beeproduced.bee.persistent.one.to.many

import com.beeproduced.bee.persistent.jpa.repository.BaseDataRepository
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

/**
 * @author Kacper Urbaniec
 * @version 2023-02-13
 */
@Component
class WorkRepository(@Qualifier("orderEntityManager") em: EntityManager) :
  BaseDataRepository<Work, WorkId>(em)

@Component
class WorkCollectionRepository(@Qualifier("orderEntityManager") em: EntityManager) :
  BaseDataRepository<WorkCollection, Long>(em)
