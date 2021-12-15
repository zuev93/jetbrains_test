package storage

/**
 * Factory for getting specific storage implementations.
 */
object IndexFactory {
  /**
   * Storage based on HashMap.
   */
  fun simpleMapStorage(): MapIndexStorage =
    MapIndexStorage(MutableMapWrap(), MutableMapWrap())

  /**
   * Storage based on dumpable map. Memory tolerant.
   */
  fun dumpableMapStorage(): MapIndexStorage =
    MapIndexStorage(DumpableMap(), DumpableMap())
}