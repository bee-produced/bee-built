package com.beeproduced.bee.persistent

import com.beeproduced.bee.persistent.selection.EmptySelection
import com.beeproduced.bee.persistent.selection.FullNonRecursiveSelection
import com.beeproduced.bee.persistent.selection.SimpleSelection
import com.beeproduced.bee.persistent.selection.SimpleSelection.FieldNode
import com.beeproduced.bee.persistent.config.DummyApplication
import com.beeproduced.bee.persistent.config.PersistenceConfiguration
import com.beeproduced.lib.data.one.to.many.*
import com.beeproduced.bee.persistent.one.to.one.Branch
import com.beeproduced.bee.persistent.one.to.one.BranchRepository
import com.beeproduced.bee.persistent.one.to.one.Root
import com.beeproduced.bee.persistent.one.to.one.RootRepository
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import jakarta.persistence.EntityManager
import org.hibernate.Hibernate
import kotlin.test.Ignore
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-02-07
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [DummyApplication::class])
@TestPropertySource("classpath:application.properties")
@ContextConfiguration(classes = [OneToOneTest.TestConfig::class, PersistenceConfiguration::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OneToOneTest(
    @Autowired
    val rootRepo: RootRepository,
    @Autowired
    val branchRepo: BranchRepository,
    @Qualifier("orderTransactionManager")
    transactionManager: PlatformTransactionManager,
    @Qualifier("orderEntityManager")
    val em: EntityManager
) {
    private val transaction = TransactionTemplate(transactionManager)

    @Configuration
    class TestConfig(val em: EntityManager) {
        @Bean
        fun rootRepository(): RootRepository = RootRepository(em)

        @Bean
        fun collectionRepository(): BranchRepository = BranchRepository(em)
    }

    @BeforeAll
    fun beforeAll() {
        clear()
    }

    @AfterEach
    fun afterEach() {
        clear()
    }

    fun clear() {
        transaction.executeWithoutResult {
            rootRepo.deleteAll()
            branchRepo.deleteAll()
        }
    }

    @Test
    @Ignore("Single relations are loaded eagerly since Hibernate 6")
    fun `select with relations not loaded`() {
        var rootId: Long = -1

        transaction.executeWithoutResult {
            val root = rootRepo.persist(Root())
            rootId = root.id
            val branchA = branchRepo.persist(Branch())
            val branchB = branchRepo.persist(Branch())
            val branchAA = branchRepo.persist(Branch())
            val branchAB = branchRepo.persist(Branch())

            rootRepo.update(
                root.copy(branchAKey = branchA.id, branchBKey = branchB.id)
            )
            branchRepo.update(
                branchA.copy(branchAKey = branchAA.id, branchBKey = branchAB.id)
            )
        }

        val selection = EmptySelection()
        val root = rootRepo.selectById(rootId, selection)

        assertNotNull(root)
        assertNull(root.branchA)
        assertNull(root.branchB)
    }

    @Test
    @Ignore("Single relations are loaded eagerly since Hibernate 6")
    fun `select with some relations loaded`() {
        var rootId: Long = -1

        transaction.executeWithoutResult {
            val root = rootRepo.persist(Root())
            rootId = root.id
            val branchA = branchRepo.persist(Branch())
            val branchB = branchRepo.persist(Branch())
            val branchAA = branchRepo.persist(Branch())
            val branchAB = branchRepo.persist(Branch())

            rootRepo.update(
                root.copy(branchAKey = branchA.id, branchBKey = branchB.id)
            )
            branchRepo.update(
                branchA.copy(branchAKey = branchAA.id, branchBKey = branchAB.id)
            )
        }

        transaction.executeWithoutResult {
            val selection = SimpleSelection(
                setOf(
                    FieldNode(
                        Root::branchA.name, setOf(
                            FieldNode(Branch::branchA.name)
                        )
                    )
                )
            )

            val jpaEntity = em.find(Root::class.java, rootId)
            if (!Hibernate.isInitialized(jpaEntity.branchA)) {
                println("proxy")
            }
            if (!Hibernate.isInitialized(jpaEntity.branchB)) {
                println("proxy2")
            }
            em.clear()

            val root = rootRepo.selectById(rootId, selection)

            assertNotNull(root)
            assertNotNull(root.branchA)
            assertNotNull(root.branchA.branchA)
            assertNull(root.branchA.branchB)
            assertNull(root.branchB)
        }
    }

    @Test
    fun `select with relations loaded`() {
        var rootId: Long = -1

        transaction.executeWithoutResult {
            val root = rootRepo.persist(Root())
            rootId = root.id
            val branchA = branchRepo.persist(Branch())
            val branchB = branchRepo.persist(Branch())
            val branchAA = branchRepo.persist(Branch())
            val branchAB = branchRepo.persist(Branch())

            rootRepo.update(
                root.copy(branchAKey = branchA.id, branchBKey = branchB.id)
            )
            branchRepo.update(
                branchA.copy(branchAKey = branchAA.id, branchBKey = branchAB.id)
            )
        }

        // Note: It is not advised to use `FullSelection` in production as it has
        // additional overhead to omit recursions and in some instances could lead
        // to incomplete results not anticipated with a “FULL” selection
        val selection = FullNonRecursiveSelection()
        val root = rootRepo.selectById(rootId, selection)

        assertNotNull(root)
        assertNotNull(root.branchA)
        assertNotNull(root.branchA?.branchA)
        assertNotNull(root.branchA?.branchB)
        assertNotNull(root.branchB)
    }

    @Test
    fun `select recursive relation`() {
        var rootId: Long = -1

        transaction.executeWithoutResult {
            val root = rootRepo.persist(Root())
            rootId = root.id
            val branchA = branchRepo.persist(Branch())
            val branchB = branchRepo.persist(Branch())
            // val branchAA = branchRepo.persist(Branch())
            val branchAB = branchRepo.persist(Branch())

            rootRepo.update(
                root.copy(branchAKey = branchA.id, branchBKey = branchB.id)
            )
            // Reference to itself on "branchA"!
            branchRepo.update(
                branchA.copy(branchAKey = branchA.id, branchBKey = branchAB.id)
            )
        }

        // Note: It is not advised to use `FullSelection` in production as it has
        // additional overhead to omit recursions and in some instances could lead
        // to incomplete results not anticipated with a “FULL” selection
        val selection = FullNonRecursiveSelection()
        val root = rootRepo.selectById(rootId, selection)

        assertNotNull(root)
        assertNotNull(root.branchA)
        assertNotNull(root.branchA?.branchA)
        assertNotNull(root.branchA?.branchB)
        assertNotNull(root.branchB)
    }
}
