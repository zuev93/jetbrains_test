package tokenizer

import java.io.BufferedReader
import java.io.StringReader
import junit.framework.TestCase
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Test


class DelimiterTokenizerTest : TestCase() {

    private val testSubject = DelimiterTokenizer()

    @Test
    fun test_meta_returnsNotEmptyString() {
        val result = testSubject.meta()

        kotlin.test.assertFalse(result.isBlank())
    }

    @Test
    fun test_tokenize_simpleString_tokenizesAsExpected() = runBlocking {
        val test = "\tfoo, \nbar.foo-bar    "
        val inputString = StringReader(test)
        val reader = BufferedReader(inputString)

        val tokens = testSubject.tokenize(reader).toList()

        assertEquals(listOf("foo", "bar", "foo-bar"), tokens)
    }
}