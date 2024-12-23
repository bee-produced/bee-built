package com.beeproduced.bee.persistent.test.select

import com.beeproduced.bee.persistent.blaze.selection.BeeSelection
import com.beeproduced.bee.persistent.test.config.BaseTestConfig
import com.beeproduced.datasource.test.dsl.LazyEntityDSL
import com.beeproduced.datasource.test.lazy.LazyEmbedded
import com.beeproduced.datasource.test.lazy.LazyEntity
import com.beeproduced.datasource.test.lazy.LazyEntityRepository
import com.beeproduced.datasource.test.lazy.SomeEmbedded
import jakarta.persistence.EntityManager
import java.util.*
import kotlin.test.Test
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
 * @version 2024-01-16
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [BaseTestConfig::class])
@TestPropertySource("classpath:application.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SelectionLazyFieldTest(
  @Qualifier("testEM") val em: EntityManager,
  @Qualifier("testTM") transactionManager: PlatformTransactionManager,
  @Autowired val lazyRepo: LazyEntityRepository,
) {
  private val transaction = TransactionTemplate(transactionManager)

  @BeforeAll fun beforeAll() = clear()

  @AfterEach fun afterEach() = clear()

  fun clear() {
    transaction.executeWithoutResult {
      lazyRepo.cbf.delete(em, LazyEntity::class.java).executeUpdate()
    }
  }

  @Test
  fun `lazy field empty selection`() {
    transaction.executeWithoutResult {
      val embedded = SomeEmbedded("data", "lazyData")
      val lazyEmbedded = LazyEmbedded("data2", "lazyData2")
      lazyRepo.persist(LazyEntity(UUID.randomUUID(), "name", "lazyName", embedded, lazyEmbedded))

      val selection = BeeSelection.empty()
      val le = lazyRepo.select(selection).firstOrNull()

      assertNotNull(le)
      assertLazyEntity(le, selection)
    }
  }

  @Test
  fun `lazy field full selection`() {
    transaction.executeWithoutResult {
      val embedded = SomeEmbedded("data", "lazyData")
      val lazyEmbedded = LazyEmbedded("data2", "lazyData2")
      lazyRepo.persist(LazyEntity(UUID.randomUUID(), "name", "lazyName", embedded, lazyEmbedded))

      val selection =
        LazyEntityDSL.select {
          lazyName()
          embedded { someLazyData() }
          lazyEmbedded { lazyLazyData() }
        }
      val le = lazyRepo.select(selection).firstOrNull()

      assertNotNull(le)
      assertLazyEntity(le, selection)
    }
  }

  @Test
  fun `lazy field on column`() {
    transaction.executeWithoutResult {
      val embedded = SomeEmbedded("data", "lazyData")
      val lazyEmbedded = LazyEmbedded("data2", "lazyData2")
      lazyRepo.persist(LazyEntity(UUID.randomUUID(), "name", "lazyName", embedded, lazyEmbedded))

      val selection = LazyEntityDSL.select { lazyName() }
      val le = lazyRepo.select(selection).firstOrNull()

      assertNotNull(le)
      assertLazyEntity(le, selection)
    }
  }

  @Test
  fun `lazy field on embedded column`() {
    transaction.executeWithoutResult {
      val embedded = SomeEmbedded("data", "lazyData")
      val lazyEmbedded = LazyEmbedded("data2", "lazyData2")
      lazyRepo.persist(LazyEntity(UUID.randomUUID(), "name", "lazyName", embedded, lazyEmbedded))

      val selection = LazyEntityDSL.select { embedded { someLazyData() } }
      val le = lazyRepo.select(selection).firstOrNull()

      assertNotNull(le)
      assertLazyEntity(le, selection)
    }
  }

  @Test
  fun `lazy field on lazy embedded column`() {
    transaction.executeWithoutResult {
      val embedded = SomeEmbedded("data", "lazyData")
      val lazyEmbedded = LazyEmbedded("data2", "lazyData2")
      lazyRepo.persist(LazyEntity(UUID.randomUUID(), "name", "lazyName", embedded, lazyEmbedded))

      val selection = LazyEntityDSL.select { lazyEmbedded { lazyLazyData() } }
      val le = lazyRepo.select(selection).firstOrNull()

      assertNotNull(le)
      assertLazyEntity(le, selection)
    }
  }

  @Test
  fun `other fields on lazy embedded`() {
    transaction.executeWithoutResult {
      val embedded = SomeEmbedded("data", "lazyData")
      val lazyEmbedded = LazyEmbedded("data2", "lazyData2")
      lazyRepo.persist(LazyEntity(UUID.randomUUID(), "name", "lazyName", embedded, lazyEmbedded))

      val selection = LazyEntityDSL.select { lazyEmbedded {} }
      val le = lazyRepo.select(selection).firstOrNull()

      assertNotNull(le)
      assertLazyEntity(le, selection)
    }
  }

  private fun assertLazyEntity(lazyEntity: LazyEntity, selection: BeeSelection) {
    val name = lazyEntity.name
    assertNotNull(name)

    val lazyName = lazyEntity.lazyName
    if (selection.contains(LazyEntity::lazyName.name)) {
      assertNotNull(lazyName)
    } else {
      assertNull(lazyName)
    }

    val embedded = lazyEntity.embedded
    val embeddedSubselection = selection.subSelect(LazyEntity::embedded.name)
    if (embeddedSubselection != null) assertSomeEmbedded(embedded, embeddedSubselection)

    val lazyEmbedded = lazyEntity.lazyEmbedded
    if (selection.contains(LazyEntity::lazyEmbedded.name)) {
      assertNotNull(lazyEmbedded)
      val subSelection = selection.subSelect(LazyEntity::lazyEmbedded.name)
      if (subSelection != null) assertLazyEmbedded(lazyEmbedded, subSelection)
    } else {
      assertNull(lazyEmbedded)
    }
  }

  private fun assertSomeEmbedded(someEmbedded: SomeEmbedded, selection: BeeSelection) {
    val someData = someEmbedded.someData
    assertNotNull(someData)

    val someLazyData = someEmbedded.someLazyData
    if (selection.contains(SomeEmbedded::someLazyData.name)) {
      assertNotNull(someLazyData)
    } else {
      assertNull(someLazyData)
    }
  }

  private fun assertLazyEmbedded(lazyEmbedded: LazyEmbedded, selection: BeeSelection) {
    val lazyData = lazyEmbedded.lazyData
    assertNotNull(lazyData)

    val lazyLazyData = lazyEmbedded.lazyLazyData
    if (selection.contains(LazyEmbedded::lazyLazyData.name)) {
      assertNotNull(lazyLazyData)
    } else {
      assertNull(lazyLazyData)
    }
  }
}
