package com.beeproduced.datasource.test.persist

import com.beeproduced.bee.persistent.blaze.BeeBlazeRepository
import com.beeproduced.bee.persistent.blaze.annotations.BeeRepository
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import java.util.*

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-15
 */

@Entity
data class GeneratedObjectId(
    @GeneratedValue
    @Id
    val id: UUID = UUID.randomUUID()
)

@BeeRepository
interface GeneratedObjectIdRepository : BeeBlazeRepository<GeneratedObjectId, UUID>

@Entity
data class GeneratedPrimitiveId(
    @GeneratedValue
    @Id
    val id: Long = -2
)

@BeeRepository
interface GeneratedPrimitiveIdRepository : BeeBlazeRepository<GeneratedPrimitiveId, Long>

@JvmInline
value class SomeInlineId(val id: Long)

@Entity
data class GeneratedInlineId(
    @GeneratedValue
    @Id
    val id: SomeInlineId
)

@BeeRepository
interface GeneratedInlineIdRepository : BeeBlazeRepository<GeneratedInlineId, SomeInlineId>