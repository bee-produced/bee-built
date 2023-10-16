package com.beeproduced.bee.fetched.application.service

import com.beeproduced.bee.fetched.graphql.dto.*
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-10-14
 */
@DgsComponent
class TestController {
    @DgsQuery
    fun foo(): Foo {
        return Foo("Foo")
    }

    @DgsQuery
    fun bar(): Bar {
        return Bar(listOf("Bar"))
    }

    @DgsQuery
    fun qux(): Qux {
        return Qux("Qux")
    }

    @DgsQuery
    fun quux(): Quux {
        return Quux(listOf("Quux"))
    }

    @DgsQuery
    fun corge(): Corge {
        return Corge("Corge")
    }

    data class MyGrault(val waldoId: String)

    @DgsQuery
    fun grault(): MyGrault {
        return MyGrault("Grault")
    }

    data class MyFred(val waldoId: String?)

    @DgsQuery
    fun fred(): MyFred {
        return MyFred("Fred")
    }

    data class MyPlugh(val waldoIds: List<String>)

    @DgsQuery
    fun plugh(): MyPlugh {
        return MyPlugh(listOf("Plugh"))
    }

    data class MyXyzzy(val waldoIds: List<String>?)

    @DgsQuery
    fun xyzzy(): MyXyzzy {
        return MyXyzzy(listOf("Xyzzy"))
    }

    @DgsQuery
    fun garply(): Garply {
        return Garply()
    }
}