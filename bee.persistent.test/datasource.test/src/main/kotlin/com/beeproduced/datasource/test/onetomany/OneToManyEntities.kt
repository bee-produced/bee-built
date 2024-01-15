package com.beeproduced.datasource.test.onetomany

import com.beeproduced.bee.persistent.blaze.BeeBlazeRepository
import com.beeproduced.bee.persistent.blaze.annotations.BeeRepository
import jakarta.persistence.*
import org.hibernate.annotations.SQLInsert
import java.io.Serializable

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-15
 */

data class WorkId(
    @Column(name = "work_id")
    val id: Long = -1,
    @Column(name = "works_id")
    val worksKey: Long = -1
) : Serializable

@Entity
@Table(name = "works")
data class Work(
    @EmbeddedId
    val id: WorkId,
    val txt: String,
    // Update & reading `works` should be done via `worksKey`
    // But JPA/Hibernate needs this entity to create a foreign key
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "works_id", referencedColumnName = "id", insertable = false, updatable = false)
    val workCollection: WorkCollection? = null
)

@BeeRepository
interface WorkRepository: BeeBlazeRepository<Work, WorkId>

@Entity
@Table(name = "work_collections")
data class WorkCollection(
    @Id
    @GeneratedValue
    val id: Long = -1,
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "workCollection")
    val works: Set<Work>? = null
)

@BeeRepository
interface WorkCollectionRepository: BeeBlazeRepository<WorkCollection, Long>