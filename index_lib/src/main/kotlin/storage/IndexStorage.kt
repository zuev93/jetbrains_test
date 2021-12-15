package storage

import MetaInfo
import java.nio.file.Path
import kotlinx.coroutines.flow.Flow

/**
 * Storage of the indexed data.
 */
interface IndexStorage : MetaInfo {

  /**
   * Searches for the given word in the storage.
   */
  fun search(word: String): Flow<Path>

  /**
   * Associates and adds given flow of the words from the path to the storage.
   */
  suspend fun add(words: Flow<String>, path: Path)

  /**
   * Removes the path and all associated words from the indexed storage.
   */
  fun remove(path: Path)
}