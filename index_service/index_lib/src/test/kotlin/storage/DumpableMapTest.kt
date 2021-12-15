package storage

import junit.framework.TestCase
import org.junit.Test
import org.mockito.Mockito

class DumpableMapTest : TestCase() {
    val storageProvider: DumpableMap.StorageProvider<String, Int> = Mockito.mock(DumpableMap.StorageProvider::class.java) as DumpableMap.StorageProvider<String, Int>
    private val testSubject = DumpableMap(storageProvider = storageProvider)

    @Test
    fun test_add_addsToOneOfStorages() {
        testSubject.add("test", 10)

        assertTrue(testSubject.storages.containsKey(0))
        assertEquals(mutableMapOf(Pair("test", mutableSetOf(10))), testSubject.storages[0])
    }

    @Test
    fun test_remove_removeFromUnderlyingStorage() {
        testSubject.storages[0] = HashMap()
        testSubject.storages[0]!!["test"] = hashSetOf(10)

        testSubject.remove("test")

        assertTrue(testSubject.storages[0]?.containsKey("test") == false)
    }

    @Test
    fun test_get_returnsFromUnderlyingStorage() {
        testSubject.storages[0] = HashMap()
        testSubject.storages[0]!!["test"] = hashSetOf(10)

        val result = testSubject.get("test")

        assertEquals(mutableSetOf(10), result)
    }

    // TODO tests for split

}