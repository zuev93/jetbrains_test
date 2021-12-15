package filter

import MetaInfo
import java.nio.file.Path

interface Filter : MetaInfo {

    fun isAllowedPath(path: Path): Boolean
}