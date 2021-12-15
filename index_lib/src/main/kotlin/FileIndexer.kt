import filter.Filter
import java.nio.file.Path
import kotlinx.coroutines.flow.Flow
import storage.IndexStorage
import tokenizer.Tokenizer

/**
 * Indexer for files and directories.
 */
interface FileIndexer : MetaInfo, IndexerConfiguration {
  val watchedDirectories: Set<Path>

  /**
   * Adds given path to the index and watches for its changes.
   * path can be either directory or file.
   */
  suspend fun addToIndex(path: Path)

  /**
   * Removes given path from the index and watched paths.
   * path can be either directory or file.
   */
  suspend fun removeFromIndex(path: Path)

  /**
   * Searches for the given word in index. Returns a flow of the found paths.
   */
  fun search(word: String): Flow<Path>

  /**
   * Runs watch service for monitoring changes.
   */
  fun run()

  /**
   * Stops watch service.
   */
  fun stop()
}

/**
 * Configurational interface for the file indexer.
 *
 * Allows changing underlying services (storage, tokenizer and filter).
 */
interface IndexerConfiguration {
  val index: IndexStorage
  val tokenizer: Tokenizer

  suspend fun withStorage(index: IndexStorage): FileIndexer

  suspend fun withTokenizer(tokenizer: Tokenizer): FileIndexer

  suspend fun withFilter(filter: Filter): FileIndexer

}