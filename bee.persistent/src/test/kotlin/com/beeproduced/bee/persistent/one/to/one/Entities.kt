package com.beeproduced.bee.persistent.one.to.one

import com.beeproduced.bee.persistent.jpa.entity.DataEntity
import jakarta.persistence.*
import org.hibernate.annotations.LazyToOne
import org.hibernate.annotations.LazyToOneOption

/**
 * @author Kacper Urbaniec
 * @version 2023-02-14
 */
@Entity
@Table(name = "roots")
data class Root(
  @Id @GeneratedValue val id: Long = -2,
  @LazyToOne(LazyToOneOption.NO_PROXY)
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "branch_a", insertable = false, updatable = false)
  val branchA: Branch? = null,
  @Column(name = "branch_a")
  // https://stackoverflow.com/a/44539145/12347616
  val branchAKey: Long? = null,
  @LazyToOne(LazyToOneOption.NO_PROXY)
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "branch_b", insertable = false, updatable = false)
  val branchB: Branch? = null,
  @Column(name = "branch_b") val branchBKey: Long? = null,
) : DataEntity<Root> {
  override fun clone(): Root = this.copy()
}

@Entity
@Table(name = "branches")
data class Branch(
  @Id @GeneratedValue val id: Long = -1,
  @LazyToOne(LazyToOneOption.NO_PROXY)
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "branch_a", insertable = false, updatable = false)
  val branchA: Branch? = null,
  @Column(name = "branch_a") val branchAKey: Long? = null,
  @LazyToOne(LazyToOneOption.NO_PROXY)
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "branch_b", insertable = false, updatable = false)
  val branchB: Branch? = null,
  @Column(name = "branch_b") val branchBKey: Long? = null,
) : DataEntity<Branch> {
  override fun toString(): String {
    return "$id"
  }

  override fun clone(): Branch = this.copy()
}
