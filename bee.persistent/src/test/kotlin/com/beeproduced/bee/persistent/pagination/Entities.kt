package com.beeproduced.bee.persistent.pagination

import com.beeproduced.bee.persistent.jpa.entity.DataEntity
import jakarta.persistence.*
import java.io.Serializable
import java.time.Instant
import java.util.*

@Entity
@Table(name = "paginated_foos")
data class PaginatedFoo(
  @Id @GeneratedValue val id: Long = 0,
  val createdBy: String,
  @Column(columnDefinition = "TIMESTAMP(6)") val createdOn: Instant,
) : DataEntity<PaginatedFoo> {
  override fun clone(): PaginatedFoo = this.copy()
}

@Embeddable data class PaginatedBarId(val idA: UUID, val idB: UUID) : Serializable

@Entity
@IdClass(PaginatedBarId::class)
@Table(name = "paginated_bars")
data class PaginatedBar(
  @Id val idA: UUID,
  @Id val idB: UUID,
  val createdBy: String,
  @Column(columnDefinition = "TIMESTAMP(6)") val createdOn: Instant,
) : DataEntity<PaginatedBar> {
  override fun clone(): PaginatedBar = this.copy()
}

@Entity
@Table(name = "paginated_foxtrots")
data class PaginatedFoxtrot(
  @Id @GeneratedValue val id: Long = 0,
  val createdBy: String,
  @Column(columnDefinition = "TIMESTAMP(6)") val createdOn: Instant,
  @OneToMany(mappedBy = "foxtrot", fetch = FetchType.LAZY) val infos: Set<FoxtrotInfo>? = null,
) : DataEntity<PaginatedFoxtrot> {
  override fun clone(): PaginatedFoxtrot = this.copy()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is PaginatedFoxtrot) return false

    if (id != other.id) return false

    return true
  }

  override fun hashCode(): Int {
    return id.hashCode()
  }
}

@Entity
@Table(name = "foxtrot_infos")
data class FoxtrotInfo(
  @Id @GeneratedValue val id: Long = 0,
  val customCreatedBy: String,
  val foxtrotId: Long,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
    name = "foxtrotId",
    referencedColumnName = "id",
    insertable = false,
    updatable = false,
  )
  val foxtrot: PaginatedFoxtrot? = null,
) : DataEntity<FoxtrotInfo> {
  override fun clone(): FoxtrotInfo = this.copy()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is FoxtrotInfo) return false

    if (id != other.id) return false

    return true
  }

  override fun hashCode(): Int {
    return id.hashCode()
  }
}
