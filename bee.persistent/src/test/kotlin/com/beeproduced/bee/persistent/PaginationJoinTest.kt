package com.beeproduced.bee.persistent

import com.beeproduced.bee.persistent.config.DummyApplication
import com.beeproduced.bee.persistent.config.PaginationTestConfiguration
import com.beeproduced.bee.persistent.pagination.FoxtrotInfo
import com.beeproduced.bee.persistent.pagination.FoxtrotInfoRepository
import com.beeproduced.bee.persistent.pagination.PaginatedFoxtrot
import com.beeproduced.bee.persistent.pagination.PaginatedFoxtrotRepository
import com.beeproduced.bee.persistent.selection.EmptySelection
import com.beeproduced.bee.persistent.selection.SimpleSelection
import com.beeproduced.bee.persistent.selection.SimpleSelection.FieldNode
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.random.Random
import kotlin.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

/**
 * Testing pagination info according to specification
 * https://relay.dev/graphql/connections.htm#sec-undefined.PageInfo
 */
@SpringBootTest(classes = [DummyApplication::class, PaginationTestConfiguration::class])
@TestPropertySource("classpath:application.properties")
class PaginationJoinTest {

  @Autowired lateinit var paginatedFoxtrot: PaginatedFoxtrotRepository

  @Autowired lateinit var info: FoxtrotInfoRepository

  @Qualifier("orderTransactionManager")
  @Autowired
  lateinit var transactionManager: PlatformTransactionManager

  private lateinit var transaction: TransactionTemplate

  private val hansCount = 25

  private val selection = SimpleSelection(setOf(FieldNode(PaginatedFoxtrot::infos.name)))

  private fun generateData(): List<PaginatedFoxtrot> {
    val createdBy = listOf("Hans", "Peter", "Alexander")
    val created = mutableListOf<PaginatedFoxtrot>()

    var relations = 0
    for (i in 1..100) {
      val name = if (i <= hansCount) createdBy[0] else if (i <= 50) createdBy[1] else createdBy[2]
      val foxtrot =
        PaginatedFoxtrot(
            createdBy = name,
            createdOn =
              Instant.now()
                .plusSeconds(i.toLong() * 3)
                .plusMillis(Random.nextLong(1000))
                .truncatedTo(ChronoUnit.MICROS),
          )
          .let(paginatedFoxtrot::persist)
      created.add(foxtrot)

      for (j in 0..relations) {
        info.persist(FoxtrotInfo(customCreatedBy = "$name!", foxtrotId = foxtrot.id))
      }

      // 0, 1, 2 and again 0, 1, 2
      relations = ((relations + 1).mod(3))
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
      info.deleteAll()
      paginatedFoxtrot.deleteAll()
    }
  }

  @Test
  fun `when fetching all elements ascending in first page have no previous and next page`() {
    transaction.executeWithoutResult { generateData() }

    val params = PaginatedFoxtrotRepository.PaginatedFoxtrotParameter("Hans!", first = hansCount)
    val result = paginatedFoxtrot.pagination(params, selection)
    val edges = requireNotNull(result.edges)
    val info = requireNotNull(result.pageInfo)

    assertFalse(info.hasNextPage, "hasNextPage")
    assertFalse(info.hasPreviousPage, "hasPreviousPage")
    assertTrue(edges.size == hansCount)

    var lastDate = Instant.MIN
    for (message in edges) {
      assertEquals("Hans", message.node.createdBy, "is not createdBy")
      assertNotNull(message.node.infos)
      assertTrue(message.node.createdOn.isAfter(lastDate), "is not ascending")
      lastDate = message.node.createdOn
    }
  }

  @Test
  fun `when fetching all elements ascending in first page with first cursor have previous but no next page`() {
    var data = listOf<PaginatedFoxtrot>()
    transaction.executeWithoutResult { data = generateData() }
    val start = data[0] // 1st element

    val params =
      PaginatedFoxtrotRepository.PaginatedFoxtrotParameter(
        "Hans!",
        first = hansCount,
        after = PaginatedFoxtrotRepository.encodeCursor(start),
      )
    val result = paginatedFoxtrot.pagination(params, selection)
    val edges = requireNotNull(result.edges)
    val info = requireNotNull(result.pageInfo)

    assertFalse(info.hasNextPage, "hasNextPage")
    assertTrue(info.hasPreviousPage, "hasPreviousPage")
    assertTrue(edges.size == hansCount - 1)

    var lastDate = Instant.MIN
    for (message in edges) {
      assertEquals("Hans", message.node.createdBy, "is not createdBy")
      assertNotNull(message.node.infos)
      assertTrue(message.node.createdOn.isAfter(lastDate), "is not ascending")
      lastDate = message.node.createdOn
    }
  }

  @Test
  fun `when fetching all elements descending in first page have no previous and next page`() {
    transaction.executeWithoutResult { generateData() }

    val params = PaginatedFoxtrotRepository.PaginatedFoxtrotParameter("Hans!", last = hansCount)
    val result = paginatedFoxtrot.pagination(params, selection)
    val edges = requireNotNull(result.edges)
    val info = requireNotNull(result.pageInfo)

    assertFalse(info.hasNextPage, "hasNextPage should be false")
    assertFalse(info.hasPreviousPage, "hasPreviousPage should be false")
    assertTrue(edges.size == hansCount)

    var lastDate = Instant.MAX
    for (message in edges) {
      assertEquals("Hans", message.node.createdBy, "is not createdBy")
      assertNotNull(message.node.infos)
      assertTrue(message.node.createdOn.isBefore(lastDate), "is not descending")
      lastDate = message.node.createdOn
    }
  }

  @Test
  fun `when fetching all elements descending in first page with last cursor have next but no previous page`() {
    var data = listOf<PaginatedFoxtrot>()
    transaction.executeWithoutResult { data = generateData() }
    val last = data[24] // 25th element

    val params =
      PaginatedFoxtrotRepository.PaginatedFoxtrotParameter(
        "Hans!",
        last = hansCount,
        before = PaginatedFoxtrotRepository.encodeCursor(last),
      )
    val result = paginatedFoxtrot.pagination(params, selection)
    val edges = requireNotNull(result.edges)
    val info = requireNotNull(result.pageInfo)

    assertTrue(info.hasNextPage, "hasNextPage should be true")
    assertFalse(info.hasPreviousPage, "hasPreviousPage should be false")
    assertTrue(edges.size == hansCount - 1)

    var lastDate = Instant.MAX
    for (message in edges) {
      assertEquals("Hans", message.node.createdBy, "is not createdBy")
      assertNotNull(message.node.infos)
      assertTrue(message.node.createdOn.isBefore(lastDate), "is not descending")
      lastDate = message.node.createdOn
    }
  }

  @Test
  fun `when fetching 10 elements ascending, should have next but no previous page`() {
    transaction.executeWithoutResult { generateData() }

    val params = PaginatedFoxtrotRepository.PaginatedFoxtrotParameter("Hans!", first = 10)
    val result = paginatedFoxtrot.pagination(params, EmptySelection())
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
    transaction.executeWithoutResult { generateData() }

    val params = PaginatedFoxtrotRepository.PaginatedFoxtrotParameter("Hans!", last = 10)
    val result = paginatedFoxtrot.pagination(params, EmptySelection())
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
    // setup
    var data = listOf<PaginatedFoxtrot>()
    transaction.executeWithoutResult { data = generateData() }
    val start = data[9] // 10th element
    assertTrue(start.createdBy == "Hans")

    // act
    val params =
      PaginatedFoxtrotRepository.PaginatedFoxtrotParameter(
        "Hans!",
        first = 10,
        after = PaginatedFoxtrotRepository.encodeCursor(start),
      )
    val result = paginatedFoxtrot.pagination(params, EmptySelection())
    val edges = requireNotNull(result.edges)
    val info = requireNotNull(result.pageInfo)

    // assert
    assertTrue(info.hasNextPage, "hasNextPage should be true")
    assertTrue(info.hasPreviousPage, "hasPreviousPage should be true")
    assertTrue("Cursor should not be in result") { edges.none { it.node.id == start.id } }
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
    // setup
    var data = listOf<PaginatedFoxtrot>()
    transaction.executeWithoutResult { data = generateData() }
    val end = data[14] // 15th element
    assertTrue(end.createdBy == "Hans")

    // act
    val params =
      PaginatedFoxtrotRepository.PaginatedFoxtrotParameter(
        "Hans!",
        last = 10,
        before = PaginatedFoxtrotRepository.encodeCursor(end),
      )
    val result = paginatedFoxtrot.pagination(params, EmptySelection())
    val edges = requireNotNull(result.edges)
    val info = requireNotNull(result.pageInfo)

    // assert
    assertTrue(info.hasNextPage, "hasNextPage should be true")
    assertTrue(info.hasPreviousPage, "hasPreviousPage should be true")
    assertTrue("Cursor should not be in result") { edges.none { it.node.id == end.id } }
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
    // setup
    var data = listOf<PaginatedFoxtrot>()
    transaction.executeWithoutResult { data = generateData() }
    val start = data[9] // 10th element
    assertTrue(start.createdBy == "Hans")

    // act
    val params =
      PaginatedFoxtrotRepository.PaginatedFoxtrotParameter(
        "Hans!",
        first = 15,
        after = PaginatedFoxtrotRepository.encodeCursor(start),
      )
    val result = paginatedFoxtrot.pagination(params, EmptySelection())
    val edges = requireNotNull(result.edges)
    val info = requireNotNull(result.pageInfo)

    // assert
    assertFalse(info.hasNextPage, "hasNextPage should be false")
    assertTrue(info.hasPreviousPage, "hasPreviousPage should be true")
    assertTrue("Cursor should not be in result") { edges.none { it.node.id == start.id } }
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
    // setup
    var data = listOf<PaginatedFoxtrot>()
    transaction.executeWithoutResult { data = generateData() }
    val end = data[15] // 16th element
    assertTrue(end.createdBy == "Hans")

    // act
    val params =
      PaginatedFoxtrotRepository.PaginatedFoxtrotParameter(
        "Hans!",
        last = 15,
        before = PaginatedFoxtrotRepository.encodeCursor(end),
      )
    val result = paginatedFoxtrot.pagination(params, EmptySelection())
    val edges = requireNotNull(result.edges)
    val info = requireNotNull(result.pageInfo)

    // assert
    assertTrue(info.hasNextPage, "hasNextPage should be true")
    assertFalse(info.hasPreviousPage, "hasPreviousPage should be false")
    assertTrue("Cursor should not be in result") { edges.none { it.node.id == end.id } }
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
    // setup
    var data = listOf<PaginatedFoxtrot>()
    transaction.executeWithoutResult { data = generateData() }
    val start = data[0] // 1st element
    assertTrue(start.createdBy == "Hans")

    // act
    val params =
      PaginatedFoxtrotRepository.PaginatedFoxtrotParameter(
        "Hans!",
        first = 1,
        after = PaginatedFoxtrotRepository.encodeCursor(start),
      )
    val result = paginatedFoxtrot.pagination(params, EmptySelection())
    val edges = requireNotNull(result.edges)
    val info = requireNotNull(result.pageInfo)

    // assert
    assertTrue(info.hasNextPage, "hasNextPage should be true")
    assertTrue(info.hasPreviousPage, "hasPreviousPage should be true")
    assertTrue("Cursor should not be in result") { edges.none { it.node.id == start.id } }
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
    // setup
    var data = listOf<PaginatedFoxtrot>()
    transaction.executeWithoutResult { data = generateData() }
    val end = data[24] // 25th element
    assertTrue(end.createdBy == "Hans")

    // act
    val params =
      PaginatedFoxtrotRepository.PaginatedFoxtrotParameter(
        "Hans!",
        last = 1,
        before = PaginatedFoxtrotRepository.encodeCursor(end),
      )
    val result = paginatedFoxtrot.pagination(params, EmptySelection())
    val edges = requireNotNull(result.edges)
    val info = requireNotNull(result.pageInfo)

    // assert
    assertTrue(info.hasNextPage, "hasNextPage should be true")
    assertTrue(info.hasPreviousPage, "hasPreviousPage should be true")
    assertTrue("Cursor should not be in result") { edges.none { it.node.id == end.id } }
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
    // setup
    var data = listOf<PaginatedFoxtrot>()
    transaction.executeWithoutResult { data = generateData() }
    val start = data[0] // 1st element
    assertTrue(start.createdBy == "Hans")
    val modifiedStart = start.copy(createdOn = start.createdOn.minusSeconds(60))

    // act
    val params =
      PaginatedFoxtrotRepository.PaginatedFoxtrotParameter(
        "Hans!",
        first = 1,
        after = PaginatedFoxtrotRepository.encodeCursor(modifiedStart),
      )
    val result = paginatedFoxtrot.pagination(params, EmptySelection())
    val edges = requireNotNull(result.edges)

    // assert
    assertNull(result.pageInfo)
    assertTrue(edges.isEmpty())
  }

  @Test
  fun `when cursor fetching after last element, should have no page`() {
    // setup
    var data = listOf<PaginatedFoxtrot>()
    transaction.executeWithoutResult { data = paginatedFoxtrot.persistAll(generateData()) }
    val end = data[24] // 25th element
    assertTrue(end.createdBy == "Hans")
    val modifiedEnd = end.copy(createdOn = end.createdOn.plusSeconds(60))

    // act
    val params =
      PaginatedFoxtrotRepository.PaginatedFoxtrotParameter(
        "Hans!",
        last = 1,
        before = PaginatedFoxtrotRepository.encodeCursor(modifiedEnd),
      )
    val result = paginatedFoxtrot.pagination(params, EmptySelection())
    val edges = requireNotNull(result.edges)

    // assert
    assertNull(result.pageInfo)
    assertTrue(edges.isEmpty())
  }
}
