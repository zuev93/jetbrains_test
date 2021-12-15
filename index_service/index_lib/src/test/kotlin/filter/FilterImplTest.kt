package filter

import java.nio.file.Path
import junit.framework.TestCase
import org.junit.Test

class FilterImplTest : TestCase() {

  private val testSubject = FilterImpl()

  @Test
  fun test_meta_returnsNotEmptyString() {
    val result = testSubject.meta()

    kotlin.test.assertFalse(result.isBlank())
  }

  @Test
  fun test_isAllowedPath_allowedPath_returnsTrue() {
    assertTrue(testSubject.isAllowedPath(Path.of("test.txt")))
  }

  @Test
  fun test_isAllowedPath_notAllowedPath_returnsFalse() {
    assertFalse(testSubject.isAllowedPath(Path.of("test.bin")))
  }
}