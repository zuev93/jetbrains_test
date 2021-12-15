import CliActions.createCliActions
import CliActions.printInfo
import filter.FilterImpl
import kotlin.concurrent.thread
import storage.IndexFactory
import tokenizer.DelimiterTokenizer

fun main() {
  val indexer =
    FileIndexerImpl(tokenizer = DelimiterTokenizer(),
                    index = IndexFactory.dumpableMapStorage(),
                    filter = FilterImpl())
  val actions = createCliActions(indexer)
  thread { indexer.run() }

  printInfo(actions)

  while (true) {
    val input = readLine() ?: continue
    val args = input.split(" ")
    val command = args[0].lowercase()
    actions.firstOrNull { command in it.first }?.second?.let {
      try {
        it.invoke(args.getOrNull(1) ?: "")
      } catch (e: Exception) {
        println(e)
      }
    } ?: println("Unknown command. Use ? for the help.")
  }
}