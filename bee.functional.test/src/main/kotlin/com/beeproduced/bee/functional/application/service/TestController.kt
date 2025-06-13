package com.beeproduced.bee.functional.application.service

import com.beeproduced.bee.functional.graphql.DgsConstants
import com.beeproduced.bee.functional.graphql.dto.*
import com.beeproduced.bee.functional.result.AppResult
import com.beeproduced.bee.functional.result.errors.BadRequestError
import com.beeproduced.bee.functional.result.errors.InternalAppError
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.DgsQuery

/**
 * @author Kacper Urbaniec
 * @version 2023-10-14
 */
@DgsComponent
class TestController {

  // ⚠️ Always add parentType + field for endpoints with `AppResult` ⚠️
  // Why? Change in com.netflix.graphql.dgs.internal.DgsSchemaProvider.registerDataFetcher:
  //   val field = dgsDataAnnotation.getString("field").ifEmpty { method.name }
  // For mangled methods this will result not in "foo" but "foo-HJRILA" which is not a valid field
  // Working on fixing this in the future...

  @DgsQuery(field = DgsConstants.QUERY.Foo)
  fun foo(): AppResult<Foo> {
    return Ok(Foo("Foo"))
  }

  @DgsData(parentType = DgsConstants.FOO.TYPE_NAME, field = DgsConstants.FOO.Waldo)
  fun fooWaldo(): AppResult<Waldo> {
    return Ok(Waldo("Foo"))
  }

  @DgsQuery
  fun bar(): Bar {
    return Bar(listOf("Bar"))
  }

  @DgsData(parentType = DgsConstants.BAR.TYPE_NAME, field = DgsConstants.BAR.Waldos)
  fun barWaldos(): AppResult<List<Waldo>> {
    return Err(BadRequestError("Bar"))
  }

  @DgsQuery(field = DgsConstants.QUERY.Qux)
  fun qux(): AppResult<Qux> {
    return Err(InternalAppError("Qux"))
  }
}
