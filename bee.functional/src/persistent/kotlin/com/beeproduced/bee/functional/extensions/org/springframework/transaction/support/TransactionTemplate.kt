package com.beeproduced.bee.functional.extensions.org.springframework.transaction.support

import com.beeproduced.bee.functional.result.errors.AppError
import com.beeproduced.bee.functional.result.errors.InternalAppError
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.onFailure
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2022-09-16
 */

open class TransactionError(
    source: Exception,
    skipStackTraceElements: Long = 0,
    limitStackTraceElements: Long = 1
) :
    InternalAppError(
        "Database transaction failed",
        source,
        skipStackTraceElements,
        limitStackTraceElements
    )

val logger: Logger = LoggerFactory.getLogger(TransactionTemplate::class.java)

@OptIn(ExperimentalContracts::class)
inline fun <V> TransactionTemplate.executeToResult(
    crossinline transform: (status: TransactionStatus) -> Result<V, AppError>
): Result<V, AppError> {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }



    return try {
        requireNotNull(
            this.execute { transaction ->
                transform(transaction).onFailure {
                    logger.debug("Transaction failed, rolling back")
                    transaction.setRollbackOnly()
                }
            }
        )
    } catch (ex: Exception) {
        Err(TransactionError(ex))
    }
}

@OptIn(ExperimentalContracts::class)
inline fun <V, U> Result<V, AppError>.andExecuteTransaction(
    transactionTemplate: TransactionTemplate,
    crossinline transform: (transaction: TransactionStatus, V) -> Result<U, AppError>
): Result<U, AppError> {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }

    val value = getOrElse { err -> return Err(err) }
    return try {
        requireNotNull(
            transactionTemplate.execute { transaction ->
                transform(transaction, value).onFailure {
                    logger.debug("Transaction failed, rolling back")
                    transaction.setRollbackOnly()
                }
            }
        )
    } catch (ex: Exception) {
        Err(TransactionError(ex))
    }
}
