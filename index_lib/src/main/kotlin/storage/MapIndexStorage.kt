package storage

import java.nio.file.Path
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.io.path.pathString
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@OptIn(InternalCoroutinesApi::class)
class MapIndexStorage(
  private val wordToPath: SimplifiedMultiMap<String, String>,
  private val pathToWord: SimplifiedMultiMap<String, String>,
) : IndexStorage {

  private val lock = ReentrantReadWriteLock()

  override fun state(): String {
    return """
      Words: ${wordToPath.state}
      Paths: ${pathToWord.state}
    """.trimIndent()
  }

  override fun meta(): String {
    return """
      MapStorage - storage based on map like structure.
      Performance and memory consumption depends on underlying implementation
      Inner storage: ${wordToPath.meta()}
    """.trimIndent()
  }

  override fun search(word: String): Flow<Path> {
    return lock.read {
      flowOf(*(wordToPath[word]?.map { Path.of(it) }?.toTypedArray() ?: arrayOf()))
    }
  }

  override suspend fun add(words: Flow<String>, path: Path) {
    lock.write {
      val pathString = path.pathString
      words.collect {
        wordToPath.add(it, pathString)
        pathToWord.add(pathString, it)
      }
    }
  }

  override fun remove(path: Path) {
    lock.write {
      val pathString = path.pathString
      pathToWord[pathString]?.forEach {
        wordToPath[it]?.remove(pathString)
      }
      pathToWord.remove(pathString)
    }
  }
}