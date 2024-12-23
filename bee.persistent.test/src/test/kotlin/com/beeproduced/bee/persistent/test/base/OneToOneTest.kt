package com.beeproduced.bee.persistent.test.base

import com.beeproduced.bee.persistent.blaze.selection.BeeSelection
import com.beeproduced.bee.persistent.test.config.BaseTestConfig
import com.beeproduced.datasource.test.dsl.RootDSL
import com.beeproduced.datasource.test.onetoone.Branch
import com.beeproduced.datasource.test.onetoone.BranchRepository
import com.beeproduced.datasource.test.onetoone.Root
import com.beeproduced.datasource.test.onetoone.RootRepository
import jakarta.persistence.EntityManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

/**
 * @author Kacper Urbaniec
 * @version 2024-01-14
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [BaseTestConfig::class])
@TestPropertySource("classpath:application.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OneToOneTest(
  @Qualifier("testEM") val em: EntityManager,
  @Qualifier("testTM") transactionManager: PlatformTransactionManager,
  @Autowired val rootRepo: RootRepository,
  @Autowired val branchRepo: BranchRepository,
) {
  private val transaction = TransactionTemplate(transactionManager)

  @BeforeAll fun beforeAll() = clear()

  @AfterEach fun afterEach() = clear()

  fun clear() =
    transaction.executeWithoutResult {
      rootRepo.cbf.delete(em, Root::class.java).executeUpdate()
      branchRepo.cbf.delete(em, Branch::class.java).executeUpdate()
    }

  @Test
  fun `select with relations not loaded`() {
    var rootId: Long = -1
    var branchAId: Long
    var branchBId: Long
    var branchAAId: Long
    var branchABId: Long
    transaction.executeWithoutResult {
      val root = rootRepo.persist(Root())
      rootId = root.id
      val branchA = branchRepo.persist(Branch())
      branchAId = branchA.id
      branchBId = branchRepo.persist(Branch()).id
      branchAAId = branchRepo.persist(Branch()).id
      branchABId = branchRepo.persist(Branch()).id

      rootRepo.update(root.copy(branchAKey = branchAId, branchBKey = branchBId))
      branchRepo.update(branchA.copy(branchAKey = branchAAId, branchBKey = branchABId))
    }

    transaction.executeWithoutResult {
      val selection = BeeSelection.empty()
      // TODO: Replace with selectById
      val root = rootRepo.select(selection) { where(RootDSL.id.eq(rootId)) }.firstOrNull()

      assertNotNull(root)
      assertNull(root.branchA)
      assertNull(root.branchB)
    }
  }

  @Test
  fun `select with relations loaded`() {
    var rootId: Long = -1
    var branchAId: Long = -1
    var branchBId: Long = -1
    var branchAAId: Long = -1
    var branchABId: Long = -1
    transaction.executeWithoutResult {
      val root = rootRepo.persist(Root())
      rootId = root.id
      val branchA = branchRepo.persist(Branch())
      branchAId = branchA.id
      branchBId = branchRepo.persist(Branch()).id
      branchAAId = branchRepo.persist(Branch()).id
      branchABId = branchRepo.persist(Branch()).id

      rootRepo.update(root.copy(branchAKey = branchAId, branchBKey = branchBId))
      branchRepo.update(branchA.copy(branchAKey = branchAAId, branchBKey = branchABId))
    }

    transaction.executeWithoutResult {
      val selection =
        RootDSL.select {
          this.branchA {
            this.branchA {
              this.branchA {}
              this.branchB {}
            }
            this.branchB {
              this.branchA {}
              this.branchB {}
            }
          }
          this.branchB {
            this.branchA {
              this.branchA {}
              this.branchB {}
            }
            this.branchB {
              this.branchA {}
              this.branchB {}
            }
          }
        }

      // TODO: Replace with selectById
      val root = rootRepo.select(selection) { where(RootDSL.id.eq(rootId)) }.firstOrNull()

      assertNotNull(root)
      assertEquals(rootId, root.id)
      val branchA = root.branchA
      assertNotNull(branchA)
      assertEquals(branchAId, branchA.id)
      val branchAA = branchA.branchA
      assertNotNull(branchAA)
      assertEquals(branchAAId, branchAA.id)
      assertNull(branchAA.branchA)
      assertNull(branchAA.branchB)
      val branchAB = branchA.branchB
      assertNotNull(branchAB)
      assertEquals(branchABId, branchAB.id)
      assertNull(branchAB.branchA)
      assertNull(branchAB.branchB)

      val branchB = root.branchB
      assertNotNull(branchB)
      assertEquals(branchBId, branchB.id)
      assertNull(branchB.branchA)
      assertNull(branchB.branchB)
    }
  }

  @Test
  fun `select recursive selection`() {
    var rootId: Long = -1
    var branchAId: Long = -1
    var branchBId: Long = -1
    var branchABId: Long = -1
    transaction.executeWithoutResult {
      val root = rootRepo.persist(Root())
      rootId = root.id
      val branchA = branchRepo.persist(Branch())
      branchAId = branchA.id
      branchBId = branchRepo.persist(Branch()).id
      branchABId = branchRepo.persist(Branch()).id

      rootRepo.update(root.copy(branchAKey = branchAId, branchBKey = branchBId))
      // Reference to itself on "branchA"!
      branchRepo.update(branchA.copy(branchAKey = branchAId, branchBKey = branchABId))
    }

    transaction.executeWithoutResult {
      val selection =
        RootDSL.select {
          this.branchA {
            this.branchA {
              this.branchA {}
              this.branchB {}
            }
            this.branchB {
              this.branchA {}
              this.branchB {}
            }
          }
          this.branchB {
            this.branchA {
              this.branchA {}
              this.branchB {}
            }
            this.branchB {
              this.branchA {}
              this.branchB {}
            }
          }
        }

      // TODO: Replace with selectById
      val root = rootRepo.select(selection) { where(RootDSL.id.eq(rootId)) }.firstOrNull()

      assertNotNull(root)
      assertEquals(rootId, root.id)
      val branchA = root.branchA
      assertNotNull(branchA)
      assertEquals(branchAId, branchA.id)
      val branchAA = branchA.branchA
      assertNotNull(branchAA)
      assertEquals(branchAId, branchAA.id)
      val branchAAA = branchAA.branchA
      assertNotNull(branchAAA)
      assertEquals(branchAId, branchAAA.id)
      assertNull(branchAAA.branchA)
      assertNull(branchAAA.branchB)

      val branchAB = branchA.branchB
      assertNotNull(branchAB)
      assertEquals(branchABId, branchAB.id)
      assertNull(branchAB.branchA)
      assertNull(branchAB.branchB)

      // TODO: Investigate in the future: Not set branch keys are 0 instead of `null`
      val branchB = root.branchB
      assertNotNull(branchB)
      assertEquals(branchBId, branchB.id)
      assertNull(branchB.branchA)
      assertNull(branchB.branchB)
    }
  }
}
