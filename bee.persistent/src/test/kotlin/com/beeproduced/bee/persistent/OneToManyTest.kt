package com.beeproduced.bee.persistent

import com.beeproduced.bee.persistent.selection.EmptySelection
import com.beeproduced.bee.persistent.selection.FullNonRecursiveSelection
import com.beeproduced.bee.persistent.selection.SimpleSelection
import com.beeproduced.bee.persistent.config.DummyApplication
import com.beeproduced.bee.persistent.config.PersistenceConfiguration
import com.beeproduced.bee.persistent.one.to.many.*
import com.beeproduced.bee.persistent.one.to.one.*
import com.beeproduced.bee.persistent.many.to.many.*
import com.linecorp.kotlinjdsl.querydsl.expression.column
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Test
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
import kotlin.test.*

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-02-07
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [DummyApplication::class])
@TestPropertySource("classpath:application.properties")
@ContextConfiguration(classes = [OneToManyTest.TestConfig::class, PersistenceConfiguration::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OneToManyTest(
    @Autowired
    val workRepo: WorkRepository,
    @Autowired
    val collectionRepo: WorkCollectionRepository,
    @Qualifier("orderTransactionManager")
    transactionManager: PlatformTransactionManager,
    @Qualifier("orderEntityManager")
    val em: EntityManager
) {
    private val transaction = TransactionTemplate(transactionManager)

    @Configuration
    class TestConfig(val em: EntityManager) {
        @Bean
        fun workRepository(): WorkRepository = WorkRepository(em)

        @Bean
        fun collectionRepository(): WorkCollectionRepository = WorkCollectionRepository(em)
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
            workRepo.deleteAll()
            collectionRepo.deleteAll()
        }
    }

    // @Test
    // fun exp() {
    //     println("here")
    //
    //     // https://stackoverflow.com/a/3335062/12347616
    //     val util = em.entityManagerFactory.persistenceUnitUtil
    //     val work = Work(1, 1, "")
    //
    //     val id = util.getIdentifier(work)
    //     println(id)
    //     println(id.javaClass)
    //
    //     val collection = WorkCollection(2, null)
    //     val collId = util.getIdentifier(collection)
    //     println(collId)
    //
    //     // Directly without session
    //     // Reversed from source code
    //     val metamodel = em.entityManagerFactory.metamodel as MetamodelImplementor
    //     val workPersister = metamodel.entityPersister(Work::class.java)
    //
    //     val workId = workPersister.getIdentifier(work, null)
    //     println(workId)
    //
    //     val collectionPersister = metamodel.entityPersister(WorkCollection::class.java)
    //     val collectionId = collectionPersister.getIdentifier(collection, null)
    //     println(collectionId)
    //
    //     println("here!")
    // }

    @Test
    fun `select with relation loaded`() {

        var collectionId: Long = -1
        var work1Id: Long
        var work2Id: Long

        transaction.executeWithoutResult {
            val collection = collectionRepo.persist(WorkCollection())
            collectionId = collection.id
            val work1 = workRepo.persist(Work(collectionId, "Hey!"))
            work1Id = work1.id
            val work2 = workRepo.persist(Work(collectionId, "Moin!"))
            work2Id = work2.id
        }

        val selection = SimpleSelection(setOf(SimpleSelection.FieldNode(WorkCollection::works.name)))
        val collection = collectionRepo.selectById(collectionId, selection)

        assertNotNull(collection)
        assertNotNull(collection.works)
        assertEquals(2, collection.works?.count())
    }

    @Test
    fun `select with relation not loaded`() {

        var collectionId: Long = -1
        var work1Id: Long
        var work2Id: Long

        transaction.executeWithoutResult {
            val collection = collectionRepo.persist(WorkCollection())
            collectionId = collection.id
            val work1 = workRepo.persist(Work(collectionId, "Hey!"))
            work1Id = work1.id
            val work2 = workRepo.persist(Work(collectionId, "Moin!"))
            work2Id = work2.id
        }

        val selection = EmptySelection()
        val collection = collectionRepo.selectById(collectionId, selection)

        assertNotNull(collection)
        assertNull(collection.works)
    }

    @Test
    fun `select with joined where clause`() {
        var collection1Id: Long = -1
        var collection2Id: Long = -1
        var work1Id: Long = -1
        var work2Id: Long = -1

        transaction.executeWithoutResult {
            val collection1 = collectionRepo.persist(WorkCollection())
            collection1Id = collection1.id
            val collection2 = collectionRepo.persist(WorkCollection())
            collection2Id = collection2.id
            val work1 = workRepo.persist(Work(collection1Id, "Hey!"))
            work1Id = work1.id
            val work2 = workRepo.persist(Work(collection2Id, "Moin!"))
            work2Id = work2.id
        }

        val collectionWithWork2 = collectionRepo.select(FullNonRecursiveSelection()) {
            join(entity(Work::class), on(column(WorkCollection::id).equal(column(Work::worksKey))))
            where(column(Work::id).equal(work2Id))
            limit(1)
        }.firstOrNull()

        assertNotNull(collectionWithWork2)
        assertEquals(collection2Id, collectionWithWork2.id)
        assertTrue { collectionWithWork2.works?.first()?.id == work2Id }
    }

    @Test
    fun `select by ids with relation loaded`() {

        var collection1Id: Long = -1
        var collection2Id: Long = -1
        var work1Id: Long
        var work2Id: Long

        transaction.executeWithoutResult {
            val collection1 = collectionRepo.persist(WorkCollection())
            collection1Id = collection1.id
            val collection2 = collectionRepo.persist(WorkCollection())
            collection2Id = collection2.id
            val work1 = workRepo.persist(Work(collection1Id, "Hey!"))
            work1Id = work1.id
            val work2 = workRepo.persist(Work(collection2Id, "Moin!"))
            work2Id = work2.id
        }

        val selection = SimpleSelection(setOf(SimpleSelection.FieldNode(WorkCollection::works.name)))
        val collections = collectionRepo.selectByIds(
            listOf(collection1Id, collection2Id), selection
        )

        assertEquals(2, collections.count())
        val collection1 = collections[0]
        val collection2 = collections[1]
        assertNotNull(collection1.works)
        assertEquals(1, collection1.works?.count())
        assertNotNull(collection2.works)
        assertEquals(1, collection2.works?.count())
    }

    @Test
    fun `persist all and select with relation loaded`() {
        var collection1Id: Long = -1
        var collection2Id: Long = -1
        var work1Id: Long
        var work2Id: Long

        transaction.executeWithoutResult {
            val collection1 = collectionRepo.persist(WorkCollection())
            collection1Id = collection1.id
            val collection2 = collectionRepo.persist(WorkCollection())
            collection2Id = collection2.id

            val selection = SimpleSelection(setOf(SimpleSelection.FieldNode(Work::works.name)))
            val works = workRepo.persistAllAndSelect(
                listOf(
                    Work(collection1Id, "Hey!"),
                    Work(collection2Id, "Moin!")
                ), selection
            )
            assertEquals(2, works.count())

            val work1 = works.getOrNull(0)
            val work2 = works.getOrNull(1)
            assertNotNull(work1)
            assertNotNull(work1.works)
            assertNotNull(work2)
            assertNotNull(work2.works)

        }
    }

    @Test
    @Ignore("Single relations are loaded eagerly since Hibernate 6")
    fun `persist all and select with relation not loaded`() {
        var collection1Id: Long = -1
        var collection2Id: Long = -1
        var work1Id: Long
        var work2Id: Long

        transaction.executeWithoutResult {
            val collection1 = collectionRepo.persist(WorkCollection())
            collection1Id = collection1.id
            val collection2 = collectionRepo.persist(WorkCollection())
            collection2Id = collection2.id

            val works = workRepo.persistAllAndSelect(
                listOf(
                    Work(collection1Id, "Hey!"),
                    Work(collection2Id, "Moin!")
                )
            )
            assertEquals(2, works.count())

            val work1 = works.getOrNull(0)
            val work2 = works.getOrNull(1)
            assertNotNull(work1)
            assertNull(work1.works)
            assertNotNull(work2)
            assertNull(work2.works)
        }
    }

    @Test
    fun exists() {
        var collection1Id: Long = -1
        var collection2Id: Long = -1
        var collection3Id: Long = -1

        transaction.executeWithoutResult {
            val collection1 = collectionRepo.persist(WorkCollection())
            collection1Id = collection1.id
            val collection2 = collectionRepo.persist(WorkCollection())
            collection2Id = collection2.id
            val collection3 = collectionRepo.persist(WorkCollection())
            collection3Id = collection3.id
        }

        var collections = collectionRepo.select()
        assertEquals(3, collections.count())

        assertTrue(collectionRepo.exists(collection1Id))
        assertTrue(collectionRepo.exists(collection2Id))
        assertTrue(collectionRepo.exists(collection3Id))
        assertFalse(collectionRepo.exists(-1))
    }

    @Test
    fun `exists with composite key`() {
        var collection1Id: Long = -1
        var collection2Id: Long = -1
        var collection3Id: Long = -1
        var work1Id: Long = -1
        var work2Id: Long = -1
        var work3Id: Long = -1

        transaction.executeWithoutResult {
            val collection1 = collectionRepo.persist(WorkCollection())
            collection1Id = collection1.id
            val collection2 = collectionRepo.persist(WorkCollection())
            collection2Id = collection2.id
            val collection3 = collectionRepo.persist(WorkCollection())
            collection3Id = collection3.id
            val work1 = workRepo.persist(Work(collection1Id, "Hey!"))
            work1Id = work1.id
            val work2 = workRepo.persist(Work(collection2Id, "Moin!"))
            work2Id = work2.id
            val work3 = workRepo.persist(Work(collection3Id, "Hi!"))
            work3Id = work3.id
        }

        var works = workRepo.select()
        assertEquals(3, works.count())

        assertTrue(workRepo.exists(WorkId(work1Id, collection1Id)))
        assertTrue(workRepo.exists(WorkId(work2Id, collection2Id)))
        assertTrue(workRepo.exists(WorkId(work3Id, collection3Id)))
        assertFalse(workRepo.exists(WorkId(-1, -1)))
    }

    @Test
    fun `exists with multiple ids`() {
        var collection1Id: Long = -1
        var collection2Id: Long = -1
        var collection3Id: Long = -1

        transaction.executeWithoutResult {
            val collection1 = collectionRepo.persist(WorkCollection())
            collection1Id = collection1.id
            val collection2 = collectionRepo.persist(WorkCollection())
            collection2Id = collection2.id
            val collection3 = collectionRepo.persist(WorkCollection())
            collection3Id = collection3.id
        }

        var collections = collectionRepo.select()
        assertEquals(3, collections.count())

        assertTrue(
            collectionRepo.existsAll(
                listOf(
                    collection1Id, collection2Id, collection3Id
                )
            )
        )
        assertFalse(
            collectionRepo.existsAll(
                listOf(
                    collection1Id, collection2Id, collection3Id, -1
                )
            )
        )
    }

    @Test
    fun `exists with multiple composite keys`() {
        var collection1Id: Long = -1
        var collection2Id: Long = -1
        var collection3Id: Long = -1
        var work1Id: Long = -1
        var work2Id: Long = -1
        var work3Id: Long = -1

        transaction.executeWithoutResult {
            val collection1 = collectionRepo.persist(WorkCollection())
            collection1Id = collection1.id
            val collection2 = collectionRepo.persist(WorkCollection())
            collection2Id = collection2.id
            val collection3 = collectionRepo.persist(WorkCollection())
            collection3Id = collection3.id
            val work1 = workRepo.persist(Work(collection1Id, "Hey!"))
            work1Id = work1.id
            val work2 = workRepo.persist(Work(collection2Id, "Moin!"))
            work2Id = work2.id
            val work3 = workRepo.persist(Work(collection3Id, "Hi!"))
            work3Id = work3.id
        }

        var works = workRepo.select()
        assertEquals(3, works.count())

        assertTrue(
            workRepo.existsAll(
                listOf(
                    WorkId(work1Id, collection1Id),
                    WorkId(work2Id, collection2Id),
                    WorkId(work3Id, collection3Id)
                )
            )
        )
        assertFalse(
            workRepo.existsAll(
                listOf(
                    WorkId(work1Id, collection1Id),
                    WorkId(work2Id, collection2Id),
                    WorkId(work3Id, collection3Id),
                    WorkId(-1, -1)
                )
            )
        )
    }

    @Test
    fun `update field`() {
        var collectionId: Long = -1
        var workId: Long = -1

        transaction.executeWithoutResult {
            val collection = collectionRepo.persist(WorkCollection())
            collectionId = collection.id
            val work = workRepo.persist(Work(collectionId, "Hey!"))
            workId = work.id
        }

        transaction.executeWithoutResult {
            // WorkId = composite key
            val work = workRepo.selectById(WorkId(workId, collectionId))
            assertNotNull(work)
            val workUpdate = work.copy(txt = "Update!")
            workRepo.update(workUpdate)
        }

        val workUpdate = workRepo.selectById(WorkId(workId, collectionId))

        assertNotNull(workUpdate)
        assertEquals("Update!", workUpdate.txt)
    }

    @Test
    fun `delete all`() {
        var collection1Id: Long = -1
        var collection2Id: Long = -1

        transaction.executeWithoutResult {
            val collection1 = collectionRepo.persist(WorkCollection())
            collection1Id = collection1.id
            val collection2 = collectionRepo.persist(WorkCollection())
            collection2Id = collection2.id
        }

        var collections = collectionRepo.select()
        assertEquals(2, collections.count())

        transaction.executeWithoutResult {
            val rows = collectionRepo.deleteAll()
            assertEquals(2, rows)
        }

        collections = collectionRepo.select()
        assertEquals(0, collections.count())
    }

    @Test
    fun `delete without where clause`() {
        var collection1Id: Long = -1
        var collection2Id: Long = -1

        transaction.executeWithoutResult {
            val collection1 = collectionRepo.persist(WorkCollection())
            collection1Id = collection1.id
            val collection2 = collectionRepo.persist(WorkCollection())
            collection2Id = collection2.id
        }

        var collections = collectionRepo.select()
        assertEquals(2, collections.count())

        // Delete without where clause should not do anything
        transaction.executeWithoutResult {
            val rows = collectionRepo.delete { }
            assertEquals(0, rows)
        }

        collections = collectionRepo.select()
        assertEquals(2, collections.count())
    }

    @Test
    fun `delete by id`() {
        var collection1Id: Long = -1
        var collection2Id: Long = -1

        transaction.executeWithoutResult {
            val collection1 = collectionRepo.persist(WorkCollection())
            collection1Id = collection1.id
            val collection2 = collectionRepo.persist(WorkCollection())
            collection2Id = collection2.id
        }

        var collections = collectionRepo.select()
        assertEquals(2, collections.count())

        transaction.executeWithoutResult {
            val rows = collectionRepo.deleteById(collection1Id)
            assertEquals(1, rows)
        }

        collections = collectionRepo.select()
        assertEquals(1, collections.count())
        assertTrue { collections.first().id == collection2Id }
    }

    @Test
    fun `delete by id with composite key`() {
        var collection1Id: Long = -1
        var collection2Id: Long = -1
        var work1Id: Long = -1
        var work2Id: Long = -1

        transaction.executeWithoutResult {
            val collection1 = collectionRepo.persist(WorkCollection())
            collection1Id = collection1.id
            val collection2 = collectionRepo.persist(WorkCollection())
            collection2Id = collection2.id
            val work1 = workRepo.persist(Work(collection1Id, "Hey!"))
            work1Id = work1.id
            val work2 = workRepo.persist(Work(collection2Id, "Moin!"))
            work2Id = work2.id
        }

        var works = workRepo.select()
        assertEquals(2, works.count())

        transaction.executeWithoutResult {
            val rows = workRepo.deleteById(WorkId(work1Id, collection1Id))
            assertEquals(1, rows)
        }

        works = workRepo.select()
        assertEquals(1, works.count())
        assertTrue { works.first().id == work2Id }
    }

    @Test
    fun `delete by ids`() {
        var collection1Id: Long = -1
        var collection2Id: Long = -1
        var collection3Id: Long = -1

        transaction.executeWithoutResult {
            val collection1 = collectionRepo.persist(WorkCollection())
            collection1Id = collection1.id
            val collection2 = collectionRepo.persist(WorkCollection())
            collection2Id = collection2.id
            val collection3 = collectionRepo.persist(WorkCollection())
            collection3Id = collection3.id
        }

        var collections = collectionRepo.select()
        assertEquals(3, collections.count())

        transaction.executeWithoutResult {
            val rows = collectionRepo.deleteByIds(listOf(collection1Id, collection2Id))
            assertEquals(2, rows)
        }

        collections = collectionRepo.select()
        assertEquals(1, collections.count())
        assertTrue { collections.first().id == collection3Id }
    }

    @Test
    fun `delete by ids with composite key`() {
        var collection1Id: Long = -1
        var collection2Id: Long = -1
        var collection3Id: Long = -1
        var work1Id: Long = -1
        var work2Id: Long = -1
        var work3Id: Long = -1

        transaction.executeWithoutResult {
            val collection1 = collectionRepo.persist(WorkCollection())
            collection1Id = collection1.id
            val collection2 = collectionRepo.persist(WorkCollection())
            collection2Id = collection2.id
            val collection3 = collectionRepo.persist(WorkCollection())
            collection3Id = collection3.id
            val work1 = workRepo.persist(Work(collection1Id, "Hey!"))
            work1Id = work1.id
            val work2 = workRepo.persist(Work(collection2Id, "Moin!"))
            work2Id = work2.id
            val work3 = workRepo.persist(Work(collection3Id, "Hi!"))
            work3Id = work3.id
        }

        var works = workRepo.select()
        assertEquals(3, works.count())

        transaction.executeWithoutResult {
            val rows = workRepo.deleteByIds(
                setOf(
                    WorkId(work1Id, collection1Id),
                    WorkId(work2Id, collection2Id)
                )
            )
            assertEquals(2, rows)
        }

        works = workRepo.select()
        assertEquals(1, works.count())
        assertTrue { works.first().id == work3Id }
    }

    @Test
    fun `delete by ids with some not existing`() {
        var collection1Id: Long = -1
        var collection2Id: Long = -1
        var collection3Id: Long = -1

        transaction.executeWithoutResult {
            val collection1 = collectionRepo.persist(WorkCollection())
            collection1Id = collection1.id
            val collection3 = collectionRepo.persist(WorkCollection())
            collection3Id = collection3.id
        }

        var collections = collectionRepo.select()
        assertEquals(2, collections.count())

        transaction.executeWithoutResult {
            val rows = collectionRepo.deleteByIds(listOf(collection1Id, collection2Id))
            assertEquals(1, rows)
        }

        collections = collectionRepo.select()
        assertEquals(1, collections.count())
        assertTrue { collections.first().id == collection3Id }
    }

    @Test
    fun rollback() {
        var collection1Id: Long = -1
        var collection2Id: Long = -1

        transaction.executeWithoutResult { tx ->
            val collection1 = collectionRepo.persist(WorkCollection())
            collection1Id = collection1.id
            val collection2 = collectionRepo.persist(WorkCollection())
            collection2Id = collection2.id
            // Perform rollback
            tx.setRollbackOnly()
        }

        val collections = collectionRepo.select()
        assertEquals(0, collections.count())
    }
}
