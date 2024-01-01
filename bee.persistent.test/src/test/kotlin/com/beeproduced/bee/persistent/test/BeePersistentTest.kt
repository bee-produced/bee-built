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
class BeePersistentTest(
    @Qualifier("aEM")
    val em: EntityManager,
    @Qualifier("aTM")
    transactionManager: PlatformTransactionManager,
    @Autowired
    val songRepository: SongRepository
) {
    private val transaction = TransactionTemplate(transactionManager)

    @Test
    fun `empty selection`() {
        addSong()
        transaction.executeWithoutResult {
            val selection = BeeSelection.create {  }
            val songs = songRepository.select(selection)
                as List<ComBeeproducedDatasourceASong__View__ComBeeproducedDatasourceASong__Core>
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
            val selection = BeeSelection.create {
                field("interpret") {
                    field("companies") {
                        field("company") {
                            field("employees") {
                                field("company")
                                field("person")
                            }
                        }
                        // field("person") {
                        //     field("companies") {
                        //         field("company")
                        //         field("person")
                        //     }
                        // }
                    }
                }
                field("producer") {
                    field("employees") {
                        field("company") {
                            field("employees") {
                                field("company")
                                field("person")
                            }
                        }
                        // field("person") {
                        //     field("companies") {
                        //         field("company")
                        //         field("person")
                        //     }
                        // }
                    }
                }
            }
            val songs = songRepository.select(selection)
                as List<ComBeeproducedDatasourceASong__View__ComBeeproducedDatasourceASong__Core>
            assertTrue { songs.isNotEmpty() }
            val song = songs.first()
            assertNotNull(song.interpret)
            assertNotNull(song.producer)
        }
    }

    fun addSong() {
        transaction.executeWithoutResult {
            val person1 = em.beePersist(Person(UUID.randomUUID(), "A", "A"))
            val person2 = em.beePersist(Person(UUID.randomUUID(), "B", "B"))
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
        }
    }
}