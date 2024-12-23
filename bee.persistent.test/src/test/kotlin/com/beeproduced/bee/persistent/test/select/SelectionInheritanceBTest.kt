package com.beeproduced.bee.persistent.test.select

import com.beeproduced.bee.persistent.blaze.selection.BeeSelection
import com.beeproduced.bee.persistent.test.config.BTestConfig
import com.beeproduced.datasource.b.*
import com.beeproduced.datasource.b.dsl.ComposerContainerDSL
import com.beeproduced.datasource.b.dsl.ComposerDSL
import jakarta.persistence.EntityManager
import java.util.*
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
 * @version 2024-01-15
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [BTestConfig::class])
@TestPropertySource("classpath:application.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SelectionInheritanceBTest(
  @Qualifier("bEM") val em: EntityManager,
  @Qualifier("bTM") transactionManager: PlatformTransactionManager,
  @Autowired val composerRepo: ComposerRepository,
  @Autowired val containerRepo: ComposerContainerRepository,
  @Autowired val aiDataRepo: AiDataRepository,
  @Autowired val humanDataRepo: HumanDataRepository,
) {
  private val transaction = TransactionTemplate(transactionManager)

  @BeforeAll fun beforeAll() = clear()

  @AfterEach fun afterEach() = clear()

  fun clear() =
    transaction.executeWithoutResult {
      containerRepo.cbf.delete(em, ComposerContainer::class.java).executeUpdate()
      composerRepo.cbf.delete(em, Composer::class.java).executeUpdate()
      aiDataRepo.cbf.delete(em, AiData::class.java).executeUpdate()
      humanDataRepo.cbf.delete(em, HumanData::class.java).executeUpdate()
    }

  @Test
  fun `empty selection`() {
    addComposers()
    transaction.executeWithoutResult {
      val selection = BeeSelection.empty()
      val composers = composerRepo.select(selection)
      assertEquals(2, composers.count())
      val aiComposer = composers.first { it is AiComposer }
      assertComposer(aiComposer, selection)
      val humanComposer = composers.first { it is HumanComposer }
      assertComposer(humanComposer, selection)
    }
  }

  @Test
  fun `full selection`() {
    addComposers()
    transaction.executeWithoutResult {
      val selection =
        ComposerDSL.select {
          aiData {}
          humanData {}
        }
      val composers = composerRepo.select(selection)
      assertEquals(2, composers.count())
      val aiComposer = composers.first { it is AiComposer }
      assertComposer(aiComposer, selection)
      val humanComposer = composers.first { it is HumanComposer }
      assertComposer(humanComposer, selection)
    }
  }

  @Test
  fun `container - empty selection`() {
    addComposers()
    transaction.executeWithoutResult {
      val selection = BeeSelection.empty()
      val container = containerRepo.select(selection).firstOrNull()
      assertNotNull(container)
      assertContainer(container, selection)
    }
  }

  @Test
  fun `container - full selection`() {
    addComposers()
    transaction.executeWithoutResult {
      val selection =
        ComposerContainerDSL.select {
          c1 {
            aiData {}
            humanData {}
          }
          c2 {
            aiData {}
            humanData {}
          }
        }
      val container = containerRepo.select(selection).firstOrNull()
      assertNotNull(container)
      assertContainer(container, selection)
    }
  }

  @Test
  fun `container - partial selection`() {
    addComposers()
    transaction.executeWithoutResult {
      val selection =
        ComposerContainerDSL.select {
          c1 {
            aiData {}
            humanData {}
          }
        }
      val container = containerRepo.select(selection).firstOrNull()
      assertNotNull(container)
      assertContainer(container, selection)
    }
  }

  @Test
  fun `container - partial selection 2`() {
    addComposers()
    transaction.executeWithoutResult {
      val selection = ComposerContainerDSL.select { c1 {} }
      val container = containerRepo.select(selection).firstOrNull()
      assertNotNull(container)
      assertContainer(container, selection)
    }
  }

  @Test
  fun `container - partial selection 3`() {
    addComposers()
    transaction.executeWithoutResult {
      val selection =
        ComposerContainerDSL.select {
          c2 {
            aiData {}
            humanData {}
          }
        }
      val container = containerRepo.select(selection).firstOrNull()
      assertNotNull(container)
      assertContainer(container, selection)
    }
  }

  private fun addComposers() {
    transaction.executeWithoutResult {
      val aiData = aiDataRepo.persist(AiData(UUID.randomUUID(), "data"))
      val aiComposer =
        composerRepo.persist(
          AiComposer(UUID.randomUUID(), "AI", "GPT", AiParams("1", "2"), aiData.id)
        )
      val humanData = humanDataRepo.persist(HumanData(UUID.randomUUID(), "Bar"))
      val humanComposer =
        composerRepo.persist(HumanComposer(UUID.randomUUID(), "Mario", "Mario", humanData.id))
      val container =
        containerRepo.persist(
          ComposerContainer(UUID.randomUUID(), aiComposer.id, null, humanComposer.id, null)
        )
    }
  }

  private fun assertComposer(composer: Composer, selection: BeeSelection) {
    if (composer is AiComposer) {
      val aiData = composer.aiData
      if (selection.contains(AiComposer::aiData.name)) {
        assertNotNull(aiData)
      } else {
        assertNull(aiData)
      }
    } else if (composer is HumanComposer) {
      val humanData = composer.humanData
      if (selection.contains(HumanComposer::humanData.name)) {
        assertNotNull(humanData)
      } else {
        assertNull(humanData)
      }
    }
  }

  private fun assertContainer(container: ComposerContainer, selection: BeeSelection) {
    val c1 = container.c1
    if (selection.contains(ComposerContainer::c1.name)) {
      assertNotNull(c1)
      val subselect = selection.subSelect(ComposerContainer::c1.name)
      if (subselect != null) assertComposer(c1, subselect)
    } else {
      assertNull(c1)
    }
    val c2 = container.c2
    if (selection.contains(ComposerContainer::c2.name)) {
      assertNotNull(c2)
      val subselect = selection.subSelect(ComposerContainer::c2.name)
      if (subselect != null) assertComposer(c2, subselect)
    } else {
      assertNull(c2)
    }
  }
}
