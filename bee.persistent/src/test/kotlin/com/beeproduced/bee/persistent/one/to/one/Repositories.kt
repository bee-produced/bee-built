package com.beeproduced.bee.persistent.one.to.one

import com.beeproduced.bee.persistent.jpa.repository.BaseDataRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import jakarta.persistence.*

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-02-14
 */

@Component
class RootRepository(
    @Qualifier("orderEntityManager") em: EntityManager,
) : BaseDataRepository<Root, Long>(em)

@Component
class BranchRepository(
    @Qualifier("orderEntityManager") em: EntityManager,
) : BaseDataRepository<Branch, Long>(em)
