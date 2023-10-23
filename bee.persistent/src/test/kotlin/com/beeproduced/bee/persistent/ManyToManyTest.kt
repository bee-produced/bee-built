package com.beeproduced.bee.persistent

import com.beeproduced.bee.persistent.selection.DataSelection
import com.beeproduced.bee.persistent.selection.EmptySelection
import com.beeproduced.bee.persistent.selection.FullNonRecursiveSelection
import com.beeproduced.bee.persistent.config.DummyApplication
import com.beeproduced.bee.persistent.config.PersistenceConfiguration
import com.beeproduced.bee.persistent.many.to.many.*
import com.linecorp.kotlinjdsl.querydsl.expression.column
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
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-02-07
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [DummyApplication::class])
@TestPropertySource("classpath:application.properties")
@ContextConfiguration(classes = [ManyToManyTest.TestConfig::class, PersistenceConfiguration::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ManyToManyTest(
    @Autowired
    val fooRepo: FooRepository,
    @Autowired
    val barRepo: BarRepository,
    @Autowired
    val fooBarRepo: FooBarRelationRepository,
    @Qualifier("orderTransactionManager")
    transactionManager: PlatformTransactionManager
) {
    private val transaction = TransactionTemplate(transactionManager)

    @Configuration
    class TestConfig(val em: EntityManager) {
        @Bean
        fun fooRepository() = FooRepository(em)

        @Bean
        fun barRepository() = BarRepository(em)

        @Bean
        fun fooBarRepository() = FooBarRelationRepository(em)
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
            fooBarRepo.deleteAll()
            fooRepo.deleteAll()
            barRepo.deleteAll()
        }
    }

    @Test
    fun `add relation`() {
        var barId: Long = -1
        var foo1Id: Long = -1
        var foo2Id: Long = -1
        var foo3Id: Long = -1

        transaction.executeWithoutResult {
            val bar = barRepo.persist(Bar())
            barId = bar.id
            val foo1 = fooRepo.persist(Foo())
            foo1Id = foo1.id
            val foo2 = fooRepo.persist(Foo())
            foo2Id = foo2.id
            val foo3 = fooRepo.persist(Foo())
            foo3Id = foo3.id
        }

        val selection = FullNonRecursiveSelection()
        var bars = barRepo.select(selection)
        assertEquals(1, bars.count())
        var foos = fooRepo.select(selection)
        assertEquals(3, foos.count())

        transaction.executeWithoutResult {
            fooBarRepo.persist(FooBarRelation(foo1Id, barId))
        }

        var bar = barRepo.selectById(barId, selection)
        assertNotNull(bar)
        assertEquals(foo1Id, bar.foos?.first()?.id)
        var foo1 = fooRepo.selectById(foo1Id, selection)
        assertNotNull(foo1)
        assertEquals(barId, foo1.bars?.first()?.id)
        var foo2 = fooRepo.selectById(foo2Id, selection)
        assertNotNull(foo2)
        assertEquals(setOf(), foo2.bars)

        transaction.executeWithoutResult {
            fooBarRepo.persist(FooBarRelation(foo2Id, barId))
        }

        bar = barRepo.selectById(barId, selection)
        assertNotNull(bar)
        assertTrue {
            for (foo in bar.foos!!) {
                if (foo.id != foo1Id && foo.id != foo2Id) return@assertTrue false
            }
            true
        }
        foo1 = fooRepo.selectById(foo1Id, selection)
        assertNotNull(foo1)
        assertEquals(barId, foo1.bars?.first()?.id)
        foo2 = fooRepo.selectById(foo2Id, selection)
        assertNotNull(foo2)
        assertEquals(barId, foo2.bars?.first()?.id)
    }

    @Test
    fun `select with relations not loaded`() {
        var barId: Long = -1
        var foo1Id: Long = -1
        var foo2Id: Long = -1
        var foo3Id: Long = -1

        transaction.executeWithoutResult {
            val bar = barRepo.persist(Bar())
            barId = bar.id
            val foo1 = fooRepo.persist(Foo())
            foo1Id = foo1.id
            val foo2 = fooRepo.persist(Foo())
            foo2Id = foo2.id
            val foo3 = fooRepo.persist(Foo())
            foo3Id = foo3.id
        }

        var selection: DataSelection = FullNonRecursiveSelection()
        var bars = barRepo.select(selection)
        assertEquals(1, bars.count())
        var foos = fooRepo.select(selection)
        assertEquals(3, foos.count())

        transaction.executeWithoutResult {
            fooBarRepo.persist(FooBarRelation(foo1Id, barId))
        }

        var bar = barRepo.selectById(barId, selection)
        assertNotNull(bar)
        assertEquals(foo1Id, bar.foos?.first()?.id)
        var foo1 = fooRepo.selectById(foo1Id, selection)
        assertNotNull(foo1)
        assertEquals(barId, foo1.bars?.first()?.id)
        var foo2 = fooRepo.selectById(foo2Id, selection)
        assertNotNull(foo2)
        assertEquals(setOf(), foo2.bars)

        transaction.executeWithoutResult {
            fooBarRepo.persist(FooBarRelation(foo2Id, barId))
        }

        // Do not load relations
        selection = EmptySelection()
        bar = barRepo.selectById(barId, selection)
        assertNotNull(bar)
        assertNull(bar.foos)
        foo1 = fooRepo.selectById(foo1Id, selection)
        assertNotNull(foo1)
        assertNull(foo1.bars)
        foo2 = fooRepo.selectById(foo2Id, selection)
        assertNotNull(foo2)
        assertNull(foo2.bars)
    }

    @Test
    fun `delete relation`() {
        var barId: Long = -1
        var foo1Id: Long = -1
        var foo2Id: Long = -1
        var foo3Id: Long = -1

        transaction.executeWithoutResult {
            val bar = barRepo.persist(Bar())
            barId = bar.id
            val foo1 = fooRepo.persist(Foo())
            foo1Id = foo1.id
            val foo2 = fooRepo.persist(Foo())
            foo2Id = foo2.id
            val foo3 = fooRepo.persist(Foo())
            foo3Id = foo3.id
        }

        val selection = FullNonRecursiveSelection()
        var bars = barRepo.select(selection)
        assertEquals(1, bars.count())
        var foos = fooRepo.select(selection)
        assertEquals(3, foos.count())

        transaction.executeWithoutResult {
            fooBarRepo.persist(FooBarRelation(foo1Id, barId))
        }

        var bar = barRepo.selectById(barId, selection)
        assertNotNull(bar)
        assertEquals(foo1Id, bar.foos?.first()?.id)
        var foo1 = fooRepo.selectById(foo1Id, selection)
        assertNotNull(foo1)
        assertEquals(barId, foo1.bars?.first()?.id)
        var foo2 = fooRepo.selectById(foo2Id, selection)
        assertNotNull(foo2)
        assertEquals(setOf(), foo2.bars)

        transaction.executeWithoutResult {
            fooBarRepo.delete {
                where(column(FooBarRelation::bar).equal(barId))
            }
        }

        bar = barRepo.selectById(barId, selection)
        assertNotNull(bar)
        assertEquals(setOf(), bar.foos)
        foo1 = fooRepo.selectById(foo1Id, selection)
        assertNotNull(foo1)
        assertEquals(setOf(), foo1.bars)
    }
}
