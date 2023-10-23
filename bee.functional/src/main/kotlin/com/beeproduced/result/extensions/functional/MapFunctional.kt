package com.beeproduced.result.extensions.functional

import com.beeproduced.result.errors.AppError
import com.github.michaelbull.result.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-01-20
 */


// @OptIn(ExperimentalContracts::class)
// inline fun <V, A, B> Result<V, AppError>.mapPair(
//     transform: (V) -> Pair<A, B>
// ): Result<Pair<A, B>, AppError> {
//     contract {
//         callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
//     }
//
//     return when (this) {
//         is Ok -> Ok(transform(value))
//         is Err -> this
//     }
// }

@OptIn(ExperimentalContracts::class)
inline fun <V, B> Result<V, AppError>.mapToPair(
    transform: (V) -> B
): Result<Pair<V, B>, AppError> {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }

    return when (this) {
        is Ok -> Ok(Pair(value, transform(value)))
        is Err -> this
    }
}

fun <V, B> Result<V, AppError>.mapToPair(
    second: B
): Result<Pair<V, B>, AppError> {
    return when (this) {
        is Ok -> Ok(Pair(value, second))
        is Err -> this
    }
}

// @OptIn(ExperimentalContracts::class)
// inline fun <V, A, B, C> Result<V, AppError>.mapTriple(
//     transform: (V) -> Triple<A, B, C>
// ): Result<Triple<A, B, C>, AppError> {
//     contract {
//         callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
//     }
//
//     return when (this) {
//         is Ok -> Ok(transform(value))
//         is Err -> this
//     }
// }

@OptIn(ExperimentalContracts::class)
inline fun <V, B, C> Result<V, AppError>.mapToTriple(
    transform: (V) -> Pair<B, C>
): Result<Triple<V, B, C>, AppError> {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }

    return when (this) {
        is Ok -> {
            val (b, c) = transform(value)
            Ok(Triple(value, b, c))
        }

        is Err -> this
    }
}

fun <V, B, C> Result<V, AppError>.mapToTriple(
    second: B, third: C
): Result<Triple<V, B, C>, AppError> {
    return when (this) {
        is Ok -> {
            Ok(Triple(value, second, third))
        }

        is Err -> this
    }
}

@OptIn(ExperimentalContracts::class)
inline fun <A, B, U> Result<Pair<A, B>, AppError>.mapWithPair(
    transform: (A, B) -> U
): Result<U, AppError> {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }

    return when (this) {
        is Ok -> Ok(transform(value.first, value.second))
        is Err -> this
    }
}

@OptIn(ExperimentalContracts::class)
inline fun <A, B, C, U> Result<Triple<A, B, C>, AppError>.mapWithTriple(
    transform: (A, B, C) -> U
): Result<U, AppError> {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }

    return when (this) {
        is Ok -> Ok(transform(value.first, value.second, value.third))
        is Err -> this
    }
}

@OptIn(ExperimentalContracts::class)
inline fun <A, B, U> Result<Pair<A, B>, AppError>.andThenWithPair(
    transform: (A, B) -> Result<U, AppError>
): Result<U, AppError> {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }

    return when (this) {
        is Ok -> transform(value.first, value.second)
        is Err -> this
    }
}

@OptIn(ExperimentalContracts::class)
inline fun <V, B> Result<V, AppError>.andThenToPair(
    transform: (V) -> Result<B, AppError>
): Result<Pair<V, B>, AppError> {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }

    return when (this) {
        is Ok -> transform(value).map { b -> Pair(value, b) }
        is Err -> this
    }
}

@OptIn(ExperimentalContracts::class)
inline fun <A, B, C, U> Result<Triple<A, B, C>, AppError>.andThenWithTriple(
    transform: (A, B, C) -> Result<U, AppError>
): Result<U, AppError> {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }

    return when (this) {
        is Ok -> transform(value.first, value.second, value.third)
        is Err -> this
    }
}

@OptIn(ExperimentalContracts::class)
inline fun <V, B, C> Result<V, AppError>.andThenToTriple(
    transform: (V) -> Result<Pair<B, C>, AppError>
): Result<Triple<V, B, C>, AppError> {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }

    return when (this) {
        is Ok -> transform(value).map { (b, c) -> Triple(value, b, c) }
        is Err -> this
    }
}

fun <A, B> Result<Pair<A, B>, AppError>.reversePair(): Result<Pair<B, A>, AppError> {
    return when (this) {
        is Ok -> Ok(Pair(value.second, value.first))
        is Err -> this
    }
}

fun <A, B, C> Result<Triple<A, B, C>, AppError>.reverseTriple(): Result<Triple<C, B, A>, AppError> {
    return when (this) {
        is Ok -> Ok(Triple(value.third, value.second, value.first))
        is Err -> this
    }
}

