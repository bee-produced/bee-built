package com.beeproduced.datasource.test.onetoone

import com.beeproduced.bee.persistent.blaze.BeeBlazeRepository
import com.beeproduced.bee.persistent.blaze.annotations.BeeRepository
import jakarta.persistence.*

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-14
 */

@Entity
@Table(name = "roots")
data class Root(
    @Id
    @GeneratedValue
    val id: Long = -2,
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_a", insertable = false, updatable = false)
    val branchA: Branch? = null,
    @Column(name = "branch_a")
    // https://stackoverflow.com/a/44539145/12347616
    val branchAKey: Long? = null,
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_b", insertable = false, updatable = false)
    val branchB: Branch? = null,
    @Column(name = "branch_b")
    val branchBKey: Long? = null
)

@BeeRepository
interface RootRepository : BeeBlazeRepository<Root, Long>

@Entity
@Table(name = "branches")
data class Branch(
    @Id
    @GeneratedValue
    val id: Long = -1,
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_a", insertable = false, updatable = false)
    val branchA: Branch? = null,
    @Column(name = "branch_a")
    val branchAKey: Long? = null,
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_b", insertable = false, updatable = false)
    val branchB: Branch? = null,
    @Column(name = "branch_b")
    val branchBKey: Long? = null
)

@BeeRepository
interface BranchRepository : BeeBlazeRepository<Branch, Long>