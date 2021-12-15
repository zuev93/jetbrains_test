package storage

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.nio.file.Files.createTempDirectory
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.math.absoluteValue
import kotlin.random.Random

class DumpableMap<T, U>(private val storageProvider: StorageProvider<T, U> = TempStorageProvider()) : SimplifiedMultiMap<T, U> {

    private var storagesCount = 1

    private val lock = ReentrantReadWriteLock()

    internal val storages: MutableMap<Int, HashMap<T, HashSet<U>>> = mutableMapOf()

    private val size: Int get() = storages.values.sumOf { it.size }

    override val state: String
        get() = """
      ${size} objects indexed in memory
      (Partitions loaded ${storages.size} / ${storagesCount}).
    """.trimIndent()

    override fun meta(): String = """
      Memory tolerant implementation.
      In case of excessive map size it will be split and not used 
      partitions might be dumped to the temp folder.
  """.trimIndent()

    override fun add(key: T, value: U) {
        lock.write {
            val storage = storage(key)
            storage.getOrPut(key) { HashSet() }.add(value)
            if (storage.size > bucketSize) {
                splitStorages()
            }
        }
    }

    override fun remove(key: T) {
        lock.write {
            storage(key).remove(key)
        }
    }

    override operator fun get(key: T): MutableSet<U>? {
        lock.read {
            return storage(key)[key]
        }
    }

    private fun storage(key: T): MutableMap<T, HashSet<U>> = storage(storageIndex(key))

    @Synchronized
    private fun storage(index: Int): MutableMap<T, HashSet<U>> {
        while (storages.size > maxStoragesInMemory) {
            val indexToRemove = storages.keys.toTypedArray()[Random.nextInt(storages.size)]
            if (indexToRemove == index) continue
            storageProvider.writeStorage(indexToRemove, storages[indexToRemove]!!)
            storages.remove(indexToRemove)
        }
        return storages.getOrPut(index) { storageProvider.loadStorage(index) }
    }

    @Synchronized
    private fun splitStorages() {
        val newStoragesCount = storagesCount * 2
        repeat(storagesCount) {
            // split #it values to #it and #(it + storagesCount)
            val storageAIndex = it
            val storageBIndex = it + storagesCount

            val storageA = storage(it)
            val storageB = storage(storageBIndex)

            val newStorageAValues = storageA.filter { storageIndex(it.key, newStoragesCount) == storageAIndex }
            val newStorageBValues = storageA.filter { storageIndex(it.key, newStoragesCount) == storageBIndex }
            storageA.clear()
            storageA.putAll(newStorageAValues)
            storageB.clear()
            storageB.putAll(newStorageBValues)
        }
        storagesCount = newStoragesCount
    }

    private fun storageIndex(key: T, storagesCount: Int = this.storagesCount) = key.hashCode().absoluteValue % storagesCount

    class TempStorageProvider<T, U> : StorageProvider<T, U> {
        internal val tempDirectory = createTempDirectory("storage-cache").toFile()


        override fun loadStorage(index: Int): HashMap<T, HashSet<U>> {
            val file = storageFile(index)
            if (!file.exists()) {
                return HashMap()
            }
            val result = FileInputStream(file).use { ObjectInputStream(it).use { it.readObject() } }
            return result as HashMap<T, HashSet<U>>? ?: HashMap()
        }

        override fun writeStorage(index: Int, storage: HashMap<T, HashSet<U>>) {
            val file = storageFile(index)
            FileOutputStream(file).use { ObjectOutputStream(it).use { it.writeObject(storage) } }
        }

        private fun storageFile(index: Int): File = File(tempDirectory, "cache-$index")
    }

    interface StorageProvider<T, U> {
        fun loadStorage(index: Int): HashMap<T, HashSet<U>>

        fun writeStorage(index: Int, storage: HashMap<T, HashSet<U>>)
    }

    companion object {
        private const val bucketSize = 10_000
        private const val maxStoragesInMemory = 1000
    }
}