package com.beeproduced.bee.fetched.test

import com.beeproduced.bee.fetched.graphql.client.FooGraphQLQuery
import com.beeproduced.bee.fetched.graphql.client.FooProjectionRoot
import com.beeproduced.bee.fetched.graphql.dto.Foo
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
        assertEquals(waldo.waldo, "Foo")
    }
}

inline fun <P, reified N : BaseSubProjectionNode<P, *>> N.select(selection: N.() -> Unit): P {
    selection()
    return this.parent()
}