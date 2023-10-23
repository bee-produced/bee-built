package com.beeproduced.result

import com.beeproduced.result.extensions.functional.mapWithPair
import com.beeproduced.result.extensions.functional.mapWithTriple
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.getOrThrow
import com.github.michaelbull.result.map
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-01-20
 */
class MapTest {

    private fun bar(str: String, str2: String) = str.count()
    private fun bar(str: String, str2: String, str3: String) = str.count() + str2.count() + str3.count()

    private class Foo {
        fun bar(str: String) = str.count()
        fun bar(str: String, str2: String) = str.count() + str2.count()
    }

    @Test
    fun `map to pair and call function`() {
        val str = "A"
        val str2 = "AB"
        val expectedCount = bar(str, str2)

        val actualCount = Ok(str)
            .map { Pair(it, str2) }
            .mapWithPair(::bar)
            .getOrThrow { IllegalArgumentException() }

        assertEquals(expectedCount, actualCount)
    }

    @Test
    fun `map to pair and call member function`() {
        val foo = Foo()
        val str = "A"
        val expectedCount = foo.bar(str)

        val actualCount = Ok(foo)
            .map { Pair(it, str) }
            .mapWithPair(Foo::bar)
            .getOrThrow { IllegalArgumentException() }

        assertEquals(expectedCount, actualCount)
    }

    @Test
    fun `map to triple and call function`() {
        val str = "A"
        val str2 = "AB"
        val str3 = "ABC"
        val expectedCount = bar(str, str2, str3)

        val actualCount = Ok(str)
            .map { Triple(it, str2, str3) }
            .mapWithTriple(::bar)
            .getOrThrow { IllegalArgumentException() }

        assertEquals(expectedCount, actualCount)
    }

    @Test
    fun `map to triple and call member function`() {
        val foo = Foo()
        val str = "A"
        val str2 = "AB"
        val expectedCount = foo.bar(str, str2)

        val actualCount = Ok(foo)
            .map { Triple(it, str, str2) }
            .mapWithTriple(Foo::bar)
            .getOrThrow { IllegalArgumentException() }

        assertEquals(expectedCount, actualCount)
    }
}