package com.beeproduced.bee.persistent

import com.beeproduced.bee.persistent.selection.EmptySelection
import com.beeproduced.bee.persistent.config.DummyApplication
import com.beeproduced.bee.persistent.config.PaginationTestConfiguration
import com.beeproduced.bee.persistent.pagination.PaginatedBar
import com.beeproduced.bee.persistent.pagination.PaginatedBarRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Testing pagination info according to specification
 * https://relay.dev/graphql/connections.htm#sec-undefined.PageInfo
 */
@SpringBootTest(
    classes = [DummyApplication::class, PaginationTestConfiguration::class]
)
@TestPropertySource("classpath:application.properties")
class PaginationCompositeTest {

    @Autowired
    lateinit var paginatedBarRepository: PaginatedBarRepository

    @Qualifier("orderTransactionManager")
    @Autowired
    lateinit var transactionManager: PlatformTransactionManager

    private lateinit var transaction: TransactionTemplate

    private val hansCount = 25

    private fun generateData(): List<PaginatedBar> {
        val createdBy = listOf("Hans", "Peter", "Alexander")
        val created = mutableListOf<PaginatedBar>()

        var lastId = UUID.fromString("00000000-0000-0000-0000-000000000000")
        for (i in 1..100) {
            val idA = lastId
            val idB = UUID.randomUUID()
            lastId = idB
            val bar = PaginatedBar(
                idA = idA,
                idB = idB,
                createdBy = if (i <= hansCount) createdBy[0] else if (i <= 50) createdBy[1] else createdBy[2],
                createdOn = Instant.now()
                    .plusSeconds(i.toLong() * 3)
                    .plusMillis(Random.nextLong(1000))
                    .truncatedTo(ChronoUnit.MICROS)
            )
            created.add(bar)
        }

        return created
    }

    @BeforeEach
    fun setup() {
        transaction = TransactionTemplate(transactionManager)
    }

    @AfterEach
    fun teardown() {
        transaction.executeWithoutResult {
            paginatedBarRepository.deleteAll()
        }
    }

    @Test
    fun `when fetching all elements ascending in first page have no previous and next page`() {
        transaction.executeWithoutResult {
            paginatedBarRepository.persistAll(generateData())
        }

        val params = PaginatedBarRepository.PaginatedBarParameter("Hans", first = hansCount)
        val result = paginatedBarRepository.pagination(params, EmptySelection())
        val edges = requireNotNull(result.edges)
        val info = requireNotNull(result.pageInfo)

        assertFalse(info.hasNextPage, "hasNextPage")
        assertFalse(info.hasPreviousPage, "hasPreviousPage")
        assertTrue(edges.size == hansCount)

        var lastDate = Instant.MIN
        for (message in edges) {
            assertEquals("Hans", message.node.createdBy, "is not createdBy")
            assertTrue(message.node.createdOn.isAfter(lastDate), "is not ascending")
            lastDate = message.node.createdOn
        }
    }

    @Test
    fun `when fetching all elements ascending in first page with first cursor have previous but no next page`() {
        var data = listOf<PaginatedBar>()
        transaction.executeWithoutResult {
            data = paginatedBarRepository.persistAll(generateData())
        }
        val start = data[0] //1st element

        val params = PaginatedBarRepository.PaginatedBarParameter(
            "Hans",
            first = hansCount,
            after = PaginatedBarRepository.encodeCursor(start)
        )
        val result = paginatedBarRepository.pagination(params, EmptySelection())
        val edges = requireNotNull(result.edges)
        val info = requireNotNull(result.pageInfo)

        assertFalse(info.hasNextPage, "hasNextPage")
        assertTrue(info.hasPreviousPage, "hasPreviousPage")
        assertTrue(edges.size == hansCount - 1)

        var lastDate = Instant.MIN
        for (message in edges) {
            assertEquals("Hans", message.node.createdBy, "is not createdBy")
            assertTrue(message.node.createdOn.isAfter(lastDate), "is not ascending")
            lastDate = message.node.createdOn
        }
    }

    @Test
    fun `when fetching all elements descending in first page have no previous and next page`() {
        transaction.executeWithoutResult {
            paginatedBarRepository.persistAll(generateData())
        }

        val params = PaginatedBarRepository.PaginatedBarParameter("Hans", last = hansCount)
        val result = paginatedBarRepository.pagination(params, EmptySelection())
        val edges = requireNotNull(result.edges)
        val info = requireNotNull(result.pageInfo)

        assertFalse(info.hasNextPage, "hasNextPage should be false")
        assertFalse(info.hasPreviousPage, "hasPreviousPage should be false")
        assertTrue(edges.size == hansCount)

        var lastDate = Instant.MAX
        for (message in edges) {
            assertEquals("Hans", message.node.createdBy, "is not createdBy")
            assertTrue(message.node.createdOn.isBefore(lastDate), "is not descending")
            lastDate = message.node.createdOn
        }
    }

    @Test
    fun `when fetching all elements descending in first page with last cursor have next but no previous page`() {
        var data = listOf<PaginatedBar>()
        transaction.executeWithoutResult {
            data = paginatedBarRepository.persistAll(generateData())
        }
        val last = data[24] //25th element

        val params = PaginatedBarRepository.PaginatedBarParameter(
            "Hans",
            last = hansCount,
            before = PaginatedBarRepository.encodeCursor(last)
        )
        val result = paginatedBarRepository.pagination(params, EmptySelection())
        val edges = requireNotNull(result.edges)
        val info = requireNotNull(result.pageInfo)

        assertTrue(info.hasNextPage, "hasNextPage should be true")
        assertFalse(info.hasPreviousPage, "hasPreviousPage should be false")
        assertTrue(edges.size == hansCount - 1)

        var lastDate = Instant.MAX
        for (message in edges) {
            assertEquals("Hans", message.node.createdBy, "is not createdBy")
            assertTrue(message.node.createdOn.isBefore(lastDate), "is not descending")
            lastDate = message.node.createdOn
        }
    }


    @Test
    fun `when fetching 10 elements ascending, should have next but no previous page`() {
        transaction.executeWithoutResult {
            paginatedBarRepository.persistAll(generateData())
        }

        val params = PaginatedBarRepository.PaginatedBarParameter("Hans", first = 10)
        val result = paginatedBarRepository.pagination(params, EmptySelection())
        val edges = requireNotNull(result.edges)
        val info = requireNotNull(result.pageInfo)

        assertTrue(info.hasNextPage, "hasNextPage should be true")
        assertFalse(info.hasPreviousPage, "hasPreviousPage should be false")
        assertTrue(edges.size == 10)

        var lastDate = Instant.MIN
        for (message in edges) {
            assertEquals("Hans", message.node.createdBy, "is not createdBy")
            assertTrue(message.node.createdOn.isAfter(lastDate), "is not ascending")
            lastDate = message.node.createdOn
        }
    }

    @Test
    fun `when fetching 10 elements descending, should have previous but no next page`() {
        transaction.executeWithoutResult {
            paginatedBarRepository.persistAll(generateData())
        }

        val params = PaginatedBarRepository.PaginatedBarParameter("Hans", last = 10)
        val result = paginatedBarRepository.pagination(params, EmptySelection())
        val edges = requireNotNull(result.edges)
        val info = requireNotNull(result.pageInfo)

        assertFalse(info.hasNextPage, "hasNextPage should be false")
        assertTrue(info.hasPreviousPage, "hasPreviousPage should be true")
        assertTrue(edges.size == 10)

        var lastDate = Instant.MAX
        for (message in edges) {
            assertEquals("Hans", message.node.createdBy, "is not createdBy")
            assertTrue(message.node.createdOn.isBefore(lastDate), "is not descending")
            lastDate = message.node.createdOn
        }
    }

    @Test
    fun `when cursor fetching 10 elements ascending, should have previous and next page`() {
        //setup
        var data = listOf<PaginatedBar>()
        transaction.executeWithoutResult {
            data = paginatedBarRepository.persistAll(generateData())
        }
        val start = data[9] //10th element
        assertTrue(start.createdBy == "Hans")

        //act
        val params = PaginatedBarRepository.PaginatedBarParameter(
            "Hans",
            first = 10,
            after = PaginatedBarRepository.encodeCursor(start)
        )
        val result = paginatedBarRepository.pagination(params, EmptySelection())
        val edges = requireNotNull(result.edges)
        val info = requireNotNull(result.pageInfo)

        //assert
        assertTrue(info.hasNextPage, "hasNextPage should be true")
        assertTrue(info.hasPreviousPage, "hasPreviousPage should be true")
        assertTrue("Cursor should not be in result") { edges.none { it.node.idA == start.idA && it.node.idB == start.idB } }
        assertTrue(edges.size == 10)

        var lastDate = Instant.MIN
        for (message in edges) {
            assertEquals("Hans", message.node.createdBy, "is not createdBy")
            assertTrue(message.node.createdOn.isAfter(start.createdOn), "is not respecting cursor start")
            assertTrue(message.node.createdOn.isAfter(lastDate), "is not ascending")
            lastDate = message.node.createdOn
        }
    }

    @Test
    fun `when cursor fetching 10 elements descending, should have previous and next page`() {
        //setup
        var data = listOf<PaginatedBar>()
        transaction.executeWithoutResult {
            data = paginatedBarRepository.persistAll(generateData())
        }
        val end = data[14] //15th element
        assertTrue(end.createdBy == "Hans")

        //act
        val params = PaginatedBarRepository.PaginatedBarParameter(
            "Hans",
            last = 10,
            before = PaginatedBarRepository.encodeCursor(end)
        )
        val result = paginatedBarRepository.pagination(params, EmptySelection())
        val edges = requireNotNull(result.edges)
        val info = requireNotNull(result.pageInfo)

        //assert
        assertTrue(info.hasNextPage, "hasNextPage should be true")
        assertTrue(info.hasPreviousPage, "hasPreviousPage should be true")
        assertTrue("Cursor should not be in result") { edges.none { it.node.idA == end.idA && it.node.idB == end.idB } }
        assertTrue(edges.size == 10)

        var lastDate = Instant.MAX
        for (message in edges) {
            assertEquals("Hans", message.node.createdBy, "is not createdBy")
            assertTrue(message.node.createdOn.isBefore(end.createdOn), "is not respecting cursor end")
            assertTrue(message.node.createdOn.isBefore(lastDate), "is not descending")
            lastDate = message.node.createdOn
        }
    }

    @Test
    fun `when cursor fetching last elements ascending, should have previous and no next page`() {
        //setup
        var data = listOf<PaginatedBar>()
        transaction.executeWithoutResult {
            data = paginatedBarRepository.persistAll(generateData())
        }
        val start = data[9]  //10th element
        assertTrue(start.createdBy == "Hans")

        //act
        val params = PaginatedBarRepository.PaginatedBarParameter(
            "Hans",
            first = 15,
            after = PaginatedBarRepository.encodeCursor(start)
        )
        val result = paginatedBarRepository.pagination(params, EmptySelection())
        val edges = requireNotNull(result.edges)
        val info = requireNotNull(result.pageInfo)

        //assert
        assertFalse(info.hasNextPage, "hasNextPage should be false")
        assertTrue(info.hasPreviousPage, "hasPreviousPage should be true")
        assertTrue("Cursor should not be in result") { edges.none { it.node.idA == start.idA && it.node.idB == start.idB } }
        assertTrue(edges.size == 15)

        var lastDate = Instant.MIN
        for (message in edges) {
            assertEquals("Hans", message.node.createdBy, "is not createdBy")
            assertTrue(message.node.createdOn.isAfter(start.createdOn), "is not respecting cursor start")
            assertTrue(message.node.createdOn.isAfter(lastDate), "is not ascending")
            lastDate = message.node.createdOn
        }
    }

    @Test
    fun `when cursor fetching last elements descending, should have next and no previous page`() {
        //setup
        var data = listOf<PaginatedBar>()
        transaction.executeWithoutResult {
            data = paginatedBarRepository.persistAll(generateData())
        }
        val end = data[15] //16th element
        assertTrue(end.createdBy == "Hans")

        //act
        val params = PaginatedBarRepository.PaginatedBarParameter(
            "Hans",
            last = 15,
            before = PaginatedBarRepository.encodeCursor(end)
        )
        val result = paginatedBarRepository.pagination(params, EmptySelection())
        val edges = requireNotNull(result.edges)
        val info = requireNotNull(result.pageInfo)

        //assert
        assertTrue(info.hasNextPage, "hasNextPage should be true")
        assertFalse(info.hasPreviousPage, "hasPreviousPage should be false")
        assertTrue("Cursor should not be in result") { edges.none { it.node.idA == end.idA && it.node.idB == end.idB } }
        assertTrue(edges.size == 15)

        var lastDate = Instant.MAX
        for (message in edges) {
            assertEquals("Hans", message.node.createdBy, "is not createdBy")
            assertTrue(message.node.createdOn.isBefore(end.createdOn), "is not respecting cursor end")
            assertTrue(message.node.createdOn.isBefore(lastDate), "is not descending")
            lastDate = message.node.createdOn
        }
    }

    @Test
    fun `when cursor fetching 1 element ascending, should have previous and next page`() {
        //setup
        var data = listOf<PaginatedBar>()
        transaction.executeWithoutResult {
            data = paginatedBarRepository.persistAll(generateData())
        }
        val start = data[0] //1st element
        assertTrue(start.createdBy == "Hans")

        //act
        val params = PaginatedBarRepository.PaginatedBarParameter(
            "Hans",
            first = 1,
            after = PaginatedBarRepository.encodeCursor(start)
        )
        val result = paginatedBarRepository.pagination(params, EmptySelection())
        val edges = requireNotNull(result.edges)
        val info = requireNotNull(result.pageInfo)

        //assert
        assertTrue(info.hasNextPage, "hasNextPage should be true")
        assertTrue(info.hasPreviousPage, "hasPreviousPage should be true")
        assertTrue("Cursor should not be in result") { edges.none { it.node.idA == start.idA && it.node.idB == start.idB } }
        assertTrue(edges.size == 1)

        var lastDate = Instant.MIN
        for (message in edges) {
            assertEquals("Hans", message.node.createdBy, "is not createdBy")
            assertTrue(message.node.createdOn.isAfter(start.createdOn), "is not respecting cursor start")
            assertTrue(message.node.createdOn.isAfter(lastDate), "is not ascending")
            lastDate = message.node.createdOn
        }
    }

    @Test
    fun `when cursor fetching 1 element descending, should have previous and next page`() {
        //setup
        var data = listOf<PaginatedBar>()
        transaction.executeWithoutResult {
            data = paginatedBarRepository.persistAll(generateData())
        }
        val end = data[24] //25th element
        assertTrue(end.createdBy == "Hans")

        //act
        val params = PaginatedBarRepository.PaginatedBarParameter(
            "Hans",
            last = 1,
            before = PaginatedBarRepository.encodeCursor(end)
        )
        val result = paginatedBarRepository.pagination(params, EmptySelection())
        val edges = requireNotNull(result.edges)
        val info = requireNotNull(result.pageInfo)

        //assert
        assertTrue(info.hasNextPage, "hasNextPage should be true")
        assertTrue(info.hasPreviousPage, "hasPreviousPage should be true")
        assertTrue("Cursor should not be in result") { edges.none { it.node.idA == end.idA && it.node.idB == end.idB } }
        assertTrue(edges.size == 1)

        var lastDate = Instant.MAX
        for (message in edges) {
            assertEquals("Hans", message.node.createdBy, "is not createdBy")
            assertTrue(message.node.createdOn.isBefore(end.createdOn), "is not respecting cursor end")
            assertTrue(message.node.createdOn.isBefore(lastDate), "is not descending")
            lastDate = message.node.createdOn
        }
    }

    @Test
    fun `when cursor fetching before first element, should have no page`() {
        //setup
        var data = listOf<PaginatedBar>()
        transaction.executeWithoutResult {
            data = paginatedBarRepository.persistAll(generateData())
        }
        val start = data[0] //1st element
        assertTrue(start.createdBy == "Hans")
        val modifiedStart = start.copy(createdOn = start.createdOn.minusSeconds(60))

        //act
        val params = PaginatedBarRepository.PaginatedBarParameter(
            "Hans",
            first = 1,
            after = PaginatedBarRepository.encodeCursor(modifiedStart)
        )
        val result = paginatedBarRepository.pagination(params, EmptySelection())
        val edges = requireNotNull(result.edges)

        //assert
        assertNull(result.pageInfo)
        assertTrue(edges.isEmpty())
    }

    @Test
    fun `when cursor fetching after last element, should have no page`() {
        //setup
        var data = listOf<PaginatedBar>()
        transaction.executeWithoutResult {
            data = paginatedBarRepository.persistAll(generateData())
        }
        val end = data[24] //25th element
        assertTrue(end.createdBy == "Hans")
        val modifiedEnd = end.copy(createdOn = end.createdOn.plusSeconds(60))

        //act
        val params = PaginatedBarRepository.PaginatedBarParameter(
            "Hans",
            last = 1,
            before = PaginatedBarRepository.encodeCursor(modifiedEnd)
        )
        val result = paginatedBarRepository.pagination(params, EmptySelection())
        val edges = requireNotNull(result.edges)

        //assert
        assertNull(result.pageInfo)
        assertTrue(edges.isEmpty())
    }
}