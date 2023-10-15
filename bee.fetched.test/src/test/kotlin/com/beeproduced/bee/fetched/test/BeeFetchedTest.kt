package com.beeproduced.bee.fetched.test

import com.beeproduced.bee.fetched.graphql.client.BarGraphQLQuery
import com.beeproduced.bee.fetched.graphql.client.BarProjectionRoot
import com.beeproduced.bee.fetched.graphql.client.CorgeGraphQLQuery
import com.beeproduced.bee.fetched.graphql.client.CorgeProjectionRoot
import com.beeproduced.bee.fetched.graphql.client.FooGraphQLQuery
import com.beeproduced.bee.fetched.graphql.client.FooProjectionRoot
import com.beeproduced.bee.fetched.graphql.client.GarplyGraphQLQuery
import com.beeproduced.bee.fetched.graphql.client.GarplyProjectionRoot
import com.beeproduced.bee.fetched.graphql.client.GraultGraphQLQuery
import com.beeproduced.bee.fetched.graphql.client.GraultProjectionRoot
import com.beeproduced.bee.fetched.graphql.client.QuuxGraphQLQuery
import com.beeproduced.bee.fetched.graphql.client.QuuxProjectionRoot
import com.beeproduced.bee.fetched.graphql.client.QuxGraphQLQuery
import com.beeproduced.bee.fetched.graphql.client.QuxProjectionRoot
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
        assertNotNull(waldo)
        assertEquals("Garply", waldo.waldo)
    }

}

inline fun <P, reified N : BaseSubProjectionNode<P, *>> N.select(selection: N.() -> Unit): P {
    selection()
    return this.parent()
}