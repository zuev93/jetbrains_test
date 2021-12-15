package storage

object IndexFactory {
  fun simpleMapStorage(): MapIndexStorage = MapIndexStorage(MutableMapWrap(), MutableMapWrap())

  fun dumpableMapStorage(): MapIndexStorage = MapIndexStorage(DumpableMap(), DumpableMap())
}