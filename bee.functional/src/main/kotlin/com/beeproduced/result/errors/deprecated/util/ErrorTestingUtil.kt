package com.beeproduced.result.errors.deprecated.util

import com.beeproduced.result.errors.deprecated.AppError

// inline should result in better readability during test
inline fun assertError(expected: AppError, actual: AppError) {
    assert(expected::class == actual::class) { "Types do not match, expected: ${expected::class} - actual: ${actual::class}" }
    var curExcpected: AppError = expected
    var curActual: AppError = actual
    while (curExcpected.source != null) {
        curExcpected.source?.let {
            assert(it::class == actual.source!!::class) { "Embedded types do not match, expected: ${it::class} - actual: ${actual.source!!::class}" }
            curExcpected = it
            curActual = actual.source!!
        }
    }
}
