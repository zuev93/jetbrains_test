package storage

import junit.framework.TestCase
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.io.ObjectOutputStream

class TempStorageProviderTest : TestCase() {
    private val testSubject = DumpableMap.TempStorageProvider<String, String>()

    @Test
    fun test_writeStorage_writesToTempFile() {
        val map = HashMap<String, HashSet<String>>()
        map["foo"] = HashSet()
        map["foo"]!!.add("bar")

        testSubject.writeStorage(10, map)

        assertTrue(File(testSubject.tempDirectory, "cache-10").exists())
    }

    @Test
    fun test_loadStorage_readsFromTempFile() {
        val map = HashMap<String, HashSet<String>>()
        map["foo"] = HashSet()
        map["foo"]!!.add("bar")
        FileOutputStream(File(testSubject.tempDirectory, "cache-10")).use {
            ObjectOutputStream(it).use { it.writeObject(map) }
        }

        val result = testSubject.loadStorage(10)

        assertEquals(map, result)
    }
}