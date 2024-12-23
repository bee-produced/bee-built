package com.beeproduced.bee.persistent.test.select

import com.beeproduced.bee.persistent.test.config.ATestConfig
import com.beeproduced.datasource.a.*
import com.beeproduced.datasource.a.dsl.WeirdClassDSL
import jakarta.persistence.EntityManager
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
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
 * @version 2024-01-15
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [ATestConfig::class])
@TestPropertySource("classpath:application.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SelectOrderByATest(
  @Qualifier("aEM") val em: EntityManager,
  @Qualifier("aTM") transactionManager: PlatformTransactionManager,
  @Autowired val weirdRepo: WeirdClassRepository,
) {
  private val transaction = TransactionTemplate(transactionManager)

  @BeforeAll fun beforeAll() = clear()

  @AfterEach fun afterEach() = clear()

  fun clear() {
    transaction.executeWithoutResult {
      weirdRepo.cbf.delete(em, WeirdClass::class.java).executeUpdate()
    }
  }

  @Test
  fun `order by asc`() {
    transaction.executeWithoutResult {
      val id = UUID.randomUUID()
      val fooBar = FooBar("foo", "bar")
      val foxtrot = Foxtrot("Foxtrot")
      weirdRepo.persist(WeirdClass(id, fooBar, foxtrot))
      val id2 = UUID.randomUUID()
      val fooBar2 = FooBar("foo", "bar2")
      val foxtrot2 = Foxtrot("Foxtrot2")
      weirdRepo.persist(WeirdClass(id2, fooBar2, foxtrot2))
      val id3 = UUID.randomUUID()
      val fooBar3 = FooBar("foo", "bar3")
      val foxtrot3 = Foxtrot("Foxtrot3")
      weirdRepo.persist(WeirdClass(id3, fooBar3, foxtrot3))

      val ws = weirdRepo.select { orderBy(WeirdClassDSL.foxtrot.asc()) }
      assertEquals(id, ws[0].id)
      assertEquals(id2, ws[1].id)
      assertEquals(id3, ws[2].id)
    }
  }

  @Test
  fun `order by desc`() {
    transaction.executeWithoutResult {
      val id = UUID.randomUUID()
      val fooBar = FooBar("foo", "bar")
      val foxtrot = Foxtrot("Foxtrot")
      weirdRepo.persist(WeirdClass(id, fooBar, foxtrot))
      val id2 = UUID.randomUUID()
      val fooBar2 = FooBar("foo", "bar2")
      val foxtrot2 = Foxtrot("Foxtrot2")
      weirdRepo.persist(WeirdClass(id2, fooBar2, foxtrot2))
      val id3 = UUID.randomUUID()
      val fooBar3 = FooBar("foo", "bar3")
      val foxtrot3 = Foxtrot("Foxtrot3")
      weirdRepo.persist(WeirdClass(id3, fooBar3, foxtrot3))

      val ws = weirdRepo.select { orderBy(WeirdClassDSL.foxtrot.desc()) }
      assertEquals(id3, ws[0].id)
      assertEquals(id2, ws[1].id)
      assertEquals(id, ws[2].id)
    }
  }

  @Test
  fun `order by multiple columns desc`() {
    transaction.executeWithoutResult {
      val id = UUID.randomUUID()
      val fooBar = FooBar("foo", "bar")
      val foxtrot = Foxtrot("Foxtrot")
      weirdRepo.persist(WeirdClass(id, fooBar, foxtrot))
      val id2 = UUID.randomUUID()
      val fooBar2 = FooBar("foo", "bar2")
      val foxtrot2 = Foxtrot("FoxtrotDuplicate")
      weirdRepo.persist(WeirdClass(id2, fooBar2, foxtrot2))
      val id3 = UUID.randomUUID()
      val fooBar3 = FooBar("foo", "bar3")
      val foxtrot3 = Foxtrot("FoxtrotDuplicate")
      weirdRepo.persist(WeirdClass(id3, fooBar3, foxtrot3))

      val ws =
        weirdRepo.select { orderBy(WeirdClassDSL.foxtrot.desc(), WeirdClassDSL.fooBar.desc()) }
      assertEquals(id3, ws[0].id)
      assertEquals(id2, ws[1].id)
      assertEquals(id, ws[2].id)
    }
  }
}
