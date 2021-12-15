package tokenizer

import MetaInfo
import java.io.BufferedReader
import kotlinx.coroutines.flow.Flow

interface Tokenizer : MetaInfo {
  fun tokenize(bufferedReader: BufferedReader): Flow<String>
}