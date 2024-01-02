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
        }
    }

    fun clear() {
        transaction.executeWithoutResult {
            composerRepository.cbf.delete(em, Composer::class.java).executeUpdate()
            containerRepository.cbf.delete(em, ComposerContainer::class.java).executeUpdate()
        }
    }
}