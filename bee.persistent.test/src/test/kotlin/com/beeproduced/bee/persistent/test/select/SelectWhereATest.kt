package com.beeproduced.bee.persistent.test.select

import com.beeproduced.bee.persistent.blaze.dsl.expression.builder.lower
import com.beeproduced.bee.persistent.blaze.dsl.predicate.builder.and
import com.beeproduced.bee.persistent.blaze.dsl.predicate.builder.or
import com.beeproduced.bee.persistent.test.config.ATestConfig
import com.beeproduced.datasource.a.*
import com.beeproduced.datasource.a.dsl.CircularDSL
import com.beeproduced.datasource.a.dsl.WeirdClassDSL
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

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
class SelectWhereATest(
    @Qualifier("aEM")
    val em: EntityManager,
    @Qualifier("aTM")
    transactionManager: PlatformTransactionManager,
    @Autowired
    val circularRepo: CircularRepository,
    @Autowired
    val weirdRepo: WeirdClassRepository
) {
    private val transaction = TransactionTemplate(transactionManager)

    @BeforeAll
    fun beforeAll() = clear()

    @AfterEach
    fun afterEach() = clear()

    fun clear() {
        transaction.executeWithoutResult {
            circularRepo.cbf.delete(em, Circular::class.java).executeUpdate()
            weirdRepo.cbf.delete(em, WeirdClass::class.java).executeUpdate()
        }
    }

    @Test
    fun `where`() {
        transaction.executeWithoutResult {
            val id1 = UUID.randomUUID()
            val id2 = UUID.randomUUID()
            circularRepo.persist(Circular(id1, null, null))
            circularRepo.persist(Circular(id2, id1, null))
            circularRepo.cbf.update(em, Circular::class.java)
                .set("cId", id2)
                .where("id").eq(id1)
                .executeUpdate()

            val selection = CircularDSL.select { this.circular { this.circular { } } }
            val circles = circularRepo.select(selection) {
                where(CircularDSL.id.eq(id2))
            }

            assertEquals(1, circles.count())
            val circle = circles.firstOrNull()
            assertNotNull(circle)
            assertEquals(id2, circle.id)
        }
    }

    @Test
    fun whereAnd() {
        transaction.executeWithoutResult {
            val id1 = UUID.randomUUID()
            val id2 = UUID.randomUUID()
            circularRepo.persist(Circular(id1, null, null))
            circularRepo.persist(Circular(id2, id1, null))
            circularRepo.cbf.update(em, Circular::class.java)
                .set("cId", id2)
                .where("id").eq(id1)
                .executeUpdate()

            val selection = CircularDSL.select { this.circular { this.circular { } } }
            val circles = circularRepo.select(selection) {
                whereAnd(
                    CircularDSL.id.eq(id1),
                    CircularDSL.id.eq(id2),
                )
            }

            assertEquals(0, circles.count())
        }
    }

    @Test
    fun `whereOr with inner whereAnd`() {
        transaction.executeWithoutResult {
            val id1 = UUID.randomUUID()
            val id2 = UUID.randomUUID()
            circularRepo.persist(Circular(id1, null, null))
            circularRepo.persist(Circular(id2, id1, null))
            circularRepo.cbf.update(em, Circular::class.java)
                .set("cId", id2)
                .where("id").eq(id1)
                .executeUpdate()

            val selection = CircularDSL.select { this.circular { this.circular { } } }
            val circles = circularRepo.select(selection) {
                whereOr(
                    CircularDSL.id.eq(id1),
                    and(
                        CircularDSL.id.eq(id2),
                        CircularDSL.id.eq(UUID.randomUUID())
                    )
                )
            }

            assertEquals(1, circles.count())
            val circle = circles.firstOrNull()
            assertNotNull(circle)
            assertEquals(id1, circle.id)
        }
    }

    @Test
    fun `whereOr with inner whereOr`() {
        transaction.executeWithoutResult {
            val id1 = UUID.randomUUID()
            val id2 = UUID.randomUUID()
            circularRepo.persist(Circular(id1, null, null))
            circularRepo.persist(Circular(id2, id1, null))
            circularRepo.cbf.update(em, Circular::class.java)
                .set("cId", id2)
                .where("id").eq(id1)
                .executeUpdate()

            val selection = CircularDSL.select { this.circular { this.circular { } } }
            val circles = circularRepo.select(selection) {
                whereOr(
                    CircularDSL.id.eq(id1),
                    or(
                        CircularDSL.id.eq(id2),
                        CircularDSL.id.eq(UUID.randomUUID())
                    )
                )
            }

            assertEquals(2, circles.count())
            val circle1 = circles.firstOrNull { it.id == id1 }
            assertNotNull(circle1)
            val circle2 = circles.firstOrNull { it.id == id2 }
            assertNotNull(circle2)
        }
    }

    @Test
    fun `whereAnd with inner whereAnd`() {
        transaction.executeWithoutResult {
            val id1 = UUID.randomUUID()
            val id2 = UUID.randomUUID()
            circularRepo.persist(Circular(id1, null, null))
            circularRepo.persist(Circular(id2, id1, null))
            circularRepo.cbf.update(em, Circular::class.java)
                .set("cId", id2)
                .where("id").eq(id1)
                .executeUpdate()

            val selection = CircularDSL.select { this.circular { this.circular { } } }
            val circles = circularRepo.select(selection) {
                whereAnd(
                    CircularDSL.id.eq(id1),
                    and(
                        CircularDSL.id.eq(id2),
                        CircularDSL.id.eq(UUID.randomUUID())
                    )
                )
            }

            assertEquals(0, circles.count())
        }
    }

    @Test
    fun `whereAnd with inner whereOr`() {
        transaction.executeWithoutResult {
            val id1 = UUID.randomUUID()
            val id2 = UUID.randomUUID()
            circularRepo.persist(Circular(id1, null, null))
            circularRepo.persist(Circular(id2, id1, null))
            circularRepo.cbf.update(em, Circular::class.java)
                .set("cId", id2)
                .where("id").eq(id1)
                .executeUpdate()

            val selection = CircularDSL.select { this.circular { this.circular { } } }
            val circles = circularRepo.select(selection) {
                whereAnd(
                    CircularDSL.id.eq(id1),
                    or(
                        CircularDSL.id.eq(id2),
                        CircularDSL.id.eq(UUID.randomUUID())
                    )
                )
            }

            assertEquals(0, circles.count())
        }
    }

    @Test
    fun `where with inline value class`() {
        transaction.executeWithoutResult {
            val id = UUID.randomUUID()
            val fooBar = FooBar("foo", "bar")
            val foxtrot = Foxtrot("Foxtrot")
            weirdRepo.persist(WeirdClass(id, fooBar, foxtrot))
            val id2 = UUID.randomUUID()
            val fooBar2 = FooBar("foo2", "bar2")
            val foxtrot2 = Foxtrot("foxtrot2")
            weirdRepo.persist(WeirdClass(id2, fooBar2, foxtrot2))

            val ws = weirdRepo.select {
                where(WeirdClassDSL.foxtrot.eq("Foxtrot"))
            }
            assertEquals(1, ws.count())
            val w = ws.first()
            assertEquals(id, w.id)

            val wsInline = weirdRepo.select {
                where(WeirdClassDSL.foxtrot.eqInline(Foxtrot("Foxtrot")))
            }
            assertEquals(1, wsInline.count())
            val wInline = wsInline.first()
            assertEquals(id, wInline.id)

            assertEquals(w, wInline)
        }
    }

    @Test
    fun `lower expression`() {
        transaction.executeWithoutResult {
            val id = UUID.randomUUID()
            val fooBar = FooBar("foo", "bar")
            val foxtrot = Foxtrot("Foxtrot")
            weirdRepo.persist(WeirdClass(id, fooBar, foxtrot))
            val id2 = UUID.randomUUID()
            val fooBar2 = FooBar("foo2", "bar2")
            val foxtrot2 = Foxtrot("foxtrot2")
            weirdRepo.persist(WeirdClass(id2, fooBar2, foxtrot2))

            val ws = weirdRepo.select {
                where(lower(WeirdClassDSL.foxtrot).eq("Foxtrot".lowercase()))
            }
            assertEquals(1, ws.count())
            val w = ws.first()
            assertEquals(id, w.id)
        }
    }

}