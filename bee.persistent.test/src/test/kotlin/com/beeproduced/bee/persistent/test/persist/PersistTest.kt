package com.beeproduced.bee.persistent.test.persist

import com.beeproduced.bee.persistent.test.base.BaseTestConfig
import com.beeproduced.bee.persistent.test.beePersist
import com.beeproduced.datasource.test.persist.*
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-15
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [BaseTestConfig::class])
@TestPropertySource("classpath:application.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PersistTest(
    @Qualifier("testEM")
    val em: EntityManager,
    @Qualifier("testTM")
    transactionManager: PlatformTransactionManager,
    @Autowired
    val objectIdRepo: GeneratedObjectIdRepository,
    @Autowired
    val primitiveRepo: GeneratedPrimitiveIdRepository,
    @Autowired
    val inlineRepo: GeneratedInlineIdRepository
) {
    private val transaction = TransactionTemplate(transactionManager)

    @BeforeAll
    fun beforeAll() = clear()

    @AfterEach
    fun afterEach() = clear()

    fun clear() = transaction.executeWithoutResult {
        objectIdRepo.cbf.delete(em, GeneratedObjectId::class.java).executeUpdate()
        primitiveRepo.cbf.delete(em, GeneratedPrimitiveId::class.java).executeUpdate()
    }

    @Test
    fun `persist implementation details`() {
        assertThrows<Exception> {
            transaction.executeWithoutResult {
                // Different value than in constructor (-2 != -42)!
                val primitiveId2 = GeneratedPrimitiveId().copy(id = -42)
                val pstPrimitiveId2 = em.beePersist(primitiveId2)
                assertNotNull(pstPrimitiveId2.id)
            }
        }

        assertThrows<Exception> {
            transaction.executeWithoutResult {
                // Primitive field which cannot be set to null!
                val idField2 = GeneratedPrimitiveId::class.java.getDeclaredField("id").apply { isAccessible = true }
                val primitiveId4 = GeneratedPrimitiveId()
                idField2.set(primitiveId4, null)
                val pstPrimitiveId4 = em.beePersist(primitiveId4)
                assertNotNull(pstPrimitiveId4.id)
            }
        }

        transaction.executeWithoutResult {
            val idField = GeneratedObjectId::class.java.getDeclaredField("id").apply { isAccessible = true }
            val objectId = GeneratedObjectId()
            idField.set(objectId, null)
            val pstObjectId = em.beePersist(objectId)
            assertNotNull(pstObjectId.id)

            val primitiveId = GeneratedPrimitiveId()
            val pstPrimitiveId = em.beePersist(primitiveId)
            assertNotNull(pstPrimitiveId.id)

            val default = GeneratedPrimitiveId::class.java
                .getConstructor()
                .newInstance()
            val primitiveId3 = GeneratedPrimitiveId().copy(id = -42)
            val pstPrimitiveId3 = em.beePersist(primitiveId3.copy(id = default.id))
            assertNotNull(pstPrimitiveId3.id)
        }
    }

    @Test
    fun `persist with primitive id`() {
        transaction.executeWithoutResult {
            val primitiveId = GeneratedPrimitiveId()
            val pstPrimitiveId = primitiveRepo.persist(primitiveId)
            assertNotNull(pstPrimitiveId.id)
        }
    }

    @Test
    fun `persist with primitive id with non default value`() {
        transaction.executeWithoutResult {
            val primitiveId = GeneratedPrimitiveId().copy(id = -42)
            val pstPrimitiveId = primitiveRepo.persist(primitiveId)
            assertNotNull(pstPrimitiveId.id)
        }
    }

    @Test
    fun `persist with object id`() {
        transaction.executeWithoutResult {
            val objectId = GeneratedObjectId()
            val pstObjectId = objectIdRepo.persist(objectId)
            assertNotNull(pstObjectId)
        }
    }


    @Test
    fun `persist with inline value id`() {
        transaction.executeWithoutResult {
            val inlineId = GeneratedInlineId(SomeInlineId(-1))
            val pstInlinedId = inlineRepo.persist(inlineId)
            assertNotNull(pstInlinedId)
        }
    }
}