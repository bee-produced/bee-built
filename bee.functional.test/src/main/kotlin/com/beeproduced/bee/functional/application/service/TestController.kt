package com.beeproduced.bee.functional.application.service

import com.beeproduced.bee.fetched.graphql.DgsConstants
import com.beeproduced.bee.fetched.graphql.dto.*
import com.beeproduced.bee.functional.result.AppResult
import com.beeproduced.bee.functional.result.errors.BadRequestError
import com.beeproduced.bee.functional.result.errors.InternalAppError
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
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
class TestController {

    @DgsQuery
    fun foo(): AppResult<Foo> {
        return Ok(Foo("Foo"))
    }

    @DgsData(
        parentType = DgsConstants.FOO.TYPE_NAME,
        field = DgsConstants.FOO.Waldo
    )
    fun fooWaldo(): AppResult<Waldo> {
        return Ok(Waldo("Foo"))
    }

    @DgsQuery
    fun bar(): Bar {
        return Bar(listOf("Bar"))
    }

    @DgsData(
        parentType = DgsConstants.BAR.TYPE_NAME,
        field = DgsConstants.BAR.Waldos
    )
    fun barWaldos(): AppResult<List<Waldo>> {
        return Err(BadRequestError("Bar"))
    }

    @DgsQuery
    fun qux(): AppResult<Qux> {
        return Err(InternalAppError("Qux"))
    }
}