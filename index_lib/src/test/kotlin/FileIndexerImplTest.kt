import filter.Filter
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import storage.IndexStorage
import tokenizer.Tokenizer
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

@RunWith(MockitoJUnitRunner::class)
class FileIndexerImplTest {
    @Mock
    private lateinit var tokenizer: Tokenizer

    @Mock
    private lateinit var indexStorage: IndexStorage

    @Mock
    private lateinit var filter: Filter

    @Test
    fun test_meta_returnsNotEmptyString() {
        val testSubject = FileIndexerImpl(tokenizer, indexStorage, filter)

        val result = testSubject.meta()

        assertFalse(result.isBlank())
    }

    @Test
    fun test_state_returnsNotEmptyString() {
        val testSubject = FileIndexerImpl(tokenizer, indexStorage, filter)

        val result = testSubject.state()

        assertFalse(result.isBlank())
    }


    @Test
    fun test_addToIndex_addsWordsToTheIndexFromTokenizer() = runBlocking {
        val path = testFile("test.txt")
        val testSubject = FileIndexerImpl(tokenizer, indexStorage, filter)
        val wordsFlow = flowOf("word1", "word2")
        `when`(tokenizer.tokenize(any())).thenReturn(wordsFlow)
        `when`(filter.isAllowedPath(any())).thenReturn(true)

        testSubject.addToIndex(path)

        verify(indexStorage).add(wordsFlow, path)
    }

    @Test
    fun test_addToIndex_addsParentDirectoryToWatchService() = runBlocking {
        val path = testFile("test.txt")
        val testSubject = FileIndexerImpl(tokenizer, indexStorage, filter)
        val wordsFlow = flowOf("word1", "word2")
        `when`(tokenizer.tokenize(any())).thenReturn(wordsFlow)
        `when`(filter.isAllowedPath(any())).thenReturn(true)

        testSubject.addToIndex(path)

        assertEquals(setOf(path.parent), testSubject.watchedDirectories)
    }

    @Test
    fun test_addToIndex_addsFileToWatchedList() = runBlocking {
        val path = testFile("test.txt")
        val testSubject = FileIndexerImpl(tokenizer, indexStorage, filter)
        val wordsFlow = flowOf("word1", "word2")
        `when`(tokenizer.tokenize(any())).thenReturn(wordsFlow)
        `when`(filter.isAllowedPath(any())).thenReturn(true)

        testSubject.addToIndex(path)

        assertEquals(setOf(path), testSubject.watchedFiles)
    }

    @Test
    fun test_addToIndex_addsAllFilesFromDirectory() = runBlocking {
        val path = testFile("test.txt")
        val testSubject = FileIndexerImpl(tokenizer, indexStorage, filter)
        val wordsFlow = flowOf("word1", "word2")
        `when`(tokenizer.tokenize(any())).thenReturn(wordsFlow)
        `when`(filter.isAllowedPath(any())).thenReturn(true)

        testSubject.addToIndex(path.parent)

        assertEquals(setOf(path), testSubject.watchedFiles)
    }

    @Test
    fun test_addToIndex_filtersWithFilter() = runBlocking {
        val path = testFile("test.txt")
        val testSubject = FileIndexerImpl(tokenizer, indexStorage, filter)
        `when`(filter.isAllowedPath(any())).thenReturn(false)

        testSubject.addToIndex(path)

        verifyNoInteractions(indexStorage)
    }

    @Test
    fun test_removeFromIndex_forFile() = runBlocking {
        val path = testFile("test.txt")
        val testSubject = FileIndexerImpl(tokenizer, indexStorage, filter)
        val wordsFlow = flowOf("word1", "word2")
        `when`(tokenizer.tokenize(any())).thenReturn(wordsFlow)
        `when`(filter.isAllowedPath(any())).thenReturn(true)
        testSubject.addToIndex(path)

        testSubject.removeFromIndex(path)

        assertEquals(setOf(), testSubject.watchedFiles)
        assertEquals(setOf(), testSubject.watchedDirectories)
    }

    @Test
    fun test_removeFromIndex_forParent() = runBlocking {
        val path = testFile("test.txt")
        val testSubject = FileIndexerImpl(tokenizer, indexStorage, filter)
        val wordsFlow = flowOf("word1", "word2")
        `when`(tokenizer.tokenize(any())).thenReturn(wordsFlow)
        `when`(filter.isAllowedPath(any())).thenReturn(true)
        testSubject.addToIndex(path)

        testSubject.removeFromIndex(path.parent)

        assertEquals(setOf(), testSubject.watchedFiles)
        assertEquals(setOf(), testSubject.watchedDirectories)
    }

    @Test
    fun test_search_returnsResultsFromIndex() = runBlocking {
        val pathFlow = flowOf(Path.of("word1"), Path.of("word2"))
        val testSubject = FileIndexerImpl(tokenizer, indexStorage, filter)
        `when`(indexStorage.search(any())).thenReturn(pathFlow)

        val result = testSubject.search("test")

        assertEquals(pathFlow, result)
    }

    @Test
    fun test_withFilter_returnsNewIndexer() = runBlocking {
        val testSubject = FileIndexerImpl(tokenizer, indexStorage, filter)

        val result = testSubject.withFilter(filter)

        assertNotEquals(result, testSubject)
    }

    @Test
    fun test_withTokenizer_returnsNewIndexer() = runBlocking {
        val testSubject = FileIndexerImpl(tokenizer, indexStorage, filter)

        val result = testSubject.withTokenizer(tokenizer)

        assertNotEquals(result, testSubject)
    }

    @Test
    fun test_withStorage_returnsNewIndexer() = runBlocking {
        val testSubject = FileIndexerImpl(tokenizer, indexStorage, filter)

        val result = testSubject.withStorage(indexStorage)

        assertNotEquals(result, testSubject)
    }


    private fun testFile(fileName: String): Path {
        val tempDir = Files.createTempDirectory("storage-cache").toFile()
        val file = File(tempDir, fileName)
        file.createNewFile()
        return file.toPath()
    }
}