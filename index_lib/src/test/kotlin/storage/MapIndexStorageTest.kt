package storage

import java.nio.file.Path
import junit.framework.TestCase
import kotlin.io.path.pathString
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Test

class MapIndexStorageTest : TestCase() {

  private val wordToPath = MutableMapWrap<String, String>()
  private val pathToWord = MutableMapWrap<String, String>()

  private val testSubject = MapIndexStorage(wordToPath, pathToWord)

  @Test
  fun test_meta_returnsNotEmptyString() {
    val result = testSubject.meta

    kotlin.test.assertFalse(result.isBlank())
  }

  @Test
  fun test_state_returnsNotEmptyString() {

    val result = testSubject.state

    kotlin.test.assertFalse(result.isBlank())
  }

  @Test
  fun test_add_addsDataToUnderlyingMultiMaps() =
    runBlocking {
      testSubject.add(flowOf("word"), Path.of("path"))

      assertEquals(setOf("path"), wordToPath["word"])
      assertEquals(setOf("word"), pathToWord["path"])
    }

  @Test
  fun test_search_usesDataFromUnderlyingMultiMaps() =
    runBlocking {
      wordToPath.add("word", "path")

      val search = testSubject.search("word").toList()

      assertEquals("path", search.firstOrNull()?.pathString)
    }

  @Test
  fun test_remove_removesDataFromUnderlyingMultiMaps() {
    wordToPath.add("word1", "path1")
    wordToPath.add("word2", "path2")
    wordToPath.add("word3", "path1")
    pathToWord.add("path1", "word1")
    pathToWord.add("path1", "word3")
    pathToWord.add("path2", "word2")

    testSubject.remove(Path.of("path1"))

    assertEquals(setOf("path2"), wordToPath["word2"])
    assertEquals(setOf("word2"), pathToWord["path2"])
  }
}