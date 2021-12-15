package tokenizer

import java.io.BufferedReader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class DelimiterTokenizer(
  private val delimiters: Array<String> = arrayOf(" ", ",", ".", "?", "!"),
) : Tokenizer {

  override val meta: String
    get() = """
      DelimiterTokenizer - the most simple word tokenizer.
      Split input by delimiters, removing blank entries and trim whitespaces.
      Delimiters: [${delimiters.joinToString { a -> "'$a'" }}]
    """.trimIndent()

  override val state: String
    get() = "There is no state in the filter"

  /**
   * P.S. Thanks to ktor quality.
   * [ByteReadChannel.readUTF8Line] randomly fails with Caused by:
   * io.ktor.utils.io.charsets.TooLongLineException: Line is longer than limit
   * after several success read calls of exact the same string.
   */
  override fun tokenize(bufferedReader: BufferedReader): Flow<String> =
    flow {
      val buffer = CharArray(BUFFER_SIZE)
      var currentString = ""
      while (true) {
        val readCount = bufferedReader.read(buffer, 0, BUFFER_SIZE)
        if (readCount == -1) break
        currentString += buffer.concatToString().substring(0, readCount)
        val split = currentString.split(*delimiters)
        if (split.size > 1) {
          currentString = split.last()
          split.dropLast(1)
            .filter { it.isNotBlank() }
            .forEach { emit(it.trim()) }
        }
      }
      if (currentString.isNotBlank()) emit(currentString)
    }

  companion object {
    private const val BUFFER_SIZE = 1024
  }
}