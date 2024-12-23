package com.beeproduced.bee.functional

import com.beeproduced.bee.functional.extensions.com.github.michaelbull.result.mapToBadRequestError
import com.beeproduced.bee.functional.extensions.com.github.michaelbull.result.mapWhenInternalError
import com.beeproduced.bee.functional.result.errors.BadRequestError
import com.beeproduced.bee.functional.result.errors.ExceptionError
import com.beeproduced.bee.functional.result.errors.InternalAppError
import com.beeproduced.bee.functional.result.errors.ResultError
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.getErrorOrElse
import com.github.michaelbull.result.mapError
import java.io.OutputStream
import java.io.PrintStream
import kotlin.test.Ignore
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

/**
 * @author Kacper Urbaniec
 * @version 2023-01-10
 */
class ErrorTest {

  private fun lineCount(text: String): Int {
    return text.replace("\u001B[0m", "").trimEnd().lineSequence().count()
  }

  private fun lineNumberBefore(): Int {
    return StackWalker.getInstance().walk { frames ->
      frames.skip(1).findFirst().get().lineNumber - 1
    }
  }

  @Test
  fun `exception wrapper`() {
    val exDescription = "-< Exception |-"
    val exType = IllegalArgumentException::class.java.simpleName
    val ex = IllegalArgumentException(exDescription)
    val exLineNumber = lineNumberBefore()
    val exLineCount = lineCount(ex.stackTraceToString())

    val error = ExceptionError(ex)
    val errorStackTrace = error.stackTraceToString()
    val errorLineCount = lineCount(errorStackTrace)

    assertEquals(ex.toString(), error.description())
    assertEquals(exLineCount, errorLineCount)
    assertTrue(errorStackTrace.contains(exDescription))
    assertTrue(errorStackTrace.contains(exType))
    assertTrue(errorStackTrace.contains("$exLineNumber"))
  }

  @Test
  fun `bad request error`() {
    val errorDescription = "-< Error |-"
    val errorType = BadRequestError::class.java.simpleName
    val error = BadRequestError(errorDescription)
    val errorLineNumber = lineNumberBefore()
    val errorStackTrace = error.stackTraceToString()
    val errorLineCount = lineCount(errorStackTrace)

    assertEquals(errorDescription, error.description())
    // Description + StackFrame = 2
    assertEquals(2, errorLineCount)
    assertTrue(errorStackTrace.contains(errorDescription))
    assertTrue(errorStackTrace.contains(errorType))
    assertTrue(errorStackTrace.contains("$errorLineNumber"))
  }

  @Test
  fun `internal error`() {
    val errorDescription = "-< Error |-"
    val errorType = InternalAppError::class.java.simpleName
    val error = InternalAppError(errorDescription)
    val errorLineNumber = lineNumberBefore()
    val errorStackTrace = error.stackTraceToString()
    val errorLineCount = lineCount(errorStackTrace)

    assertEquals(errorDescription, error.description())
    // Description + StackFrame = 2
    assertEquals(2, errorLineCount)
    assertTrue(errorStackTrace.contains(errorDescription))
    assertTrue(errorStackTrace.contains(errorType))
    assertTrue(errorStackTrace.contains("$errorLineNumber"))
  }

  @Test
  fun `bad request error with underlying exception error`() {
    val exDescription = "-< Exception |-"
    val exType = IllegalArgumentException::class.java.simpleName
    val ex = IllegalArgumentException(exDescription)
    val exLineNumber = lineNumberBefore()

    val error1 = ExceptionError(ex)
    val error1StackTrace = error1.stackTraceToString()
    val error1LineCount = lineCount(error1StackTrace)

    val error2Description = "-< Error |-"
    val error2Type = BadRequestError::class.java.simpleName
    val error2 = BadRequestError(error2Description, error1)
    val error2LineNumber = lineNumberBefore()
    val error2StackTrace = error2.stackTraceToString()
    val error2LineCount = lineCount(error2StackTrace)

    assertTrue(error2StackTrace.contains(exDescription))
    assertTrue(error2StackTrace.contains(exType))
    assertTrue(error2StackTrace.contains("$exLineNumber"))
    assertTrue(error2StackTrace.contains(error2Description))
    assertTrue(error2StackTrace.contains(error2Type))
    assertTrue(error2StackTrace.contains("$error2LineNumber"))
    assertTrue(error2StackTrace.contains(error1StackTrace))
    assertEquals(error1LineCount + 2, error2LineCount)
  }

  @Test
  fun `bad request error with underlying internal error`() {
    val error1Description = "-< Error 1 |-"
    val error1Type = InternalAppError::class.java.simpleName
    val error1 = InternalAppError(error1Description)
    val error1LineNumber = lineNumberBefore()
    val error1StackTrace = error1.stackTraceToString()

    val error2Description = "-< Error 2 |-"
    val error2Type = BadRequestError::class.java.simpleName
    val error2 = BadRequestError(error2Description, error1)
    val error2LineNumber = lineNumberBefore()
    val error2StackTrace = error2.stackTraceToString()
    val error2LineCount = lineCount(error2StackTrace)

    assertTrue(error2StackTrace.contains(error1Description))
    assertTrue(error2StackTrace.contains(error1Type))
    assertTrue(error2StackTrace.contains("$error1LineNumber"))
    assertTrue(error2StackTrace.contains(error2Description))
    assertTrue(error2StackTrace.contains(error2Type))
    assertTrue(error2StackTrace.contains("$error2LineNumber"))
    assertTrue(error2StackTrace.contains(error1StackTrace))
    assertEquals(4, error2LineCount)
  }

  @Test
  fun `map bad request to bad request via lambda`() {
    val error1Description = "-< First Error |-"
    val error2Description = "-< Second Error |-"

    val error1 = BadRequestError(error1Description)
    val error1LineNumber = lineNumberBefore()
    val error1StackTrace = error1.stackTraceToString()
    val error2 =
      Err(error1)
        .mapError { e -> BadRequestError(error2Description, e) }
        .getErrorOrElse { throw IllegalArgumentException() }
    val error2LineNumber = lineNumberBefore() - 1
    val error2StackTrace = error2.stackTraceToString()
    val error2LineCount = lineCount(error2StackTrace)

    assertTrue(error2StackTrace.contains(error1Description))
    assertTrue(error2StackTrace.contains("$error1LineNumber"))
    assertTrue(error2StackTrace.contains(error2Description))
    assertTrue(error2StackTrace.contains("$error2LineNumber"))
    assertTrue(error2StackTrace.contains(error1StackTrace))
    assertEquals(4, error2LineCount)
  }

  @Test
  fun `map bad request to bad request via description`() {
    val error1Description = "-< First Error |-"
    val error2Description = "-< Second Error |-"

    val error1 = BadRequestError(error1Description)
    val error1LineNumber = lineNumberBefore()
    val error1StackTrace = error1.stackTraceToString()
    val error2 =
      Err(error1).mapToBadRequestError(error2Description).getErrorOrElse {
        throw IllegalArgumentException()
      }
    val error2LineNumber = lineNumberBefore() - 2
    val error2StackTrace = error2.stackTraceToString()
    val error2LineCount = lineCount(error2StackTrace)

    assertTrue(error2StackTrace.contains(error1Description))
    assertTrue(error2StackTrace.contains("$error1LineNumber"))
    assertTrue(error2StackTrace.contains(error2Description))
    assertTrue(error2StackTrace.contains("$error2LineNumber"))
    assertTrue(error2StackTrace.contains(error1StackTrace))
    assertEquals(4, error2LineCount)
  }

  @Test
  fun `omit bad request mapping because internal error mapping via lambda`() {
    val error1Description = "-< First Error |-"
    val error2Description = "-< Second Error |-"

    val error1 = BadRequestError(error1Description)
    val error1LineNumber = lineNumberBefore()
    val error1StackTrace = error1.stackTraceToString()
    val error2 =
      Err(error1)
        .mapWhenInternalError { e -> InternalAppError(error2Description, e) }
        .getErrorOrElse { throw IllegalArgumentException() }
    val error2LineNumber = lineNumberBefore() - 1
    val error2StackTrace = error2.stackTraceToString()
    val error2LineCount = lineCount(error2StackTrace)

    assertTrue(error2StackTrace.contains(error1Description))
    assertTrue(error2StackTrace.contains("$error1LineNumber"))
    assertFalse(error2StackTrace.contains(error2Description))
    assertFalse(error2StackTrace.contains("$error2LineNumber"))
    assertEquals(error2StackTrace, error1StackTrace)
    assertEquals(2, error2LineCount)
  }

  @Test
  fun `omit bad request mapping because internal error mapping via description`() {
    val error1Description = "-< First Error |-"
    val error2Description = "-< Second Error |-"

    val error1 = BadRequestError(error1Description)
    val error1LineNumber = lineNumberBefore()
    val error1StackTrace = error1.stackTraceToString()
    val error2 =
      Err(error1).mapWhenInternalError(error2Description).getErrorOrElse {
        throw IllegalArgumentException()
      }
    val error2LineNumber = lineNumberBefore() - 1
    val error2StackTrace = error2.stackTraceToString()
    val error2LineCount = lineCount(error2StackTrace)

    assertTrue(error2StackTrace.contains(error1Description))
    assertTrue(error2StackTrace.contains("$error1LineNumber"))
    assertFalse(error2StackTrace.contains(error2Description))
    assertFalse(error2StackTrace.contains("$error2LineNumber"))
    assertEquals(error2StackTrace, error1StackTrace)
    assertEquals(2, error2LineCount)
  }

  class CustomBadRequest(d: String, e: ResultError? = null) : BadRequestError(d, e)

  @Test
  fun `map bad request to custom bad request via lambda`() {
    val error1Description = "-< First Error |-"
    val error2Description = "-< Second Error |-"
    val error2Type = CustomBadRequest::class.java.simpleName

    val error1 = BadRequestError(error1Description)
    val error1LineNumber = lineNumberBefore()
    val error1StackTrace = error1.stackTraceToString()
    val error2 =
      Err(error1)
        .mapError { e -> CustomBadRequest(error2Description, e) }
        .getErrorOrElse { throw IllegalArgumentException() }
    val error2LineNumber = lineNumberBefore() - 1
    val error2StackTrace = error2.stackTraceToString()
    val error2LineCount = lineCount(error2StackTrace)

    assertTrue(error2StackTrace.contains(error1Description))
    assertTrue(error2StackTrace.contains("$error1LineNumber"))
    assertTrue(error2StackTrace.contains(error2Description))
    assertTrue(error2StackTrace.contains("$error2LineNumber"))
    assertTrue(error2StackTrace.contains(error2Type))
    assertTrue(error2StackTrace.contains(error1StackTrace))
    assertEquals(4, error2LineCount)
  }

  @Test
  @Ignore
  fun `app error benchmark`() {
    benchmark {
      val first = BadRequestError("First error", IllegalArgumentException("test"))
      val second = BadRequestError("Second error", first)

      println(second.description())
      println(second.stackTraceToString())
    }
  }

  @Test
  @Ignore
  fun `exception benchmark`() {
    benchmark {
      lateinit var second: Exception
      try {
        try {
          throw IllegalArgumentException("test")
        } catch (zero: Exception) {
          val first = IllegalArgumentException("First error")
          first.initCause(zero)
          throw first
        }
      } catch (first: Exception) {
        second = IllegalArgumentException("Second error")
        second.initCause(first)
      }

      println(second.message)
      println(second.stackTraceToString())
    }
  }

  private fun benchmark(loopCount: Long = 100_000, fn: () -> Unit) {
    val originalStream = System.out
    val dummyStream =
      PrintStream(
        object : OutputStream() {
          override fun write(b: Int) {}
        }
      )
    System.setOut(dummyStream)
    val start = System.currentTimeMillis()
    for (i in 0 until loopCount) fn()
    val end = System.currentTimeMillis() - start
    val average = end / loopCount.toDouble()
    System.setOut(originalStream)
    println("Benchmark: Time: $end ms | Avg: $average ms")
  }
}
