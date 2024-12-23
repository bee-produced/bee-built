package com.beeproduced.bee.persistent.test.select

import com.beeproduced.bee.persistent.blaze.selection.BeeSelection
import com.beeproduced.bee.persistent.test.config.ATestConfig
import com.beeproduced.datasource.a.*
import com.beeproduced.datasource.a.dsl.SongDSL
import jakarta.persistence.EntityManager
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
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
class SelectionATest(
  @Qualifier("aEM") val em: EntityManager,
  @Qualifier("aTM") transactionManager: PlatformTransactionManager,
  @Autowired val songRepo: SongRepository,
  @Autowired val companyPersonRepo: CompanyPersonRepository,
  @Autowired val personRepo: PersonRepository,
  @Autowired val companyRepo: CompanyRepository,
  @Autowired val addressRepo: AddressRepository,
  @Autowired val weirdClassRepo: WeirdClassRepository,
) {
  private val transaction = TransactionTemplate(transactionManager)

  @BeforeAll fun beforeAll() = clear()

  @AfterEach fun afterEach() = clear()

  fun clear() {
    transaction.executeWithoutResult {
      songRepo.cbf.delete(em, Song::class.java).executeUpdate()
      companyPersonRepo.cbf.delete(em, CompanyPerson::class.java).executeUpdate()
      personRepo.cbf.delete(em, Person::class.java).executeUpdate()
      companyRepo.cbf.delete(em, Company::class.java).executeUpdate()
      weirdClassRepo.cbf.delete(em, WeirdClass::class.java).executeUpdate()
    }
  }

  @Test
  fun `empty selection`() {
    addSong()
    transaction.executeWithoutResult {
      val selection = BeeSelection.empty()
      val songs = songRepo.select(selection)
      assertTrue { songs.isNotEmpty() }
      val song = songs.first()
      assertNull(song.interpret)
      assertNull(song.producer)
    }
  }

  @Test
  fun `full selection`() {
    addSong()
    transaction.executeWithoutResult {
      val selection =
        SongDSL.select {
          this.interpret {
            this.companies {
              this.company {
                this.employees {
                  this.company {}
                  this.person {}
                }
              }
              this.person {
                this.companies {}
                this.address {}
              }
            }
            this.address {}
          }
          this.producer {
            this.employees {
              this.company { this.employees {} }
              this.person {
                this.companies {
                  this.company {}
                  this.person {}
                }
                this.address {}
              }
            }
          }
        }
      val song = songRepo.select(selection).firstOrNull()
      assertNotNull(song)
      assertSong(song, selection)
    }
  }

  @Test
  fun `partial selection 1`() {
    addSong()
    transaction.executeWithoutResult {
      val selection =
        SongDSL.select {
          this.interpret {
            this.companies {
              this.company {
                this.employees {
                  this.company {}
                  this.person {}
                }
              }
              this.person {
                this.companies {}
                this.address {}
              }
            }
          }
        }
      val song = songRepo.select(selection).firstOrNull()
      assertNotNull(song)
      assertSong(song, selection)
    }
  }

  @Test
  fun `partial selection 2`() {
    addSong()
    transaction.executeWithoutResult {
      val selection =
        SongDSL.select {
          this.producer {
            this.employees {
              this.company { this.employees {} }
              this.person {
                this.companies {
                  this.company {}
                  this.person {}
                }
                this.address {}
              }
            }
          }
        }
      val song = songRepo.select(selection).firstOrNull()
      assertNotNull(song)
      assertSong(song, selection)
    }
  }

  @Test
  fun `partial selection 3`() {
    addSong()
    transaction.executeWithoutResult {
      val selection =
        SongDSL.select {
          this.interpret {
            this.companies {
              this.company {}
              this.person {}
            }
          }
          this.producer {}
        }
      val song = songRepo.select(selection).firstOrNull()
      assertNotNull(song)
      assertSong(song, selection)
    }
  }

  @Test
  fun `inline value class and converter`() {
    transaction.executeWithoutResult {
      val id = UUID.randomUUID()
      val fooBar = FooBar("foo", "bar")
      val foxtrot = Foxtrot("foxtrot")
      weirdClassRepo.persist(WeirdClass(id, fooBar, foxtrot))

      val w = weirdClassRepo.select().firstOrNull()

      assertNotNull(w)
      assertEquals(id, w.id)
      assertEquals(fooBar, w.fooBar)
      assertEquals(foxtrot, w.foxtrot)
    }
  }

  private fun addSong() {
    transaction.executeWithoutResult {
      val address1 = addressRepo.persist(Address(UUID.randomUUID(), "Street 1"))
      val address2 = addressRepo.persist(Address(UUID.randomUUID(), "Street 2"))
      val person1 = personRepo.persist(Person(UUID.randomUUID(), "A", "A", null, address1.id, null))
      val person2 = personRepo.persist(Person(UUID.randomUUID(), "B", "B", null, address2.id, null))
      val company = companyRepo.persist(Company(UUID.randomUUID(), null))
      val company2 = companyRepo.persist(Company(UUID.randomUUID(), null))
      // companyPersonRepository.persist(CompanyPerson(CompanyPersonId(company.id, person1.id),
      // null, null))
      companyPersonRepo.persist(CompanyPerson(CompanyPersonId(company.id, person1.id), null, null))
      companyPersonRepo.persist(CompanyPerson(CompanyPersonId(company.id, person2.id), null, null))
      companyPersonRepo.persist(CompanyPerson(CompanyPersonId(company2.id, person1.id), null, null))
      songRepo.persist(Song(UUID.randomUUID(), "Song", person1.id, null, company.id, null))
    }
  }

  private fun assertSong(song: Song, selection: BeeSelection) {
    val interpret = song.interpret
    if (selection.contains(Song::interpret.name)) {
      assertNotNull(interpret)
      val subselect = selection.subSelect(Song::interpret.name)
      if (subselect != null) assertPerson(interpret, subselect)
    } else {
      assertNull(interpret)
    }
    val producer = song.producer
    if (selection.contains(Song::producer.name)) {
      assertNotNull(producer)
      val subselect = selection.subSelect(Song::producer.name)
      if (subselect != null) assertCompany(producer, subselect)
    } else {
      assertNull(producer)
    }
  }

  private fun assertPerson(person: Person, selection: BeeSelection) {
    val companyPersons = person.companies
    if (selection.contains(Person::companies.name)) {
      assertNotNull(companyPersons)
      val subselect = selection.subSelect(Person::companies.name)
      if (subselect != null)
        for (companyPerson in companyPersons) assertCompanyPerson(companyPerson, subselect)
    } else {
      assertTrue { companyPersons.isNullOrEmpty() }
    }
    val address = person.address
    if (selection.contains(Person::address.name)) {
      assertNotNull(address)
    } else {
      assertNull(address)
    }
  }

  private fun assertCompany(company: Company, selection: BeeSelection) {
    val companyPersons = company.employees
    if (selection.contains(Company::employees.name)) {
      assertNotNull(companyPersons)
      val subselect = selection.subSelect(Company::employees.name)
      if (subselect != null)
        for (companyPerson in companyPersons) assertCompanyPerson(companyPerson, subselect)
    } else {
      assertTrue { companyPersons.isNullOrEmpty() }
    }
  }

  private fun assertCompanyPerson(companyPerson: CompanyPerson, selection: BeeSelection) {
    val company = companyPerson.company
    if (selection.contains(CompanyPerson::company.name)) {
      assertNotNull(company)
      val subselect = selection.subSelect(CompanyPerson::company.name)
      if (subselect != null) assertCompany(company, subselect)
    } else {
      assertNull(company)
    }
    val person = companyPerson.person
    if (selection.contains(CompanyPerson::person.name)) {
      assertNotNull(person)
      val subselect = selection.subSelect(CompanyPerson::person.name)
      if (subselect != null) assertPerson(person, subselect)
    } else {
      assertNull(person)
    }
  }
}
