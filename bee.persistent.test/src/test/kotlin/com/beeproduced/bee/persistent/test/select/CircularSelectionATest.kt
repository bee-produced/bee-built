package com.beeproduced.bee.persistent.test.select

import com.beeproduced.bee.persistent.blaze.selection.BeeSelection
import com.beeproduced.bee.persistent.test.beePersist
import com.beeproduced.bee.persistent.test.config.ATestConfig
import com.beeproduced.datasource.a.*
import com.beeproduced.datasource.a.dsl.CircularDSL
import com.beeproduced.datasource.a.dsl.SemiCircular1DSL
import jakarta.persistence.EntityManager
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
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-15
 */

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [ATestConfig::class])
@TestPropertySource("classpath:application.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CircularSelectionATest(
    @Qualifier("aEM")
    val em: EntityManager,
    @Qualifier("aTM")
    transactionManager: PlatformTransactionManager,
    @Autowired
    val circularRepo: CircularRepository,
    @Autowired
    val semiCircular1Repo: SemiCircular1Repository,
    @Autowired
    val semiCircular2Repo: SemiCircular2Repository,
) {
    private val transaction = TransactionTemplate(transactionManager)

    @BeforeAll
    fun beforeAll() = clear()

    @AfterEach
    fun afterEach() = clear()

    fun clear() {
        transaction.executeWithoutResult {
            circularRepo.cbf.delete(em, Circular::class.java).executeUpdate()
            semiCircular1Repo.cbf.delete(em, SemiCircular1::class.java).executeUpdate()
            semiCircular2Repo.cbf.delete(em, SemiCircular2::class.java).executeUpdate()
        }
    }

    @Test
    fun `test circular`() {
        transaction.executeWithoutResult {
            val id = UUID.randomUUID()
            circularRepo.persist(Circular(id, id, null))

            val selection = CircularDSL.select {
                this.circular { this.circular {  } }
            }
            val circle  = circularRepo.select(selection).firstOrNull()
            assertNotNull(circle)
            assertCircular(circle, listOf(id, id, id))
        }
    }

    @Test
    fun `test circular 2`() {
        transaction.executeWithoutResult {
            val id1 = UUID.randomUUID()
            val id2 = UUID.randomUUID()
            circularRepo.persist(Circular(id1, null, null))
            circularRepo.persist(Circular(id2, id1, null))
            circularRepo.cbf.update(em, Circular::class.java)
                .set("cId", id2)
                .where("id").eq(id1)
                .executeUpdate()

            val selection = CircularDSL.select {
                this.circular { this.circular {  } }
            }
            val circles = circularRepo.select(selection)

            val circle1 = circles.firstOrNull { it.id == id1 }
            assertNotNull(circle1)
            assertCircular(circle1, listOf(id1, id2, id1))

            val circle2 = circles.firstOrNull { it.id == id2 }
            assertNotNull(circle2)
            assertCircular(circle2, listOf(id2, id1, id2))
        }
    }

    @Test
    fun `test circular 3`() {
        transaction.executeWithoutResult {
            val id1 = UUID.randomUUID()
            circularRepo.persist(Circular(id1, id1, null))

            val selection = BeeSelection.empty()
            val circle  = circularRepo.select(selection).firstOrNull()
            assertNotNull(circle)
            assertNull(circle.circular)
        }
    }

    @Test
    fun `test semi circular`() {
        transaction.executeWithoutResult {
            val id1 = UUID.randomUUID()
            val id2 = UUID.randomUUID()
            semiCircular1Repo.persist(SemiCircular1(id1, null, null))
            semiCircular2Repo.persist(SemiCircular2(id2, id1, null))
            semiCircular1Repo.cbf.update(em, SemiCircular1::class.java)
                .set("cId", id2)
                .where("id").eq(id1)
                .executeUpdate()

            val selection = SemiCircular1DSL.select {
                this.circular { this.circular { this.circular { } } }
            }
            val circle = semiCircular1Repo.select(selection).firstOrNull()
            assertNotNull(circle)
            assertSemiCircular1(circle, listOf(id1, id2, id1, id2))
        }
    }

    @Test
    fun `test semi circular 2`() {
        transaction.executeWithoutResult {
            val id1 = UUID.randomUUID()
            val id2 = UUID.randomUUID()
            val id3 = UUID.randomUUID()
            semiCircular1Repo.persist(SemiCircular1(id1, null, null))
            semiCircular2Repo.persist(SemiCircular2(id2, id1, null))
            semiCircular1Repo.persist(SemiCircular1(id3, id2, null))
            semiCircular1Repo.cbf.update(em, SemiCircular1::class.java)
                .set("cId", id2)
                .where("id").eq(id1)
                .executeUpdate()

            val selection = SemiCircular1DSL.select {
                this.circular { this.circular { this.circular { } } }
            }
            val circle = semiCircular1Repo.select(selection)
                .firstOrNull { it.id == id3 }
            assertNotNull(circle)
            assertSemiCircular1(circle, listOf(id3, id2, id1, id2))
        }
    }

    @Test
    fun `test semi circular 3`() {
        transaction.executeWithoutResult {
            val id1 = UUID.randomUUID()
            val id2 = UUID.randomUUID()
            val id3 = UUID.randomUUID()
            val id4 = UUID.randomUUID()
            semiCircular1Repo.persist(SemiCircular1(id1, null, null))
            semiCircular2Repo.persist(SemiCircular2(id2, null, null))
            semiCircular1Repo.persist(SemiCircular1(id3, null, null))
            semiCircular2Repo.persist(SemiCircular2(id4, id1, null))
            semiCircular1Repo.cbf.update(em, SemiCircular1::class.java)
                .set("cId", id2).where("id").eq(id1).executeUpdate()
            semiCircular2Repo.cbf.update(em, SemiCircular2::class.java)
                .set("cId", id3).where("id").eq(id2).executeUpdate()
            semiCircular1Repo.cbf.update(em, SemiCircular1::class.java)
                .set("cId", id4).where("id").eq(id3).executeUpdate()

            val selection = SemiCircular1DSL.select {
                this.circular { this.circular { this.circular { } } }
            }
            val circle = semiCircular1Repo.select(selection)
                .firstOrNull { it.id == id1 }
            assertNotNull(circle)
            assertSemiCircular1(circle, listOf(id1, id2, id3, id4))
        }
    }

    private fun assertCircular(circular: Circular, ids: List<UUID>) {
        val id = ids.firstOrNull() ?: return
        assertEquals(id, circular.id)
        val remainingIds = ids.drop(1)
        if (remainingIds.isNotEmpty()) {
            val c = circular.circular
            assertNotNull(c)
            assertCircular(c, remainingIds)
        }
    }

    private fun assertSemiCircular1(circular1: SemiCircular1, ids: List<UUID>) {
        val id = ids.firstOrNull() ?: return
        assertEquals(id, circular1.id)
        val remainingIds = ids.drop(1)
        if (remainingIds.isNotEmpty()) {
            val c = circular1.circular
            assertNotNull(c)
            assertSemiCircular2(c, remainingIds)
        }
    }

    private fun assertSemiCircular2(circular2: SemiCircular2, ids: List<UUID>) {
        val id = ids.firstOrNull() ?: return
        assertEquals(id, circular2.id)
        val remainingIds = ids.drop(1)
        if (remainingIds.isNotEmpty()) {
            val c = circular2.circular
            assertNotNull(c)
            assertSemiCircular1(c, remainingIds)
        }
    }
}