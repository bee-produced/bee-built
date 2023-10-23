package com.beeproduced.result.errors

/**
 * Inspired by
 * - https://www.sitepoint.com/deep-dive-into-java-9s-stack-walking-api/
 * - https://docs.oracle.com/javase/9/docs/api/java/lang/StackWalker.html
 *
 * @author Kacper Urbaniec
 * @version 2023-01-10
 */


sealed interface ResultError {
    fun description(): String
    fun stackTraceToString(): String
}

open class ExceptionError(
    protected val exception: Throwable
) : ResultError {
    override fun description(): String = exception.toString()
    override fun stackTraceToString(): String = exception.stackTraceToString()
}

sealed class AppError(
    protected val description: String,
    protected val source: ResultError? = null,
    skipStackTraceElements: Long = 0,
    limitStackTraceElements: Long = 1
) : ResultError {

    @Suppress("MemberVisibilityCanBePrivate")
    protected fun takeStackTraceElements(skip: Long, limit: Long): Array<StackTraceElement> {
        return StackWalker
            .getInstance()
            .walk { frames ->
                // Drop traces from this method & error construction
                // Get elements determined by skip & limit after construction
                return@walk frames.skip(1).dropWhile {
                    it.methodName.indexOf('<') != -1 // Drop <init>
                }.skip(skip).limit(limit).map {
                    it.toStackTraceElement()
                }.toArray { size ->
                    // See: https://discuss.kotlinlang.org/t/how-does-one-translate-foo-new-from-java-to-kotlin/133
                    arrayOfNulls<StackTraceElement>(size)
                }
            }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    protected val stackTraceElements: Array<StackTraceElement> =
        takeStackTraceElements(skipStackTraceElements, limitStackTraceElements)

    constructor(
        description: String,
        source: Throwable,
        skipStackTraceElements: Long = 0,
        limitStackTraceElements: Long = 1
    ) : this(description, ExceptionError(source), skipStackTraceElements, limitStackTraceElements)

    override fun description(): String = description
    private fun fullDescription() = "${this.javaClass.name}: $description"
    override fun stackTraceToString(): String {
        return buildString {
            // Print in red: https://discuss.kotlinlang.org/t/printing-in-colors/22492
            append("\u001b[31m")
            appendLine(this@AppError.fullDescription())
            for (stackTraceElement in stackTraceElements)
                appendLine("\tat $stackTraceElement")
            if (this@AppError.source != null)
                append("Caused by: ${this@AppError.source.stackTraceToString()}")
            append("\u001b[0m")
        }
    }
}

open class BadRequestError : AppError {
    constructor(
        description: String,
        source: ResultError? = null,
        skipStackTraceElements: Long = 0,
        limitStackTraceElements: Long = 1
    ) : super(description, source, skipStackTraceElements, limitStackTraceElements)

    constructor(
        description: String,
        source: Throwable,
        skipStackTraceElements: Long = 0,
        limitStackTraceElements: Long = 1
    ) : super(description, source, skipStackTraceElements, limitStackTraceElements)
}

open class InternalAppError : AppError {
    constructor(
        description: String,
        source: ResultError? = null,
        skipStackTraceElements: Long = 0,
        limitStackTraceElements: Long = 1
    ) : super(description, source, skipStackTraceElements, limitStackTraceElements)

    constructor(
        description: String,
        source: Throwable,
        skipStackTraceElements: Long = 0,
        limitStackTraceElements: Long = 1
    ) : super(description, source, skipStackTraceElements, limitStackTraceElements)
}
