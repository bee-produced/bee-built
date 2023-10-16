package com.beeproduced.bee.fetched.test

import com.beeproduced.bee.fetched.graphql.client.*
import com.beeproduced.bee.fetched.graphql.dto.*
import com.beeproduced.example.application.Application
import com.netflix.graphql.dgs.DgsQueryExecutor
import com.netflix.graphql.dgs.client.codegen.BaseSubProjectionNode
import com.netflix.graphql.dgs.client.codegen.GraphQLQueryRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-10-14
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [Application::class]
)
class BeeFetchedTest {
    @Autowired
    private lateinit var dgsQueryExecutor: DgsQueryExecutor

    // SafeMode = true
    // ====================================================================

    @Test
    fun `query singular id dataloader (foo)`() {
        val query = GraphQLQueryRequest(
            FooGraphQLQuery(),
            FooProjectionRoot()
                .waldoId()
                .waldo().select {
                    waldo()
                }
        ).serialize()

        val foo = dgsQueryExecutor.executeAndExtractJsonPathAsObject(
            query, "data.foo", emptyMap(), Foo::class.java
        )

        assertNotNull(foo)
        val waldo = foo.waldo
        assertNotNull(waldo)
        assertEquals("Foo", waldo.waldo)
    }

    @Test
    fun `query plural ids dataloader (bar)`() {
        val query = GraphQLQueryRequest(
            BarGraphQLQuery(),
            BarProjectionRoot()
                .waldoIds()
                .waldos().select {
                    waldo()
                }
        ).serialize()

        val bar = dgsQueryExecutor.executeAndExtractJsonPathAsObject(
            query, "data.bar", emptyMap(), Bar::class.java
        )

        assertNotNull(bar)
        val waldos = bar.waldos
        assertNotNull(waldos)
        waldos.forEach { waldo -> assertEquals("Bar", waldo.waldo) }
    }

    @Test
    fun `query singular nullable id dataloader (qux)`() {
        val query = GraphQLQueryRequest(
            QuxGraphQLQuery(),
            QuxProjectionRoot()
                .waldoId()
                .waldo().select {
                    waldo()
                }
        ).serialize()

        val qux = dgsQueryExecutor.executeAndExtractJsonPathAsObject(
            query, "data.qux", emptyMap(), Qux::class.java
        )

        assertNotNull(qux)
        val waldo = qux.waldo
        assertNotNull(waldo)
        assertEquals("Qux", waldo.waldo)
    }

    @Test
    fun `query plural nullable ids dataloader (quux)`() {
        val query = GraphQLQueryRequest(
            QuuxGraphQLQuery(),
            QuuxProjectionRoot()
                .waldoIds()
                .waldos().select {
                    waldo()
                }
        ).serialize()

        val quux = dgsQueryExecutor.executeAndExtractJsonPathAsObject(
            query, "data.quux", emptyMap(), Quux::class.java
        )

        assertNotNull(quux)
        val waldos = quux.waldos
        assertNotNull(waldos)
        waldos.forEach { waldo -> assertEquals("Quux", waldo.waldo) }
    }

    @Test
    fun `query singular unrelated id dataloader (corge)`() {
        val query = GraphQLQueryRequest(
            CorgeGraphQLQuery(),
            CorgeProjectionRoot()
                .corgeToWaldoId()
                .waldo().select {
                    waldo()
                }
        ).serialize()

        val corge = dgsQueryExecutor.executeAndExtractJsonPathAsObject(
            query, "data.corge", emptyMap(), Corge::class.java
        )

        assertNotNull(corge)
        val waldo = corge.waldo
        assertNotNull(waldo)
        assertEquals("Corge", waldo.waldo)
    }

    @Test
    fun `query singular id internal type dataloader (grault)`() {
        val query = GraphQLQueryRequest(
            GraultGraphQLQuery(),
            GraultProjectionRoot()
                .waldo().select {
                    waldo()
                }
        ).serialize()

        val grault = dgsQueryExecutor.executeAndExtractJsonPathAsObject(
            query, "data.grault", emptyMap(), Grault::class.java
        )

        assertNotNull(grault)
        val waldo = grault.waldo
        assertNotNull(waldo)
        assertEquals("Grault", waldo.waldo)
    }

    @Test
    fun `query singular nullable id internal type dataloader (fred)`() {
        val query = GraphQLQueryRequest(
            FredGraphQLQuery(),
            FredProjectionRoot()
                .waldo().select {
                    waldo()
                }
        ).serialize()

        val fred = dgsQueryExecutor.executeAndExtractJsonPathAsObject(
            query, "data.fred", emptyMap(), Fred::class.java
        )

        assertNotNull(fred)
        val waldo = fred.waldo
        assertNotNull(waldo)
        assertEquals("Fred", waldo.waldo)
    }

    @Test
    fun `query plural nullable ids internal type dataloader (xyzzy)`() {
        val query = GraphQLQueryRequest(
            XyzzyGraphQLQuery(),
            XyzzyProjectionRoot()
                .waldos().select {
                    waldo()
                }
        ).serialize()

        val xyzzy = dgsQueryExecutor.executeAndExtractJsonPathAsObject(
            query, "data.xyzzy", emptyMap(), Xyzzy::class.java
        )

        assertNotNull(xyzzy)
        val waldos = xyzzy.waldos
        assertNotNull(waldos)
        waldos.forEach { waldo -> assertEquals("Xyzzy", waldo.waldo) }
    }

    @Test
    fun `query plural ids internal type dataloader (plugh)`() {
        val query = GraphQLQueryRequest(
            PlughGraphQLQuery(),
            PlughProjectionRoot()
                .waldos().select {
                    waldo()
                }
        ).serialize()

        val plugh = dgsQueryExecutor.executeAndExtractJsonPathAsObject(
            query, "data.plugh", emptyMap(), Plugh::class.java
        )

        assertNotNull(plugh)
        val waldos = plugh.waldos
        assertNotNull(waldos)
        waldos.forEach { waldo -> assertEquals("Plugh", waldo.waldo) }
    }

    @Test
    fun `query not generated dataloader (garply)`() {
        val query = GraphQLQueryRequest(
            GarplyGraphQLQuery(),
            GarplyProjectionRoot()
                .waldo().select {
                    waldo()
                }
        ).serialize()

        val garply = dgsQueryExecutor.executeAndExtractJsonPathAsObject(
            query, "data.garply", emptyMap(), Garply::class.java
        )

        assertNotNull(garply)
        val waldo = garply.waldo
        assertNull(waldo)
    }

    // SafeMode = false
    // ====================================================================

    @Test
    fun `query unsafe singular id dataloader (alpha)`() {
        val query = GraphQLQueryRequest(
            AlphaGraphQLQuery(),
            AlphaProjectionRoot()
                .zuluId()
                .zulu().select {
                    zulu()
                }
        ).serialize()

        val alpha = dgsQueryExecutor.executeAndExtractJsonPathAsObject(
            query, "data.alpha", emptyMap(), Alpha::class.java
        )

        assertNotNull(alpha)
        val zulu = alpha.zulu
        assertNotNull(zulu)
        assertEquals("Alpha", zulu.zulu)
    }

    @Test
    fun `query unsafe plural ids dataloader (bravo)`() {
        val query = GraphQLQueryRequest(
            BravoGraphQLQuery(),
            BravoProjectionRoot()
                .zuluIds()
                .zulus().select {
                    zulu()
                }
        ).serialize()

        val bravo = dgsQueryExecutor.executeAndExtractJsonPathAsObject(
            query, "data.bravo", emptyMap(), Bravo::class.java
        )

        assertNotNull(bravo)
        val zulus = bravo.zulus
        assertNotNull(zulus)
        zulus.forEach { zulu -> assertEquals("Bravo", zulu.zulu) }
    }

    @Test
    fun `query unsafe singular nullable id dataloader (charlie)`() {
        val query = GraphQLQueryRequest(
            CharlieGraphQLQuery(),
            CharlieProjectionRoot()
                .zuluId()
                .zulu().select {
                    zulu()
                }
        ).serialize()

        val charlie = dgsQueryExecutor.executeAndExtractJsonPathAsObject(
            query, "data.charlie", emptyMap(), Charlie::class.java
        )

        assertNotNull(charlie)
        val zulu = charlie.zulu
        assertNotNull(zulu)
        assertEquals("Charlie", zulu.zulu)
    }

    @Test
    fun `query unsafe plural nullable ids dataloader (delta)`() {
        val query = GraphQLQueryRequest(
            DeltaGraphQLQuery(),
            DeltaProjectionRoot()
                .zuluIds()
                .zulus().select {
                    zulu()
                }
        ).serialize()

        val delta = dgsQueryExecutor.executeAndExtractJsonPathAsObject(
            query, "data.delta", emptyMap(), Delta::class.java
        )

        assertNotNull(delta)
        val zulus = delta.zulus
        assertNotNull(zulus)
        zulus.forEach { zulu -> assertEquals("Delta", zulu.zulu) }
    }

    @Test
    fun `query unsafe singular unrelated id dataloader (echo)`() {
        val query = GraphQLQueryRequest(
            EchoGraphQLQuery(),
            EchoProjectionRoot()
                .echoToZuluId()
                .zulu().select {
                    zulu()
                }
        ).serialize()

        val echo = dgsQueryExecutor.executeAndExtractJsonPathAsObject(
            query, "data.echo", emptyMap(), Echo::class.java
        )

        assertNotNull(echo)
        val zulu = echo.zulu
        assertNotNull(zulu)
        assertEquals("Echo", zulu.zulu)
    }

    @Test
    fun `query unsafe singular id internal type dataloader (foxtrot)`() {
        val query = GraphQLQueryRequest(
            FoxtrotGraphQLQuery(),
            FoxtrotProjectionRoot()
                .zulu().select {
                    zulu()
                }
        ).serialize()

        val foxtrot = dgsQueryExecutor.executeAndExtractJsonPathAsObject(
            query, "data.foxtrot", emptyMap(), Foxtrot::class.java
        )

        assertNotNull(foxtrot)
        val zulu = foxtrot.zulu
        assertNotNull(zulu)
        assertEquals("Foxtrot", zulu.zulu)
    }

    @Test
    fun `query unsafe singular nullable id internal type dataloader (golf)`() {
        val query = GraphQLQueryRequest(
            GolfGraphQLQuery(),
            GolfProjectionRoot()
                .zulu().select {
                    zulu()
                }
        ).serialize()

        val golf = dgsQueryExecutor.executeAndExtractJsonPathAsObject(
            query, "data.golf", emptyMap(), Golf::class.java
        )

        assertNotNull(golf)
        val zulu = golf.zulu
        assertNotNull(zulu)
        assertEquals("Golf", zulu.zulu)
    }

    @Test
    fun `query unsafe plural ids internal type dataloader (hotel)`() {
        val query = GraphQLQueryRequest(
            HotelGraphQLQuery(),
            HotelProjectionRoot()
                .zulus().select {
                    zulu()
                }
        ).serialize()

        val hotel = dgsQueryExecutor.executeAndExtractJsonPathAsObject(
            query, "data.hotel", emptyMap(), Hotel::class.java
        )

        assertNotNull(hotel)
        val zulus = hotel.zulus
        assertNotNull(zulus)
        zulus.forEach { zulu -> assertEquals("Hotel", zulu.zulu) }
    }

    @Test
    fun `query unsafe plural nullable ids internal type dataloader (india)`() {
        val query = GraphQLQueryRequest(
            IndiaGraphQLQuery(),
            IndiaProjectionRoot()
                .zulus().select {
                    zulu()
                }
        ).serialize()

        val india = dgsQueryExecutor.executeAndExtractJsonPathAsObject(
            query, "data.india", emptyMap(), Hotel::class.java
        )

        assertNotNull(india)
        val zulus = india.zulus
        assertNotNull(zulus)
        zulus.forEach { zulu -> assertEquals("India", zulu.zulu) }
    }

    @Test
    fun `query unsafe not generated dataloader (juliet)`() {
        val query = GraphQLQueryRequest(
            JulietGraphQLQuery(),
            JulietProjectionRoot()
                .zulu().select {
                    zulu()
                }
        ).serialize()

        val juliet = dgsQueryExecutor.executeAndExtractJsonPathAsObject(
            query, "data.juliet", emptyMap(), Juliet::class.java
        )

        assertNotNull(juliet)
        val zulu = juliet.zulu
        assertNull(zulu)
    }
}

inline fun <P, reified N : BaseSubProjectionNode<P, *>> N.select(selection: N.() -> Unit): P {
    selection()
    return this.parent()
}