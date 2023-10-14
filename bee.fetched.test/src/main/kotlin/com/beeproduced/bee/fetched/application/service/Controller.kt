package com.beeproduced.bee.fetched.application.service

import com.beeproduced.bee.fetched.graphql.DgsConstants
import com.beeproduced.bee.fetched.graphql.dto.*
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.DgsQuery

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-10-14
 */
@DgsComponent
class Controller {
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

    data class MyGrault(
        val waldoId: String
    )

    @DgsQuery
    fun grault(): MyGrault {
        return MyGrault("Grault")
    }

    @DgsQuery
    fun garply(): Garply {
        return Garply()
    }

    @DgsData(parentType = DgsConstants.GARPLY.TYPE_NAME, field = DgsConstants.GARPLY.Waldo)
    fun garplyWaldo(): Waldo {
        return Waldo("Garply")
    }
}