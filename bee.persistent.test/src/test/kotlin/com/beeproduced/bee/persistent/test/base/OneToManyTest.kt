package com.beeproduced.bee.persistent.test.base

import com.beeproduced.bee.persistent.blaze.selection.BeeSelection
import com.beeproduced.datasource.test.dsl.WorkCollectionDSL
import com.beeproduced.datasource.test.onetomany.*
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
import kotlin.test.*

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
class OneToManyTest(
    @Qualifier("testEM")
    val em: EntityManager,
    @Qualifier("testTM")
    transactionManager: PlatformTransactionManager,
    @Autowired
    val workRepo: WorkRepository,
    @Autowired
    val collectionRepo: WorkCollectionRepository
) {
    private val transaction = TransactionTemplate(transactionManager)

    @BeforeAll
    fun beforeAll() = clear()

    @AfterEach
    fun afterEach() = clear()

    fun clear() = transaction.executeWithoutResult {
        workRepo.cbf.delete(em, Work::class.java).executeUpdate()
        collectionRepo.cbf.delete(em, WorkCollection::class.java).executeUpdate()
    }

    // Embedded properties cannot use generated values
    // https://stackoverflow.com/a/66377401/12347616
    var workIdCount : Long = 0

    @Test
    fun `select with relation loaded`() {
        var collectionId: Long = -1
        var workId1 = WorkId()
        var workId2 = WorkId()

        transaction.executeWithoutResult {
            val collection = collectionRepo.persist(WorkCollection())
            collectionId = collection.id
            val work1 = workRepo.persist(Work(WorkId(++workIdCount, collectionId), "Hey!"))
            workId1 = work1.id
            val work2 = workRepo.persist(Work(WorkId(++workIdCount, collectionId), "Moin!"))
            workId2 = work2.id
        }

        transaction.executeWithoutResult {
            val selection = WorkCollectionDSL.select {
                this.works { this.workCollection { this.works {  } } }
            }

            // TODO: Replace with selectById
            val collection = collectionRepo.select(selection) {
                where(WorkCollectionDSL.id.eq(collectionId))
            }.firstOrNull()

            assertNotNull(collection)
            assertWorkCollection(collection, collectionId, setOf(workId1, workId2), 2)
        }
    }

    @Test
    fun `select with relation not loaded`() {
        var collectionId: Long = -1
        var workId1 = WorkId()
        var workId2 = WorkId()

        transaction.executeWithoutResult {
            val collection = collectionRepo.persist(WorkCollection())
            collectionId = collection.id
            val work1 = workRepo.persist(Work(WorkId(++workIdCount, collectionId), "Hey!"))
            workId1 = work1.id
            val work2 = workRepo.persist(Work(WorkId(++workIdCount, collectionId), "Moin!"))
            workId2 = work2.id
        }

        transaction.executeWithoutResult {
            val selection = BeeSelection.empty()

            // TODO: Replace with selectById
            val collection = collectionRepo.select(selection) {
                where(WorkCollectionDSL.id.eq(collectionId))
            }.firstOrNull()

            assertNotNull(collection)
            assertWorkCollection(collection, collectionId, setOf(workId1, workId2), 0)
        }
    }

    @Test
    fun `select with joined where clause`() {
        var collectionId1: Long = -1
        var collectionId2: Long = -1
        var workId1 = WorkId()
        var workId2 = WorkId()

        transaction.executeWithoutResult {
            val collection1 = collectionRepo.persist(WorkCollection())
            collectionId1 = collection1.id
            val collection2 = collectionRepo.persist(WorkCollection())
            collectionId2 = collection2.id
            val work1 = workRepo.persist(Work(WorkId(++workIdCount, collectionId1), "Hey!"))
            workId1 = work1.id
            val work2 = workRepo.persist(Work(WorkId(++workIdCount, collectionId2), "Moin!"))
            workId2 = work2.id
        }

        transaction.executeWithoutResult {
            val selection = WorkCollectionDSL.select {
                this.works { this.workCollection { this.works {  } } }
            }

            // TODO: Replace with selectById
            val collection = collectionRepo.select(selection) {
                where(WorkCollectionDSL.works.id.eq(workId2))
            }.firstOrNull()

            assertNotNull(collection)
            assertWorkCollection(collection, collectionId2, setOf(workId2), 2)
        }
    }

    @Test
    fun `select by ids with relation loaded`() {
        var collectionId1: Long = -1
        var collectionId2: Long = -1
        var workId1 = WorkId()
        var workId2 = WorkId()

        transaction.executeWithoutResult {
            val collection1 = collectionRepo.persist(WorkCollection())
            collectionId1 = collection1.id
            val collection2 = collectionRepo.persist(WorkCollection())
            collectionId2 = collection2.id
            val work1 = workRepo.persist(Work(WorkId(++workIdCount, collectionId1), "Hey!"))
            workId1 = work1.id
            val work2 = workRepo.persist(Work(WorkId(++workIdCount, collectionId2), "Moin!"))
            workId2 = work2.id
        }

        transaction.executeWithoutResult {
            val selection = WorkCollectionDSL.select {
                this.works { this.workCollection { this.works {  } } }
            }

            // TODO: Replace with selectByIds
            val collections = collectionRepo.select(selection) {
                whereOr(
                    WorkCollectionDSL.id.eq(collectionId1),
                    WorkCollectionDSL.id.eq(collectionId2)
                )
            }

            assertEquals(2, collections.count())
            val collection1 = collections.first { it.id == collectionId1 }
            val collection2 = collections.first { it.id == collectionId2 }
            assertWorkCollection(collection1, collectionId1, setOf(workId1), 2)
            assertWorkCollection(collection2, collectionId2, setOf(workId2), 2)
        }
    }

    private fun assertWorkCollection(
        workCollection: WorkCollection,
        collectionId: Long, workIds: Set<WorkId>, depth: Int
    ) {
        assertEquals(workCollection.id, collectionId)
        val works = workCollection.works
        if (depth == 0) {
            assertTrue { works.isNullOrEmpty() }
            return
        }

        // TODO: Investigate in the future: Not loaded relations are in some cases empty collections instead of `null`
        assertNotNull(works)
        assertEquals(workIds.count(), works.count())
        for (work in works) {
            workIds.contains(work.id)
            val newDepth = depth - 1
            val newWorkCollection = work.workCollection
            if (newDepth != 0) {
                assertNotNull(newWorkCollection)
                assertWorkCollection(newWorkCollection, collectionId, workIds, newDepth)
            } else {
                assertNull(newWorkCollection)
            }

        }
    }


}