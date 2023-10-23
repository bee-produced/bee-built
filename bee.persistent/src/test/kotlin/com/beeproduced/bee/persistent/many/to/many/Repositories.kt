package com.beeproduced.bee.persistent.many.to.many

import com.beeproduced.bee.persistent.jpa.repository.BaseDataRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import jakarta.persistence.EntityManager

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-02-15
 */
@Component
class FooRepository(
    @Qualifier("orderEntityManager") em: EntityManager
) : BaseDataRepository<Foo, Long>(em)

@Component
class BarRepository(
    @Qualifier("orderEntityManager") em: EntityManager
) : BaseDataRepository<Bar, Long>(em)

@Component
class FooBarRelationRepository(
    @Qualifier("orderEntityManager") em: EntityManager
) : BaseDataRepository<FooBarRelation, FooBarId>(em)

