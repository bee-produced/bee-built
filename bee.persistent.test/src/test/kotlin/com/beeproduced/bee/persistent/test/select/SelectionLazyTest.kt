package com.beeproduced.bee.persistent.test.select

import com.beeproduced.bee.persistent.test.config.ATestConfig
import com.beeproduced.bee.persistent.test.config.BaseTestConfig
import com.beeproduced.datasource.a.Circular
import com.beeproduced.datasource.a.WeirdClass
import com.beeproduced.datasource.test.dsl.LazyEntityDSL
import com.beeproduced.datasource.test.lazy.LazyEmbedded
import com.beeproduced.datasource.test.lazy.LazyEntity
import com.beeproduced.datasource.test.lazy.LazyEntityRepository
import com.beeproduced.datasource.test.lazy.SomeEmbedded
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
import java.util.*
import kotlin.test.Test

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-16
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [BaseTestConfig::class])
@TestPropertySource("classpath:application.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SelectionLazyTest(
    @Qualifier("testEM")
    val em: EntityManager,
    @Qualifier("testTM")
    transactionManager: PlatformTransactionManager,
    @Autowired
    val lazyRepo: LazyEntityRepository
) {
    private val transaction = TransactionTemplate(transactionManager)

    @BeforeAll
    fun beforeAll() = clear()

    @AfterEach
    fun afterEach() = clear()

    fun clear() {
        transaction.executeWithoutResult {
            lazyRepo.cbf.delete(em, LazyEntity::class.java).executeUpdate()
        }
    }

    @Test
    fun `lazy field select`() {
        transaction.executeWithoutResult {

            val embedded = SomeEmbedded("data", "lazyData")
            val lazyEmbedded = LazyEmbedded("data2", "lazyData2")
            lazyRepo.persist(LazyEntity(UUID.randomUUID(), "name", "lazyName", embedded, lazyEmbedded))

            val first = lazyRepo.select().firstOrNull()

            val selection = LazyEntityDSL.select {
                lazyName()
                this.embedded { }
            }
            val second = lazyRepo.select(selection).firstOrNull()

            val selection2 = LazyEntityDSL.select {
                this.embedded { }
            }
            val third = lazyRepo.select(selection).firstOrNull()

            println("test")
        }


    }

}