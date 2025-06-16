package com.beeproduced.bee.functional.persistent.transactional

import com.beeproduced.bee.functional.extensions.com.github.michaelbull.result.failureAsErr
import com.beeproduced.bee.functional.extensions.com.github.michaelbull.result.isFailure
import com.beeproduced.bee.functional.result.errors.AppError
import com.beeproduced.bee.functional.result.errors.InternalAppError
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.ApplicationContext
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

@Aspect
@Order(-1)
@Component
@ConditionalOnClass(Result::class)
class TransactionalResultAspect(private val context: ApplicationContext) {
  private val logger = LoggerFactory.getLogger(TransactionalResultAspect::class.java)

  // Previously was
  // "execution(@com.beeproduced.bee.functional.persistent.transactional.TransactionalResult
  //   com.github.michaelbull.result.Result *(..))"
  // However, as Result is an inline value class now, it does not exist at runtime to point…
  @Pointcut(
    "@annotation(com.beeproduced.bee.functional.persistent.transactional.TransactionalResult)"
  )
  fun transactionalMethodReturningResult() = Unit

  @Suppress("UNCHECKED_CAST")
  @Around("transactionalMethodReturningResult() && @annotation(transactionalAnnotation)")
  fun runInTransactionAndRollbackOnErr(
    joinPoint: ProceedingJoinPoint,
    transactionalAnnotation: TransactionalResult,
  ): Any? {
    val transactionManager: PlatformTransactionManager =
      if (transactionalAnnotation.value == "") {
        context.getBean(PlatformTransactionManager::class.java)
      } else {
        context.getBean(transactionalAnnotation.value, PlatformTransactionManager::class.java)
      }

    val template =
      TransactionTemplate(transactionManager).apply {
        isolationLevel = transactionalAnnotation.isolation.value()
        isReadOnly = transactionalAnnotation.readOnly
        propagationBehavior = transactionalAnnotation.propagation.value()
        timeout = transactionalAnnotation.timeout
      }

    return template.execute { transaction ->
      val result =
        try {
          joinPoint.proceed()
          // boxResult(value) as Result<*, AppError>
        } catch (ex: Throwable) {
          Err(InternalAppError(transactionalAnnotation.exceptionDescription, ex))
        }

      if (isFailure(result)) {
        val e = failureAsErr(result).error as? AppError
        logger.debug("Transaction failed, rolling back. Error: ${e?.stackTraceToString()}")
        transaction.setRollbackOnly()
      }

      // result.onFailure {
      //   logger.debug("Transaction failed, rolling back. Error: ${it.stackTraceToString()}")
      //   transaction.setRollbackOnly()
      // }
      result
    }
  }
}
