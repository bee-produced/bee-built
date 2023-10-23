package com.beeproduced.bee.buzz.manager

import com.beeproduced.result.errors.InternalAppError
import com.beeproduced.result.errors.ResultError

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2022-02-10
 */
open class EventError : InternalAppError {
    constructor(
        description: String,
        source: ResultError? = null,
        skipStackTraceElements: Long = 0,
        limitStackTraceElements: Long = 1
    ) : super(description, source, skipStackTraceElements, limitStackTraceElements)

    constructor(
        description: String,
        source: Exception,
        skipStackTraceElements: Long = 0,
        limitStackTraceElements: Long = 1
    ) : super(description, source, skipStackTraceElements, limitStackTraceElements)
}