import kotlinx.coroutines.*
import java.util.concurrent.Executors
import kotlin.io.path.Path
import kotlin.system.exitProcess
import storage.IndexFactory
import tokenizer.DelimiterTokenizer

@OptIn(InternalCoroutinesApi::class)
object CliActions {
  private val dispatcher = Executors.newFixedThreadPool(5).asCoroutineDispatcher()

  fun printInfo(actions: Array<Pair<Array<String>, (String) -> Unit>>) {
    println("Please use one of following command followed by an argument (if needed): ")
    for (action in actions) {
      println(action.first.joinToString())
    }
  }

  fun createCliActions(defaultIndexer: FileIndexer): Array<Pair<Array<String>, (String) -> Unit>> {
    var indexer: FileIndexer =
      defaultIndexer
    lateinit var actions: Array<Pair<Array<String>, (String) -> Unit>>
    actions = arrayOf(
      Pair(arrayOf("exit", "stop", "q")) { exitProcess(0) },
      Pair(arrayOf("add", "a")) { arg ->
        GlobalScope.launch(dispatcher) {
          indexer.addToIndex(Path(arg))
          println("Request adding $arg to index is completed.")
        }
        println("Request to add to index is initiated. Meanwhile you can ran other commands.")
      },
      Pair(arrayOf("remove", "delete", "rm")) { arg ->
        GlobalScope.launch(dispatcher) {
          indexer.removeFromIndex(Path(arg))
          println("Request removing $arg from index is completed.")
        }
        println("Request to remove from index is initiated. Meanwhile you can ran other commands.")
      },
      Pair(arrayOf("search", "s")) { arg ->
        runBlocking {
          println("Search results (listed online):")
          indexer.search(arg).collect { println(it) }
        }
      },
      Pair(arrayOf("state", "st")) { println(indexer.state()) },
      Pair(arrayOf("meta", "mt")) { println(indexer.meta()) },
      Pair(arrayOf("list",
                   "watched")) { println(indexer.watchedDirectories.joinToString(separator = "\n")) },
      Pair(arrayOf("storage")) { arg ->
        run {
          val storage = when (arg) {
            "simple" -> IndexFactory.simpleMapStorage()
            "dump" -> IndexFactory.dumpableMapStorage()
            else -> {
              println("Current storage is '${indexer.index::class}'")
              return@run
            }
          }
          GlobalScope.launch(dispatcher) {
            indexer = indexer.withStorage(storage)
            println("Indexer has been rebuilt with a ${storage.meta()}")
          }
          println("Index rebuilt has been started. " +
                    "Meanwhile you can continue working with a current one")
        }
      },
      Pair(arrayOf("tokenizer")) { arg ->
        run {
          val tokenizer = when (arg) {
            "word" -> DelimiterTokenizer()
            else -> {
              println("Current tokenizer is '${indexer.tokenizer::class}'")
              return@run
            }
          }
          if (tokenizer::class == indexer.tokenizer::class) {
            println("Indexer is already based on '$arg' tokenizer")
            return@run
          }
          GlobalScope.launch(dispatcher) {
            indexer = indexer.withTokenizer(tokenizer)
            println("Indexer has been rebuilt with a ${indexer.meta()}")
          }
          println("Index rebuilt has been started. " +
                    "Meanwhile you can continue working with a current one")
        }
      },
      Pair(arrayOf("help", "?")) { printInfo(actions) },
    )
    return actions
  }
}