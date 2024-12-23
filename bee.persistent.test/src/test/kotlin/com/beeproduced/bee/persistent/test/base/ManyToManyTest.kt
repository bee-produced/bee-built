package com.beeproduced.bee.persistent.test.base

import com.beeproduced.bee.persistent.blaze.selection.BeeSelection
import com.beeproduced.bee.persistent.test.config.BaseTestConfig
import com.beeproduced.datasource.test.dsl.BarDSL
import com.beeproduced.datasource.test.dsl.FooDSL
import com.beeproduced.datasource.test.manytomany.*
import jakarta.persistence.EntityManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
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
 * @version 2024-01-15
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [BaseTestConfig::class])
@TestPropertySource("classpath:application.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ManyToManyTest(
  @Qualifier("testEM") val em: EntityManager,
  @Qualifier("testTM") transactionManager: PlatformTransactionManager,
  @Autowired val fooRepo: FooRepository,
  @Autowired val barRepo: BarRepository,
  @Autowired val fooBarRepo: FooBarRepository,
) {
  private val transaction = TransactionTemplate(transactionManager)

  @BeforeAll fun beforeAll() = clear()

  @AfterEach fun afterEach() = clear()

  fun clear() =
    transaction.executeWithoutResult {
      fooBarRepo.cbf.delete(em, FooBarRelation::class.java)
      fooRepo.cbf.delete(em, Foo::class.java).executeUpdate()
      barRepo.cbf.delete(em, Bar::class.java).executeUpdate()
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

    transaction.executeWithoutResult {
      val bars = barRepo.select()
      assertEquals(1, bars.count())
      val foos = fooRepo.select()
      assertEquals(3, foos.count())
    }

    transaction.executeWithoutResult { fooBarRepo.persist(FooBarRelation(FooBarId(foo1Id, barId))) }

    val barSelection = BarDSL.select { this.foos { this.bars { this.foos {} } } }
    val fooSelection = FooDSL.select { this.bars { this.foos { this.bars {} } } }
    transaction.executeWithoutResult {
      val bar = barRepo.select(barSelection) { where(BarDSL.id.eq(barId)) }.firstOrNull()
      assertNotNull(bar)
      assertBar(bar, setOf(barId), setOf(foo1Id), 3)
      val foo1 = fooRepo.select(fooSelection) { where(FooDSL.id.eq(foo1Id)) }.firstOrNull()
      assertNotNull(foo1)
      assertFoo(foo1, setOf(foo1Id), setOf(barId), 3)
      val foo2 = fooRepo.select(fooSelection) { where(FooDSL.id.eq(foo2Id)) }.firstOrNull()
      assertNotNull(foo2)
      assertFoo(foo2, setOf(foo2Id), emptySet(), 3)
    }

    transaction.executeWithoutResult { fooBarRepo.persist(FooBarRelation(FooBarId(foo2Id, barId))) }

    transaction.executeWithoutResult {
      val bar = barRepo.select(barSelection) { where(BarDSL.id.eq(barId)) }.firstOrNull()
      assertNotNull(bar)
      assertBar(bar, setOf(barId), setOf(foo1Id, foo2Id), 3)
      val foo1 = fooRepo.select(fooSelection) { where(FooDSL.id.eq(foo1Id)) }.firstOrNull()
      assertNotNull(foo1)
      assertFoo(foo1, setOf(foo1Id, foo2Id), setOf(barId), 3)
      val foo2 = fooRepo.select(fooSelection) { where(FooDSL.id.eq(foo2Id)) }.firstOrNull()
      assertNotNull(foo2)
      assertFoo(foo2, setOf(foo2Id, foo1Id), setOf(barId), 3)
    }
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

    transaction.executeWithoutResult {
      val bars = barRepo.select()
      assertEquals(1, bars.count())
      val foos = fooRepo.select()
      assertEquals(3, foos.count())
    }

    transaction.executeWithoutResult { fooBarRepo.persist(FooBarRelation(FooBarId(foo1Id, barId))) }

    val barSelection = BeeSelection.empty()
    val fooSelection = BeeSelection.empty()
    transaction.executeWithoutResult {
      val bar = barRepo.select(barSelection) { where(BarDSL.id.eq(barId)) }.firstOrNull()
      assertNotNull(bar)
      assertBar(bar, setOf(barId), setOf(foo1Id), 0)
      val foo1 = fooRepo.select(fooSelection) { where(FooDSL.id.eq(foo1Id)) }.firstOrNull()
      assertNotNull(foo1)
      assertFoo(foo1, setOf(foo1Id), setOf(barId), 0)
      val foo2 = fooRepo.select(fooSelection) { where(FooDSL.id.eq(foo2Id)) }.firstOrNull()
      assertNotNull(foo2)
      assertFoo(foo2, setOf(foo2Id), emptySet(), 0)
    }

    transaction.executeWithoutResult { fooBarRepo.persist(FooBarRelation(FooBarId(foo2Id, barId))) }

    transaction.executeWithoutResult {
      val bar = barRepo.select(barSelection) { where(BarDSL.id.eq(barId)) }.firstOrNull()
      assertNotNull(bar)
      assertBar(bar, setOf(barId), setOf(foo1Id, foo2Id), 0)
      val foo1 = fooRepo.select(fooSelection) { where(FooDSL.id.eq(foo1Id)) }.firstOrNull()
      assertNotNull(foo1)
      assertFoo(foo1, setOf(foo1Id, foo2Id), setOf(barId), 0)
      val foo2 = fooRepo.select(fooSelection) { where(FooDSL.id.eq(foo2Id)) }.firstOrNull()
      assertNotNull(foo2)
      assertFoo(foo2, setOf(foo2Id, foo1Id), setOf(barId), 0)
    }
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

    transaction.executeWithoutResult {
      val bars = barRepo.select()
      assertEquals(1, bars.count())
      val foos = fooRepo.select()
      assertEquals(3, foos.count())
    }

    transaction.executeWithoutResult { fooBarRepo.persist(FooBarRelation(FooBarId(foo1Id, barId))) }

    val barSelection = BarDSL.select { this.foos { this.bars { this.foos {} } } }
    val fooSelection = FooDSL.select { this.bars { this.foos { this.bars {} } } }
    transaction.executeWithoutResult {
      val bar = barRepo.select(barSelection) { where(BarDSL.id.eq(barId)) }.firstOrNull()
      assertNotNull(bar)
      assertBar(bar, setOf(barId), setOf(foo1Id), 3)
      val foo1 = fooRepo.select(fooSelection) { where(FooDSL.id.eq(foo1Id)) }.firstOrNull()
      assertNotNull(foo1)
      assertFoo(foo1, setOf(foo1Id), setOf(barId), 3)
    }

    transaction.executeWithoutResult {
      // TODO: Replace with delete
      fooBarRepo.cbf
        .delete(em, FooBarRelation::class.java)
        .where("id.bar")
        .eq(barId)
        .executeUpdate()
    }

    transaction.executeWithoutResult {
      val bar = barRepo.select(barSelection) { where(BarDSL.id.eq(barId)) }.firstOrNull()
      assertNotNull(bar)
      assertBar(bar, setOf(barId), emptySet(), 3)
      val foo1 = fooRepo.select(fooSelection) { where(FooDSL.id.eq(foo1Id)) }.firstOrNull()
      assertNotNull(foo1)
      assertFoo(foo1, setOf(foo1Id, foo2Id), emptySet(), 3)
    }
  }

  private fun assertBar(bar: Bar, barIds: Set<Long>, fooIds: Set<Long>, depth: Int) {
    assertTrue { barIds.contains(bar.id) }
    val foos = bar.foos
    if (depth == 0) {
      assertTrue { foos.isNullOrEmpty() }
      return
    }

    assertNotNull(foos)
    for (foo in foos) assertFoo(foo, fooIds, barIds, depth - 1)
  }

  private fun assertFoo(foo: Foo, fooIds: Set<Long>, barIds: Set<Long>, depth: Int) {
    assertTrue { fooIds.contains(foo.id) }
    val bars = foo.bars
    if (depth == 0) {
      assertTrue { bars.isNullOrEmpty() }
      return
    }

    assertNotNull(bars)
    for (bar in bars) assertBar(bar, barIds, fooIds, depth - 1)
  }
}
