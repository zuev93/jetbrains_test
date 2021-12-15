package tokenizer

import MetaInfo
import java.io.BufferedReader
import kotlinx.coroutines.flow.Flow

interface Tokenizer : MetaInfo {
  /**
   * Tokenize the given reader to tokens flow.
   */
  fun tokenize(bufferedReader: BufferedReader): Flow<String>
}