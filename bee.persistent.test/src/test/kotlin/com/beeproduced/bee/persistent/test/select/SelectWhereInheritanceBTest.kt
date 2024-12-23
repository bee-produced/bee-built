package com.beeproduced.bee.persistent.test.select

import com.beeproduced.bee.persistent.blaze.selection.BeeSelection
import com.beeproduced.bee.persistent.test.config.BTestConfig
import com.beeproduced.datasource.b.*
import com.beeproduced.datasource.b.dsl.ComposerContainerDSL
import com.beeproduced.datasource.b.dsl.ComposerDSL
import jakarta.persistence.EntityManager
import java.util.*
import kotlin.test.*
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
@SpringBootTest(classes = [BTestConfig::class])
@TestPropertySource("classpath:application.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SelectWhereInheritanceBTest(
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
  fun `where with treat`() {
    transaction.executeWithoutResult {
      val aiData1 = aiDataRepo.persist(AiData(UUID.randomUUID(), "data"))
      val aiComposer1 =
        composerRepo.persist(
          AiComposer(UUID.randomUUID(), "AI", "GPT", AiParams("1", "2"), aiData1.id)
        )
      val aiData2 = aiDataRepo.persist(AiData(UUID.randomUUID(), "data2"))
      val aiComposer2 =
        composerRepo.persist(
          AiComposer(UUID.randomUUID(), "AI2", "GPT2", AiParams("3", "4"), aiData2.id)
        )
      val humanData1 = humanDataRepo.persist(HumanData(UUID.randomUUID(), "Bar"))
      val humanComposer1 =
        composerRepo.persist(HumanComposer(UUID.randomUUID(), "Mario", "Mario", humanData1.id))

      val selection =
        ComposerDSL.select {
          aiData {}
          humanData {}
        }
      val composers = composerRepo.select(selection) { where(ComposerDSL.aiDataId.eq(aiData2.id)) }

      assertEquals(1, composers.count())
      val composer = composers.first()
      assertEquals(aiComposer2.id, composer.id)
      assertTrue { composer is AiComposer }
      assertComposer(composer, selection)
    }
  }

  @Test
  fun `where with treat on relation`() {
    transaction.executeWithoutResult {
      val aiData1 = aiDataRepo.persist(AiData(UUID.randomUUID(), "data"))
      val aiComposer1 =
        composerRepo.persist(
          AiComposer(UUID.randomUUID(), "AI", "GPT", AiParams("1", "2"), aiData1.id)
        )
      val aiData2 = aiDataRepo.persist(AiData(UUID.randomUUID(), "data2"))
      val aiComposer2 =
        composerRepo.persist(
          AiComposer(UUID.randomUUID(), "AI2", "GPT2", AiParams("3", "4"), aiData2.id)
        )
      val humanData1 = humanDataRepo.persist(HumanData(UUID.randomUUID(), "Bar"))
      val humanComposer1 =
        composerRepo.persist(HumanComposer(UUID.randomUUID(), "Mario", "Mario", humanData1.id))

      val selection =
        ComposerDSL.select {
          aiData {}
          humanData {}
        }
      val composers =
        composerRepo.select(selection) { where(ComposerDSL.aiData.data.eq(aiData2.data)) }

      assertEquals(1, composers.count())
      val composer = composers.first()
      assertEquals(aiComposer2.id, composer.id)
      assertTrue { composer is AiComposer }
      assertComposer(composer, selection)
    }
  }

  @Test
  fun `where with treat on not loaded relation`() {
    transaction.executeWithoutResult {
      val aiData1 = aiDataRepo.persist(AiData(UUID.randomUUID(), "data"))
      val aiComposer1 =
        composerRepo.persist(
          AiComposer(UUID.randomUUID(), "AI", "GPT", AiParams("1", "2"), aiData1.id)
        )
      val aiData2 = aiDataRepo.persist(AiData(UUID.randomUUID(), "data2"))
      val aiComposer2 =
        composerRepo.persist(
          AiComposer(UUID.randomUUID(), "AI2", "GPT2", AiParams("3", "4"), aiData2.id)
        )
      val humanData1 = humanDataRepo.persist(HumanData(UUID.randomUUID(), "Bar"))
      val humanComposer1 =
        composerRepo.persist(HumanComposer(UUID.randomUUID(), "Mario", "Mario", humanData1.id))

      val selection = BeeSelection.empty()
      val composers =
        composerRepo.select(selection) { where(ComposerDSL.aiData.data.eq(aiData2.data)) }

      assertEquals(1, composers.count())
      val composer = composers.first()
      assertEquals(aiComposer2.id, composer.id)
      assertTrue { composer is AiComposer }
      assertComposer(composer, selection)
    }
  }

  @Test
  fun `where with treat on embedded`() {
    transaction.executeWithoutResult {
      val aiData1 = aiDataRepo.persist(AiData(UUID.randomUUID(), "data"))
      val aiComposer1 =
        composerRepo.persist(
          AiComposer(UUID.randomUUID(), "AI", "GPT", AiParams("1", "2"), aiData1.id)
        )
      val aiData2 = aiDataRepo.persist(AiData(UUID.randomUUID(), "data2"))
      val aiComposer2 =
        composerRepo.persist(
          AiComposer(UUID.randomUUID(), "AI2", "GPT2", AiParams("3", "4"), aiData2.id)
        )
      val humanData1 = humanDataRepo.persist(HumanData(UUID.randomUUID(), "Bar"))
      val humanComposer1 =
        composerRepo.persist(HumanComposer(UUID.randomUUID(), "Mario", "Mario", humanData1.id))

      val selection =
        ComposerDSL.select {
          aiData {}
          humanData {}
        }
      val composers =
        composerRepo.select(selection) { where(ComposerDSL.params.eq(AiParams("3", "4"))) }

      assertEquals(1, composers.count())
      val composer = composers.first()
      assertEquals(aiComposer2.id, composer.id)
      assertTrue { composer is AiComposer }
      assertComposer(composer, selection)
    }
  }

  @Test
  fun `where with treat on embedded field`() {
    transaction.executeWithoutResult {
      val aiData1 = aiDataRepo.persist(AiData(UUID.randomUUID(), "data"))
      val aiComposer1 =
        composerRepo.persist(
          AiComposer(UUID.randomUUID(), "AI", "GPT", AiParams("1", "2"), aiData1.id)
        )
      val aiData2 = aiDataRepo.persist(AiData(UUID.randomUUID(), "data2"))
      val aiComposer2 =
        composerRepo.persist(
          AiComposer(UUID.randomUUID(), "AI2", "GPT2", AiParams("3", "4"), aiData2.id)
        )
      val humanData1 = humanDataRepo.persist(HumanData(UUID.randomUUID(), "Bar"))
      val humanComposer1 =
        composerRepo.persist(HumanComposer(UUID.randomUUID(), "Mario", "Mario", humanData1.id))

      val selection =
        ComposerDSL.select {
          aiData {}
          humanData {}
        }
      val composers = composerRepo.select(selection) { where(ComposerDSL.paramsZ1.eq("3")) }

      assertEquals(1, composers.count())
      val composer = composers.first()
      assertEquals(aiComposer2.id, composer.id)
      assertTrue { composer is AiComposer }
      assertComposer(composer, selection)
    }
  }

  @Test
  fun `container - where with treat`() {
    transaction.executeWithoutResult {
      val aiData1 = aiDataRepo.persist(AiData(UUID.randomUUID(), "data"))
      val aiComposer1 =
        composerRepo.persist(
          AiComposer(UUID.randomUUID(), "AI", "GPT", AiParams("1", "2"), aiData1.id)
        )
      val aiData2 = aiDataRepo.persist(AiData(UUID.randomUUID(), "data2"))
      val aiComposer2 =
        composerRepo.persist(
          AiComposer(UUID.randomUUID(), "AI2", "GPT2", AiParams("3", "4"), aiData2.id)
        )
      val humanData1 = humanDataRepo.persist(HumanData(UUID.randomUUID(), "Bar"))
      val humanComposer1 =
        composerRepo.persist(HumanComposer(UUID.randomUUID(), "Mario", "Mario", humanData1.id))
      val container1 =
        containerRepo.persist(
          ComposerContainer(UUID.randomUUID(), aiComposer1.id, null, humanComposer1.id, null)
        )
      val container2 =
        containerRepo.persist(
          ComposerContainer(UUID.randomUUID(), aiComposer2.id, null, humanComposer1.id, null)
        )

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
      val containers =
        containerRepo.select(selection) { where(ComposerContainerDSL.c1.aiDataId.eq(aiData2.id)) }

      assertEquals(1, containers.count())
      val container = containers.first()
      assertEquals(container2.id, container.id)
      assertContainer(container, selection)
    }
  }

  @Test
  fun `container - where with treat on relation`() {
    transaction.executeWithoutResult {
      val aiData1 = aiDataRepo.persist(AiData(UUID.randomUUID(), "data"))
      val aiComposer1 =
        composerRepo.persist(
          AiComposer(UUID.randomUUID(), "AI", "GPT", AiParams("1", "2"), aiData1.id)
        )
      val aiData2 = aiDataRepo.persist(AiData(UUID.randomUUID(), "data2"))
      val aiComposer2 =
        composerRepo.persist(
          AiComposer(UUID.randomUUID(), "AI2", "GPT2", AiParams("3", "4"), aiData2.id)
        )
      val humanData1 = humanDataRepo.persist(HumanData(UUID.randomUUID(), "Bar"))
      val humanComposer1 =
        composerRepo.persist(HumanComposer(UUID.randomUUID(), "Mario", "Mario", humanData1.id))
      val container1 =
        containerRepo.persist(
          ComposerContainer(UUID.randomUUID(), aiComposer1.id, null, humanComposer1.id, null)
        )
      val container2 =
        containerRepo.persist(
          ComposerContainer(UUID.randomUUID(), aiComposer2.id, null, humanComposer1.id, null)
        )

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
      val containers =
        containerRepo.select(selection) {
          where(ComposerContainerDSL.c1.aiData.data.eq(aiData2.data))
        }

      assertEquals(1, containers.count())
      val container = containers.first()
      assertEquals(container2.id, container.id)
      assertContainer(container, selection)
    }
  }

  @Test
  fun `container - where with treat on relation (not loaded)`() {
    transaction.executeWithoutResult {
      val aiData1 = aiDataRepo.persist(AiData(UUID.randomUUID(), "data"))
      val aiComposer1 =
        composerRepo.persist(
          AiComposer(UUID.randomUUID(), "AI", "GPT", AiParams("1", "2"), aiData1.id)
        )
      val aiData2 = aiDataRepo.persist(AiData(UUID.randomUUID(), "data2"))
      val aiComposer2 =
        composerRepo.persist(
          AiComposer(UUID.randomUUID(), "AI2", "GPT2", AiParams("3", "4"), aiData2.id)
        )
      val humanData1 = humanDataRepo.persist(HumanData(UUID.randomUUID(), "Bar"))
      val humanComposer1 =
        composerRepo.persist(HumanComposer(UUID.randomUUID(), "Mario", "Mario", humanData1.id))
      val container1 =
        containerRepo.persist(
          ComposerContainer(UUID.randomUUID(), aiComposer1.id, null, humanComposer1.id, null)
        )
      val container2 =
        containerRepo.persist(
          ComposerContainer(UUID.randomUUID(), aiComposer2.id, null, humanComposer1.id, null)
        )

      val selection = BeeSelection.empty()
      val containers =
        containerRepo.select(selection) {
          where(ComposerContainerDSL.c1.aiData.data.eq(aiData2.data))
        }

      assertEquals(1, containers.count())
      val container = containers.first()
      assertEquals(container2.id, container.id)
      assertContainer(container, selection)
    }
  }

  @Test
  fun `container - where with treat on embedded`() {
    transaction.executeWithoutResult {
      val aiData1 = aiDataRepo.persist(AiData(UUID.randomUUID(), "data"))
      val aiComposer1 =
        composerRepo.persist(
          AiComposer(UUID.randomUUID(), "AI", "GPT", AiParams("1", "2"), aiData1.id)
        )
      val aiData2 = aiDataRepo.persist(AiData(UUID.randomUUID(), "data2"))
      val aiComposer2 =
        composerRepo.persist(
          AiComposer(UUID.randomUUID(), "AI2", "GPT2", AiParams("3", "4"), aiData2.id)
        )
      val humanData1 = humanDataRepo.persist(HumanData(UUID.randomUUID(), "Bar"))
      val humanComposer1 =
        composerRepo.persist(HumanComposer(UUID.randomUUID(), "Mario", "Mario", humanData1.id))
      val container1 =
        containerRepo.persist(
          ComposerContainer(UUID.randomUUID(), aiComposer1.id, null, humanComposer1.id, null)
        )
      val container2 =
        containerRepo.persist(
          ComposerContainer(UUID.randomUUID(), aiComposer2.id, null, humanComposer1.id, null)
        )

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
      val containers =
        containerRepo.select(selection) {
          where(ComposerContainerDSL.c1.params.eq(AiParams("3", "4")))
        }

      assertEquals(1, containers.count())
      val container = containers.first()
      assertEquals(container2.id, container.id)
      assertContainer(container, selection)
    }
  }

  @Test
  fun `container - where with treat on embedded field`() {
    transaction.executeWithoutResult {
      val aiData1 = aiDataRepo.persist(AiData(UUID.randomUUID(), "data"))
      val aiComposer1 =
        composerRepo.persist(
          AiComposer(UUID.randomUUID(), "AI", "GPT", AiParams("1", "2"), aiData1.id)
        )
      val aiData2 = aiDataRepo.persist(AiData(UUID.randomUUID(), "data2"))
      val aiComposer2 =
        composerRepo.persist(
          AiComposer(UUID.randomUUID(), "AI2", "GPT2", AiParams("3", "4"), aiData2.id)
        )
      val humanData1 = humanDataRepo.persist(HumanData(UUID.randomUUID(), "Bar"))
      val humanComposer1 =
        composerRepo.persist(HumanComposer(UUID.randomUUID(), "Mario", "Mario", humanData1.id))
      val container1 =
        containerRepo.persist(
          ComposerContainer(UUID.randomUUID(), aiComposer1.id, null, humanComposer1.id, null)
        )
      val container2 =
        containerRepo.persist(
          ComposerContainer(UUID.randomUUID(), aiComposer2.id, null, humanComposer1.id, null)
        )

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
      val containers =
        containerRepo.select(selection) { where(ComposerContainerDSL.c1.paramsZ1.eq("3")) }

      assertEquals(1, containers.count())
      val container = containers.first()
      assertEquals(container2.id, container.id)
      assertContainer(container, selection)
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
