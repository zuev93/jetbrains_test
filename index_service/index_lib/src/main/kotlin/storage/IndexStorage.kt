package storage

import MetaInfo
import java.nio.file.Path
import kotlinx.coroutines.flow.Flow

interface IndexStorage : MetaInfo {

  fun search(word: String): Flow<Path>
  suspend fun add(words: Flow<String>, path: Path)
  fun remove(path: Path)

  fun state(): String
}