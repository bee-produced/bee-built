package com.beeproduced.result.jpa.transactional

import com.beeproduced.result.errors.AppError
import com.beeproduced.result.errors.InternalAppError
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.onFailure
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
class TransactionalResultAspect(
    private val context: ApplicationContext
) {
    private val logger = LoggerFactory.getLogger(TransactionalResultAspect::class.java)
    @Pointcut("execution(@com.beeproduced.result.jpa.transactional.TransactionalResult com.github.michaelbull.result.Result *(..))")
    fun transactionalMethodReturningResult() = Unit

    @Suppress("UNCHECKED_CAST")
    @Around("transactionalMethodReturningResult() && @annotation(transactionalAnnotation)")
    fun runInTransactionAndRollbackOnErr(
        joinPoint: ProceedingJoinPoint,
        transactionalAnnotation: TransactionalResult
    ): Any? {
        val transactionManager: PlatformTransactionManager = if (transactionalAnnotation.value == "") {
            context.getBean(PlatformTransactionManager::class.java)
        } else {
            context.getBean(transactionalAnnotation.value, PlatformTransactionManager::class.java)
        }

        val template = TransactionTemplate(transactionManager).apply {
            isolationLevel = transactionalAnnotation.isolation.value()
            isReadOnly = transactionalAnnotation.readOnly
            propagationBehavior = transactionalAnnotation.propagation.value()
            timeout = transactionalAnnotation.timeout
        }

        return template.execute { transaction ->
            val result = try {
                joinPoint.proceed() as Result<*, AppError>
            } catch (ex: Throwable) {
                Err(InternalAppError(transactionalAnnotation.exceptionDescription, ex))
            }

            result.onFailure {
                logger.debug("Transaction failed, rolling back. Error: ${it.stackTraceToString()}")
                transaction.setRollbackOnly()
            }
            result
        }
    }

}
