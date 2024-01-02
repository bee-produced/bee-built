package com.beeproduced.bee.persistent.test

import com.beeproduced.bee.persistent.application.Application
import com.beeproduced.bee.persistent.blaze.selection.BeeSelection
import com.beeproduced.datasource.b.*
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.*
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-02
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [Application::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BeePersistentTestB(
    @Qualifier("bEM")
    val em: EntityManager,
    @Qualifier("bTM")
    transactionManager: PlatformTransactionManager,
    @Autowired
    val composerRepository: ComposerRepository,
    @Autowired
    val containerRepository: ComposerContainerRepository
) {
    private val transaction = TransactionTemplate(transactionManager)

    @Test
    fun `empty selection`() {
        addComposers()
        transaction.executeWithoutResult {
            val selection = BeeSelection.create {  }
            val composers = composerRepository.select(selection)
            assertTrue { composers.count() == 2 }
            val c1 = composers[0] as? AiComposer
            assertNotNull(c1)
            assertNull(c1.aiData)
            val c2 = composers[1] as? HumanComposer
            assertNotNull(c2)
            assertNull(c2.humanData)
        }
    }

    @Test
    fun `full selection`() {
        addComposers()
        transaction.executeWithoutResult {
            val selection = BeeSelection.create {
                field("aiData") { field("id") }
                field("humanData") { field("id") }
            }
            val composers = composerRepository.select(selection)
            assertTrue { composers.count() == 2 }
            val c1 = composers[0] as? AiComposer
            assertNotNull(c1)
            assertNotNull(c1.aiData)
            val c2 = composers[1] as? HumanComposer
            assertNotNull(c2)
            assertNotNull(c2.humanData)
        }
    }

    @Test
    fun `container - empty selection`() {
        addComposers()
        transaction.executeWithoutResult {
            val selection = BeeSelection.create {  }
            val container = containerRepository.select(selection).firstOrNull()
            assertNotNull(container)
            assertNull(container.c1)
            assertNull(container.c2)
        }
    }

    @Test
    fun `container - full selection`() {
        addComposers()
        transaction.executeWithoutResult {
            val selection = BeeSelection.create {
                field("c1") {
                    field("aiData") { field("id") }
                    field("humanData") { field("id") }
                }
                field("c2") {
                    field("aiData") { field("id") }
                    field("humanData") { field("id") }
                }
            }
            val container = containerRepository.select(selection).firstOrNull()
            assertNotNull(container)
            val c1 = container.c1 as? AiComposer
            assertNotNull(c1)
            assertNotNull(c1.aiData)
            val c2 = container.c2 as? HumanComposer
            assertNotNull(c2)
            assertNotNull(c2.humanData)
        }
    }

    @Test
    fun `container - partial selection 1`() {
        addComposers()
        transaction.executeWithoutResult {
            val selection = BeeSelection.create {
                field("c1") {
                    field("aiData") { field("id") }
                    field("humanData") { field("id") }
                }
            }
            val container = containerRepository.select(selection).firstOrNull()
            assertNotNull(container)
            val c1 = container.c1 as? AiComposer
            assertNotNull(c1)
            assertNotNull(c1.aiData)
            assertNull(container.c2)
        }
    }

    @Test
    fun `container - partial selection 2`() {
        addComposers()
        transaction.executeWithoutResult {
            val selection = BeeSelection.create {
                field("c1") {
                    field("id")
                }
            }
            val container = containerRepository.select(selection).firstOrNull()
            assertNotNull(container)
            val c1 = container.c1 as? AiComposer
            assertNotNull(c1)
            assertNull(c1.aiData)
            assertNull(container.c2)
        }
    }


    @Test
    fun `container - partial selection 3`() {
        addComposers()
        transaction.executeWithoutResult {
            val selection = BeeSelection.create {
                field("c2") {
                    field("aiData") { field("id") }
                    field("humanData") { field("id") }
                }
            }
            val container = containerRepository.select(selection).firstOrNull()
            assertNotNull(container)
            assertNull(container.c1)
            val c2 = container.c2 as? HumanComposer
            assertNotNull(c2)
            assertNotNull(c2.humanData)
        }
    }

    @BeforeAll
    fun beforeAll() {
        clear()
    }

    @AfterEach
    fun afterEach() {
        clear()
    }

    fun addComposers() {
        transaction.executeWithoutResult {
            val aiData = em.beePersist(AiData(UUID.randomUUID(), "data"))
            val aiComposer = em.beePersist(AiComposer(UUID.randomUUID(), "AI", "GPT", AiParams("1", "2"), aiData.id))
            val humanData = em.beePersist(HumanData(UUID.randomUUID(), "Bar"))
            val humanComposer = em.beePersist(HumanComposer(UUID.randomUUID(), "Mario", "Mario", humanData.id))
            val container = em.beePersist(ComposerContainer(UUID.randomUUID(), aiComposer.id, null, humanComposer.id, null))
        }
    }

    fun clear() {
        transaction.executeWithoutResult {
            containerRepository.cbf.delete(em, ComposerContainer::class.java).executeUpdate()
            composerRepository.cbf.delete(em, Composer::class.java).executeUpdate()
        }
    }
}