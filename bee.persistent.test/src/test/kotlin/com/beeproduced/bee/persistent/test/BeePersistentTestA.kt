package com.beeproduced.bee.persistent.test

import com.beeproduced.bee.persistent.application.Application
import com.beeproduced.bee.persistent.blaze.dsl.entity.Path
import com.beeproduced.bee.persistent.blaze.dsl.entity.ValuePath
import com.beeproduced.bee.persistent.blaze.dsl.expression.builder.lower
import com.beeproduced.bee.persistent.blaze.dsl.predicate.builder.and
import com.beeproduced.bee.persistent.blaze.dsl.predicate.builder.or
import com.beeproduced.bee.persistent.blaze.meta.dsl.InlineValueUnwrapper
import com.beeproduced.bee.persistent.blaze.meta.dsl.InlineValueUnwrappers
import com.beeproduced.bee.persistent.blaze.selection.BeeSelection
import com.beeproduced.datasource.a.*
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.lang.invoke.LambdaMetafactory
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
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
    val weirdRepository: WeirdClassRepository,
    @Autowired
    val circularRepository: CircularRepository
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

            data class Wow(val s: String)

            val test = Wow::s
            val testName = test.name

            println("hey?")


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

    @Test
    fun `test circular`() {
        transaction.executeWithoutResult {
            val id = UUID.randomUUID()
            em.beePersist(Circular(id, id, null))

            val selection = BeeSelection.create {
                field("circular") {
                    field("circular") {
                        field("circular")
                    }
                }
            }
            val circles = circularRepository.select(selection)
            val circle = circles.firstOrNull()
            assertNotNull(circle)
            val circleD1 = circle.circular
            assertNotNull(circleD1)
            val circleD2 = circleD1.circular
            assertNotNull(circleD2)
            assertNull(circleD2.circular)
        }
    }

    @Test
    fun `test circular 2`() {
        transaction.executeWithoutResult {
            val id1 = UUID.randomUUID()
            val id2 = UUID.randomUUID()
            em.beePersist(Circular(id1, null, null))
            em.beePersist(Circular(id2, id1, null))
            circularRepository.cbf.update(em, Circular::class.java)
                .set("cId", id2)
                .where("id").eq(id1)
                .executeUpdate()

            val selection = BeeSelection.create {
                field("circular") {
                    field("circular") {
                        field("circular")
                    }
                }
            }
            val circles = circularRepository.select(selection)
            val circle1 = circles.firstOrNull { it.id == id1 }
            assertNotNull(circle1)
            val circle1D1 = circle1.circular
            assertNotNull(circle1D1)
            assertEquals(id2, circle1D1.id)
            val circle1D2 = circle1D1.circular
            assertNotNull(circle1D2)
            assertEquals(id1, circle1D2.id)
            assertNull(circle1D2.circular)

            val circle2 = circles.firstOrNull { it.id == id2 }
            assertNotNull(circle2)
            val circle2D1 = circle2.circular
            assertNotNull(circle2D1)
            assertEquals(id1, circle2D1.id)
            val circle2D2 = circle2D1.circular
            assertNotNull(circle2D2)
            assertEquals(id2, circle2D2.id)
            assertNull(circle2D2.circular)
        }
    }

    @Test
    fun `test circular 3`() {
        transaction.executeWithoutResult {
            val id1 = UUID.randomUUID()
            em.beePersist(Circular(id1, id1, null))

            val selection = BeeSelection.create { }
            val circles = circularRepository.select(selection)
            val circle = circles.firstOrNull()
            assertNotNull(circle)
            assertNull(circle.circular)
        }
    }

    @Test
    fun `test where`() {
        transaction.executeWithoutResult {
            val id1 = UUID.randomUUID()
            val id2 = UUID.randomUUID()
            em.beePersist(Circular(id1, null, null))
            em.beePersist(Circular(id2, id1, null))
            circularRepository.cbf.update(em, Circular::class.java)
                .set("cId", id2)
                .where("id").eq(id1)
                .executeUpdate()

            val circles = circularRepository.select {
                where(Path<UUID>("id").eq(id2))
            }

            assertEquals(1, circles.size)
            val circle = circles.first()
            assertEquals(id2, circle.id)

            val circles2 = circularRepository.select {
                whereAnd(
                    Path<UUID>("id").eq(id1),
                    Path<UUID>("id").eq(id2),
                )
                // where(Path<UUID>("id").equal(id2))
            }

            val circles3 = circularRepository.select {
                whereOr(
                    Path<UUID>("id").eq(id1),
                    and(
                        Path<UUID>("id").eq(id2),
                        Path<UUID>("id").eq(UUID.randomUUID())
                    )
                )
                // where(Path<UUID>("id").equal(id2))
            }

            val circles4 = circularRepository.select {
                whereOr(
                    Path<UUID>("id").eq(id1),
                    or(
                        Path<UUID>("id").eq(id2),
                        Path<UUID>("id").eq(UUID.randomUUID())
                    )
                )
                // where(Path<UUID>("id").equal(id2))
            }

            val circles5 = circularRepository.select {
                whereAnd(
                    Path<UUID>("id").eq(id1),
                    and(
                        Path<UUID>("id").eq(id2),
                        Path<UUID>("id").eq(UUID.randomUUID())
                    )
                )
            }

            val circles6 = circularRepository.select {
                whereAnd(
                    Path<UUID>("id").eq(id1),
                    or(
                        Path<UUID>("id").eq(id2),
                        Path<UUID>("id").eq(UUID.randomUUID())
                    )
                )
            }

            println("baum")

        }
    }



    @Test
    fun `more where`() {
        val clazz = Foxtrot::class.java
        val method = clazz.getDeclaredMethod("unbox-impl")
        val lookup = MethodHandles.privateLookupIn(clazz, MethodHandles.lookup())
        val methodHandle = lookup.unreflect(method)

        val unwrap = LambdaMetafactory.metafactory(
            lookup,
            InlineValueUnwrapper::unwrap.name,
            MethodType.methodType(InlineValueUnwrapper::class.java),
            MethodType.methodType(Any::class.java, Any::class.java),
            methodHandle,
            MethodType.methodType(String::class.java, Foxtrot::class.java),
        ).target.invokeExact() as InlineValueUnwrapper

        InlineValueUnwrappers.unwrappers[Foxtrot::class.qualifiedName!!] = unwrap

        transaction.executeWithoutResult {
            val id = UUID.randomUUID()
            val fooBar = FooBar("foo", "bar")
            val foxtrot = Foxtrot("Foxtrot")
            em.beePersist(WeirdClass(id, fooBar, foxtrot))
            val id2 = UUID.randomUUID()
            val fooBar2 = FooBar("foo2", "bar2")
            val foxtrot2 = Foxtrot("foxtrot2")
            em.beePersist(WeirdClass(id2, fooBar2, foxtrot2))

            val w = weirdRepository.select {
                where(
                    ValuePath<Foxtrot, String>("foxtrot", Foxtrot::class
                ).eq(Foxtrot("Foxtrot")))
            }.firstOrNull()

            val w2 = weirdRepository.select {
                where(lower(
                    ValuePath("foxtrot", Foxtrot::class)
                ).eq("foxtrot"))

            }.firstOrNull()

            println("baum")

            // assertNotNull(w)
            // assertEquals(id, w.id)
            // assertEquals(fooBar, w.fooBar)
            // assertEquals(foxtrot, w.foxtrot)
        }
    }

    fun addSong() {
        transaction.executeWithoutResult {
            val address1 = em.beePersist(Address(UUID.randomUUID(), "Street 1"))
            val address2 = em.beePersist(Address(UUID.randomUUID(), "Street 2"))
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
            circularRepository.cbf.delete(em, Circular::class.java).executeUpdate()
        }
    }
}