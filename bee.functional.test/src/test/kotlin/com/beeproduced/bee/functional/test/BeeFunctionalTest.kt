package com.beeproduced.bee.functional.test

import com.beeproduced.bee.functional.graphql.client.*
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
import kotlin.test.assertTrue

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
class BeeFunctionalTest {
    @Autowired
    private lateinit var dgsQueryExecutor: DgsQueryExecutor

    @Test
    fun `query ok type (foo)`() {
        val query = GraphQLQueryRequest(
            FooGraphQLQuery(),
            projection(::FooProjectionRoot)
                .waldoId()
                .waldo().select {
                    waldo()
                }
        ).serialize()

        val result = dgsQueryExecutor.execute(query)
        assertTrue { result.errors.isEmpty() }
        val data = result.getData<Map<String, Any?>>()
        val foo = data.typeOrNull("foo")
        assertNotNull(foo)
        val waldo = foo.typeOrNull("waldo")
        assertNotNull(waldo)
    }

    @Test
    fun `query ok type with nested err (bar)`() {
        val query = GraphQLQueryRequest(
            BarGraphQLQuery(),
            projection(::BarProjectionRoot)
                .waldoIds()
                .waldos().select {
                    waldo()
                }
        ).serialize()

        val result = dgsQueryExecutor.execute(query)

        val errors = result.errors
        assertEquals(1, errors.count())
        val barWaldoError = errors.first()
        assertEquals("Bar", barWaldoError.message)
        assertTrue { barWaldoError.path.containsAll(listOf("bar", "waldos")) }
        assertEquals("BAD_REQUEST", barWaldoError.extensions["errorType"])

        val data = result.getData<Map<String, Any?>>()
        val bar = data.typeOrNull("bar")
        assertNotNull(bar)
        val waldos = bar["waldos"]
        assertNull(waldos)
    }

    @Test
    fun `query err type (qux)`() {
        val query = GraphQLQueryRequest(
            QuxGraphQLQuery(),
            projection(::QuxProjectionRoot)
                .waldoId()
                .waldo().select {
                    waldo()
                }
        ).serialize()

        val result = dgsQueryExecutor.execute(query)

        val errors = result.errors
        assertEquals(1, errors.count())
        val quxError = errors.first()
        assertEquals("Qux", quxError.message)
        assertTrue { quxError.path.containsAll(listOf("qux")) }
        assertEquals("INTERNAL", quxError.extensions["errorType"])

        val data = result.getData<Map<String, Any?>?>()
        assertNull(data)
    }

}

@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.typeOrNull(typeName: String): Map<String, Any?>? {
    return this[typeName] as? Map<String, Any?>
}

inline fun <P, reified N : BaseSubProjectionNode<P, *>> N.select(selection: N.() -> Unit): P {
    this.selection()
    return this.parent()
}

// Workaround as Kotlin does not support Java diamond operator
// https://netflix.github.io/dgs/generating-code-from-schema/#client-api-v2
inline fun <reified R : BaseSubProjectionNode<Nothing, Nothing>> projection(constructor: ()->R): R {
    return constructor()
}