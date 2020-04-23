package com.asizon.sdk

import com.asizon.extractdate.AutoExtractTime
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun extractDate() {
        val result = AutoExtractTime().start("11:35")
        if (result.isSuccessful) {
            println(result.format)
        }
    }
}
