package com.beeproduced.bee.functional.persistent.transactional

import java.lang.annotation.Inherited
import org.springframework.core.annotation.AliasFor
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Propagation

/**
 * @author Kacper Urbaniec
 * @version 2023-02-16
 */
@Target(
  AnnotationTarget.CLASS,
  AnnotationTarget.FUNCTION,
  AnnotationTarget.PROPERTY_GETTER,
  AnnotationTarget.PROPERTY_SETTER,
)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
@MustBeDocumented
annotation class TransactionalResult(
  /**
   * Alias for [.transactionManager].
   *
   * @see .transactionManager
   */
  @get:AliasFor("transactionManager") val value: String = "",
  /**
   * A *qualifier* value for the specified transaction.
   *
   * May be used to determine the target transaction manager, matching the qualifier value (or the
   * bean name) of a specific
   * [TransactionManager][org.springframework.transaction.TransactionManager] bean definition.
   *
   * @see .value
   * @see org.springframework.transaction.PlatformTransactionManager
   * @see org.springframework.transaction.ReactiveTransactionManager
   * @since 4.2
   */
  @get:AliasFor("value") val transactionManager: String = "",
  /**
   * The transaction propagation type.
   *
   * Defaults to [Propagation.REQUIRED].
   *
   * @see org.springframework.transaction.interceptor.TransactionAttribute.getPropagationBehavior
   */
  val propagation: Propagation = Propagation.REQUIRED,
  /**
   * The transaction isolation level.
   *
   * Defaults to [Isolation.DEFAULT].
   *
   * Exclusively designed for use with [Propagation.REQUIRED] or [Propagation.REQUIRES_NEW] since it
   * only applies to newly started transactions. Consider switching the
   * "validateExistingTransactions" flag to "true" on your transaction manager if you'd like
   * isolation level declarations to get rejected when participating in an existing transaction with
   * a different isolation level.
   *
   * @see org.springframework.transaction.interceptor.TransactionAttribute.getIsolationLevel
   * @see
   *   org.springframework.transaction.support.AbstractPlatformTransactionManager.setValidateExistingTransaction
   */
  val isolation: Isolation = Isolation.DEFAULT,
  /**
   * The timeout for this transaction (in seconds).
   *
   * Defaults to the default timeout of the underlying transaction system.
   *
   * Exclusively designed for use with [Propagation.REQUIRED] or [Propagation.REQUIRES_NEW] since it
   * only applies to newly started transactions.
   *
   * @return the timeout in seconds
   * @see org.springframework.transaction.interceptor.TransactionAttribute.getTimeout
   */
  val timeout: Int = TransactionDefinition.TIMEOUT_DEFAULT,
  /**
   * A boolean flag that can be set to `true` if the transaction is effectively read-only, allowing
   * for corresponding optimizations at runtime.
   *
   * Defaults to `false`.
   *
   * This just serves as a hint for the actual transaction subsystem; it will *not necessarily*
   * cause failure of write access attempts. A transaction manager which cannot interpret the
   * read-only hint will *not* throw an exception when asked for a read-only transaction but rather
   * silently ignore the hint.
   *
   * @see org.springframework.transaction.interceptor.TransactionAttribute.isReadOnly
   * @see
   *   org.springframework.transaction.support.TransactionSynchronizationManager.isCurrentTransactionReadOnly
   */
  val readOnly: Boolean = false,
  /**
   * Determines what description the returned [com.beeproduced.result.errors.InternalError] will
   * have when an exception is thrown in a method surrounded by [TransactionalResult].
   */
  val exceptionDescription: String = "Transaction failed",
)
