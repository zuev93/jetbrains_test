import filter.Filter
import filter.FilterImpl
import java.nio.file.Path
import storage.IndexStorage
import tokenizer.Tokenizer
import kotlinx.coroutines.flow.Flow

interface FileIndexer : MetaInfo, IndexerConfiguration {
  val watchedDirectories: Set<Path>

  suspend fun addToIndex(path: Path)

  suspend fun removeFromIndex(path: Path)

  fun search(word: String): Flow<Path>

  fun state(): String
}

interface IndexerConfiguration {
  val index: IndexStorage
  val tokenizer: Tokenizer

  suspend fun withStorage(index: IndexStorage): FileIndexer

  suspend fun withTokenizer(tokenizer: Tokenizer): FileIndexer

  suspend fun withFilter(filter: Filter): FileIndexer

}