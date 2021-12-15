package filter

import MetaInfo
import java.nio.file.Path

interface Filter : MetaInfo {

  /**
   * Returns true if the given path is allowed otherwise false.
   */
  fun isAllowedPath(path: Path): Boolean
}