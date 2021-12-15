package filter

import java.nio.file.Path
import kotlin.io.path.extension

class FilterImpl(
  private val allowedFileExtensions: Array<String> = arrayOf("kt",
                                                             "txt",
                                                             "java"),
) : Filter {

  override val meta: String
    get() = """
      Filter for determining whether file indexable or not. 
      This implementation is a whitelist based on file extension. 
      However this can be generalised even more for more enhanced logic.
      Whitelist: [${allowedFileExtensions.joinToString { a -> "'$a'" }}]
    """.trimIndent()

  override val state: String
    get() = "There is no state in the filter"

  override fun isAllowedPath(path: Path): Boolean {
    return path.extension in allowedFileExtensions
  }
}