package com.beeproduced.bee.persistent.one.to.many

import com.beeproduced.bee.persistent.jpa.entity.DataEntity
import java.io.Serializable
import jakarta.persistence.*

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-02-07
 */

data class WorkId(
    val id: Long = -1,
    val worksKey: Long = -1
) : Serializable

@Entity
@IdClass(WorkId::class)
@Table(name = "works")
data class Work(
    @Id
    @GeneratedValue
    val id: Long,
    @Id
    @Column(name = "works_id")
    val worksKey: Long,
    val txt: String,
    // Update & reading `works` should be done via `worksKey`
    // But JPA/Hibernate needs this entity to create a foreign key
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "works_id", referencedColumnName = "id", insertable = false, updatable = false)
    val works: WorkCollection? = null
) : DataEntity<Work> {

    constructor(works: Long, txt: String) : this(-1, works, txt)

    override fun clone(): Work = this.copy()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Work) return false

        if (id != other.id) return false
        if (worksKey != other.worksKey) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + worksKey.hashCode()
        return result
    }
}

@Entity
@Table(name = "work_collections")
data class WorkCollection(
    @Id
    @GeneratedValue
    val id: Long,
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "works")
    val works: Set<Work>?
) : DataEntity<WorkCollection> {
    constructor() : this(-1, null)

    override fun clone(): WorkCollection = this.copy()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WorkCollection) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
