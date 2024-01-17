package com.beeproduced.datasource.test.lazy

import com.beeproduced.bee.persistent.blaze.BeeBlazeRepository
import com.beeproduced.bee.persistent.blaze.annotations.BeeRepository
import com.beeproduced.bee.persistent.blaze.annotations.LazyField
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import java.io.Serializable
import java.util.UUID

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-16
 */

@Entity
data class LazyEntity(
    @Id
    @GeneratedValue
    val id: UUID = UUID.randomUUID(),
    val name: String,
    @LazyField
    val lazyName: String?,
    @Embedded
    val embedded: SomeEmbedded,
    @LazyField
    @Embedded
    val lazyEmbedded: LazyEmbedded?
)

@BeeRepository
interface LazyEntityRepository : BeeBlazeRepository<LazyEntity, UUID>

@Embeddable
data class SomeEmbedded(
    val someData: String,
    @LazyField
    val someLazyData: String?
) : Serializable

@Embeddable
data class LazyEmbedded(
    val lazyData: String,
    @LazyField
    val lazyLazyData: String?
) : Serializable