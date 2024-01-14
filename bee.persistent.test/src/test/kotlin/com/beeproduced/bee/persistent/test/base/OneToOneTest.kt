package com.beeproduced.bee.persistent.test.base

import com.beeproduced.datasource.test.onetoone.Branch
import com.beeproduced.datasource.test.onetoone.BranchRepository
import com.beeproduced.datasource.test.onetoone.Root
import com.beeproduced.datasource.test.onetoone.RootRepository
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
import kotlin.test.Test

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-14
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [BaseTestConfig::class])
@TestPropertySource("classpath:application.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OneToOneTest(
    @Qualifier("testEM")
    val em: EntityManager,
    @Qualifier("testTM")
    transactionManager: PlatformTransactionManager,
    @Autowired
    val rootRepo: RootRepository,
    @Autowired
    val branchRepo: BranchRepository
) {
    private val transaction = TransactionTemplate(transactionManager)

    @BeforeAll
    fun beforeAll() = clear()

    @AfterEach
    fun afterEach() = clear()

    fun clear() = transaction.executeWithoutResult {
        rootRepo.cbf.delete(em, Root::class.java).executeUpdate()
        branchRepo.cbf.delete(em, Branch::class.java).executeUpdate()
    }

    @Test
    fun `select with relations not loaded`() {
        // TODO
    }
}