import filter.Filter
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
import java.nio.file.WatchKey
import java.util.WeakHashMap
import java.util.concurrent.Executors
import kotlin.io.path.bufferedReader
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import storage.IndexStorage
import tokenizer.Tokenizer

@OptIn(DelicateCoroutinesApi::class)
class FileIndexerImpl constructor(
        override val tokenizer: Tokenizer,
        override val index: IndexStorage,
        private val filter: Filter,
) : FileIndexer {

    private val watchService = FileSystems.getDefault().newWatchService()
    private val dispatcher = Executors.newFixedThreadPool(5).asCoroutineDispatcher()

    private val watchKeys: MutableMap<Path, WatchKey> = mutableMapOf()
    override val watchedDirectories: Set<Path> get() = watchKeys.keys.toSet()
    internal val watchedFiles: MutableSet<Path> = mutableSetOf()

    private val indexingPaths: MutableMap<Path, Deferred<Unit>> = WeakHashMap()

    override fun meta(): String {
        return """
      File indexer is based on: 
      Storage: 
      ${index.meta()}
      Tokenizer: 
      ${tokenizer.meta()}
      Filter: 
      ${filter.meta()}
    """.trimIndent()
    }


    override fun state(): String {
        return """
      Watched directories count: ${watchKeys.size} 
      Storage state:
      ${index.state()}
    """.trimIndent()
    }

    override suspend fun withStorage(index: IndexStorage): FileIndexer {
        val newIndexer = FileIndexerImpl(tokenizer, index, filter)
        copyToFileIndexer(newIndexer)
        return newIndexer
    }

    override suspend fun withTokenizer(tokenizer: Tokenizer): FileIndexer {
        val newIndexer = FileIndexerImpl(tokenizer, index, filter)
        copyToFileIndexer(newIndexer)
        return newIndexer
    }

    override suspend fun withFilter(filter: Filter): FileIndexer {
        val newIndexer = FileIndexerImpl(tokenizer, index, filter)
        copyToFileIndexer(newIndexer)
        return newIndexer
    }

    override suspend fun addToIndex(path: Path): Unit = coroutineScope {
        if (!path.exists()) throw Exception("$path does not exists.")
        val deferred = path.toFile().walkTopDown().map {
            addPathToIndexAsync(it.toPath())
        }.toMutableList()

        if (path.isRegularFile()) {
            // watch parent directory changes to enable watch service.
            deferred.add(addPathToIndexAsync(path.parent))
        }

        awaitAll(*deferred.toTypedArray())
    }

    override suspend fun removeFromIndex(path: Path) {
        if (path.isRegularFile()) return removeFromIndex(path.parent)
        synchronized(this) {
            watchKeys.remove(path)?.cancel()
            watchedFiles.filter { it.parent.equals(path) }.forEach {
                watchedFiles.remove(it)
                index.remove(it)
            }
        }
    }

    override fun search(word: String): Flow<Path> {
        return index.search(word)
    }

    fun run() {
        while (true) {
            val watchKey = watchService.poll() ?: continue

            for (event in watchKey.pollEvents()) {
                val path = event.context() as Path
                if (path.isRegularFile() && path !in watchedFiles) continue
                when (event.kind()) {
                    ENTRY_CREATE -> GlobalScope.launch(dispatcher) {
                        // New entry causes to recursive indexing
                        addToIndex(path)
                    }
                    ENTRY_MODIFY -> {
                        GlobalScope.launch(dispatcher) {
                            addPathToIndexAsync(path).await()
                        }
                    }
                    ENTRY_DELETE -> index.remove(path)
                }
            }

            if (!watchKey.reset()) {
                watchKey.cancel()
                break
            }
        }
    }

    @Synchronized
    private fun addPathToIndexAsync(path: Path): Deferred<Unit> {
        if (path.isDirectory()) {
            if (watchKeys.containsKey(path)) return CompletableDeferred(value = Unit)
            val pathKey = path.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE)
            watchKeys[path] = pathKey
        }
        if (path.isRegularFile()) {
            return indexFileAsync(path)
        }
        return CompletableDeferred(value = Unit)
    }

    @Synchronized
    private fun indexFileAsync(path: Path): Deferred<Unit> {
        if (indexingPaths[path]?.isCompleted == true) {
            indexingPaths.remove(path)
        }
        return indexingPaths.getOrPut(path) {
            GlobalScope.async(dispatcher) {
                try {
                    if (!filter.isAllowedPath(path)) return@async
                    watchedFiles.add(path)
                    index.remove(path)
                    path.bufferedReader().use {
                        index.add(tokenizer.tokenize(it), path)
                    }
                } catch (e: Exception) {
                    throw Exception("Error during processing $path", e)
                }
            }
        }
    }

    private suspend fun copyToFileIndexer(newIndexer: FileIndexerImpl) {
        val deferred = watchedFiles.map {
            newIndexer.addPathToIndexAsync(it)
        }.toList()
        awaitAll(*deferred.toTypedArray())
    }
}