package com.beeproduced.bee.persistent.test

import com.beeproduced.bee.persistent.application.Application
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import com.beeproduced.bee.persistent.blaze.selection.BeeSelection
import com.beeproduced.datasource.a.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-01
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [Application::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BeePersistentTestA(
    @Qualifier("aEM")
    val em: EntityManager,
    @Qualifier("aTM")
    transactionManager: PlatformTransactionManager,
    @Autowired
    val songRepository: SongRepository,
    @Autowired
    val weirdRepository: WeirdClassRepository
) {
    private val transaction = TransactionTemplate(transactionManager)

    @Test
    fun `empty selection`() {
        addSong()
        transaction.executeWithoutResult {
            val selection = BeeSelection.create {  }
            val songs = songRepository.select(selection)
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
            // TODO: Bring back fullNonRecursiveSelection? Hmm...
            val selection = BeeSelection.create {
                field("interpret") {
                    field("companies") {
                        field("company") {
                            field("employees") {
                                field("company") {
                                    // TODO: When no field is set, relation will not be loaded...
                                    field("id")
                                }
                                field("person") {
                                    field("id")
                                }
                            }
                        }
                        field("person") {
                            field("companies") {
                                field("id")
                            }
                            field("address") { field("id") }
                        }
                    }
                    field("address") { field("id") }
                }
                field("producer") {
                    field("employees") {
                        field("company") {
                            field("employees") {
                                field("id")
                            }
                        }
                        field("person") {
                            field("companies") {
                                field("company") {
                                    field("id")
                                }
                                field("person") {
                                    field("id")
                                }
                            }
                            field("address") { field("id") }
                        }

                    }
                }
            }
            val songs = songRepository.select(selection)
            assertTrue { songs.isNotEmpty() }
            val song = songs.first()

            val interpret = song.interpret
            assertNotNull(interpret)
            assertNotNull(interpret.address)
            val interpretCompanies = interpret.companies?.firstOrNull()
            assertNotNull(interpretCompanies)
            val interpretCompaniesCompany = interpretCompanies.company
            assertNotNull(interpretCompaniesCompany)
            val iCCE = interpretCompaniesCompany.employees?.firstOrNull()
            assertNotNull(iCCE)
            val iCCEC = iCCE.company
            assertNotNull(iCCEC)
            val iCCEP = iCCE.person
            assertNotNull(iCCEP)
            assertNull(iCCEP.address)
            val interpretCompaniesPerson = interpretCompanies.person
            assertNotNull(interpretCompaniesPerson)
            assertNotNull(interpretCompaniesPerson.address)
            val iCPE = interpretCompaniesPerson.companies?.firstOrNull()
            assertNotNull(iCPE)

            val producer = song.producer
            assertNotNull(producer)
            val producerEmployees = producer.employees?.firstOrNull()
            assertNotNull(producerEmployees)
            val producerEmployeesCompany = producerEmployees.company
            assertNotNull(producerEmployeesCompany)
            val pECE = producerEmployeesCompany.employees?.firstOrNull()
            assertNotNull(pECE)
            val producerEmployeesPerson = producerEmployees.person
            assertNotNull(producerEmployeesPerson)
            assertNotNull(producerEmployeesPerson.address)
            val pEPC = producerEmployeesPerson.companies?.firstOrNull()
            assertNotNull(pEPC)
            val pEPCC = pEPC.company
            assertNotNull(pEPCC)
            val pEPCP = pEPC.person
            assertNotNull(pEPCP)
            assertNull(pEPCP.address)
        }
    }

    @Test
    fun `partial selection 1`() {
        addSong()
        transaction.executeWithoutResult {
            val selection = BeeSelection.create {
                field("interpret") {
                    field("companies") {
                        field("company") {
                            field("employees") {
                                field("company") {
                                    field("id")
                                }
                                field("person") {
                                    field("id")
                                }
                            }
                        }
                        field("person") {
                            field("companies") {
                                field("id")
                            }
                            field("address") { field("id") }
                        }
                    }
                }
            }
            val songs = songRepository.select(selection)
            assertTrue { songs.isNotEmpty() }
            val song = songs.first()

            val interpret = song.interpret
            assertNotNull(interpret)
            assertNull(interpret.address)
            val interpretCompanies = interpret.companies?.firstOrNull()
            assertNotNull(interpretCompanies)
            val interpretCompaniesCompany = interpretCompanies.company
            assertNotNull(interpretCompaniesCompany)
            val iCCE = interpretCompaniesCompany.employees?.firstOrNull()
            assertNotNull(iCCE)
            assertNotNull(iCCE.company)
            assertNotNull(iCCE.person)
            val interpretCompaniesPerson = interpretCompanies.person
            assertNotNull(interpretCompaniesPerson)
            assertNotNull(interpretCompaniesPerson.address)
            val iCPE = interpretCompaniesPerson.companies?.firstOrNull()
            assertNotNull(iCPE)

            assertNull(song.producer)
        }
    }

    @Test
    fun `partial selection 2`() {
        addSong()
        transaction.executeWithoutResult {
            val selection = BeeSelection.create {
                field("producer") {
                    field("employees") {
                        field("company") {
                            field("employees") {
                                field("id")
                            }
                        }
                        field("person") {
                            field("companies") {
                                field("company") {
                                    field("id")
                                }
                                field("person") {
                                    field("id")
                                }
                            }
                            field("address") { field("id") }
                        }
                    }
                }
            }
            val songs = songRepository.select(selection)
            assertTrue { songs.isNotEmpty() }
            val song = songs.first()

            val interpret = song.interpret
            assertNull(interpret)

            val producer = song.producer
            assertNotNull(producer)
            val producerEmployees = producer.employees?.firstOrNull()
            assertNotNull(producerEmployees)
            val producerEmployeesCompany = producerEmployees.company
            assertNotNull(producerEmployeesCompany)
            val pECE = producerEmployeesCompany.employees?.firstOrNull()
            assertNotNull(pECE)
            val producerEmployeesPerson = producerEmployees.person
            assertNotNull(producerEmployeesPerson)
            assertNotNull(producerEmployeesPerson.address)
            val pEPC = producerEmployeesPerson.companies?.firstOrNull()
            assertNotNull(pEPC)
            val pEPCC = pEPC.company
            assertNotNull(pEPCC)
            val pEPCP = pEPC.person
            assertNotNull(pEPCP)
        }
    }

    @Test
    fun `partial selection 3`() {
        addSong()
        transaction.executeWithoutResult {
            // TODO: Bring back fullNonRecursiveSelection? Hmm...
            val selection = BeeSelection.create {
                field("interpret") {
                    field("companies") {
                        field("company") {
                            field("id")
                        }
                        field("person") {
                            field("companies") {
                                field("id")
                            }
                        }
                    }
                }
                field("producer") {
                    field("id")
                }
            }
            val songs = songRepository.select(selection)
            assertTrue { songs.isNotEmpty() }
            val song = songs.first()

            val interpret = song.interpret
            assertNotNull(interpret)
            val interpretCompanies = interpret.companies?.firstOrNull()
            assertNotNull(interpretCompanies)
            val interpretCompaniesCompany = interpretCompanies.company
            assertTrue { interpretCompaniesCompany?.employees.isNullOrEmpty() }
            val interpretCompaniesPerson = interpretCompanies.person
            assertNotNull(interpretCompaniesPerson)
            val iCPE = interpretCompaniesPerson.companies?.firstOrNull()
            assertNotNull(iCPE)

            val producer = song.producer
            assertNotNull(producer)
            assertTrue { producer.employees.isNullOrEmpty() }
        }
    }

    @Test
    fun `test value class and converter`() {
        transaction.executeWithoutResult {
            val id = UUID.randomUUID()
            val fooBar = FooBar("foo", "bar")
            val foxtrot = Foxtrot("foxtrot")
            em.beePersist(WeirdClass(id, fooBar, foxtrot))

            val ws = weirdRepository.select(BeeSelection.create {  })
            val w = ws.firstOrNull()

            assertNotNull(w)
            assertEquals(id, w.id)
            assertEquals(fooBar, w.fooBar)
            assertEquals(foxtrot, w.foxtrot)
        }
    }

    fun addSong() {
        transaction.executeWithoutResult {
            val address1 = em.beePersist(Address(UUID.randomUUID(), "Street 1"))
            val address2 = em.beePersist(Address(UUID.randomUUID(), "Street 1"))
            val person1 = em.beePersist(Person(UUID.randomUUID(), "A", "A", null, address1.id, null))
            val person2 = em.beePersist(Person(UUID.randomUUID(), "B", "B", null, address2.id, null))
            val company = em.beePersist(
                Company(UUID.randomUUID(), null)
            )
            val company2 = em.beePersist(
                Company(UUID.randomUUID(), null)
            )
            // companyPersonRepository.persist(CompanyPerson(CompanyPersonId(company.id, person1.id), null, null))
            em.beePersist(CompanyPerson(CompanyPersonId(company.id, person1.id), null, null))
            em.beePersist(CompanyPerson(CompanyPersonId(company.id, person2.id), null, null))
            em.beePersist(CompanyPerson(CompanyPersonId(company2.id, person1.id), null, null))
            em.beePersist(
                Song(
                    UUID.randomUUID(),
                    "Song",
                    person1.id,
                    null,
                    company.id,
                    null
                )
            )
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

    fun clear() {
        transaction.executeWithoutResult {
            songRepository.cbf.delete(em, Song::class.java).executeUpdate()
            songRepository.cbf.delete(em, CompanyPerson::class.java).executeUpdate()
            songRepository.cbf.delete(em, Person::class.java).executeUpdate()
            songRepository.cbf.delete(em, Company::class.java).executeUpdate()
            weirdRepository.cbf.delete(em, WeirdClass::class.java).executeUpdate()
        }
    }
}