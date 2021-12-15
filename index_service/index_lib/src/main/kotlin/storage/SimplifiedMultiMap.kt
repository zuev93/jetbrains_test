package storage

import MetaInfo

/**
 * Even though interfaces implies that this is map like structure it is not
 * limited to maps only.
 *
 * Depends on data nature other structures like tries, ternary search true,
 * DAFSA or Aho–Corasick might be preferred over others
 */
interface SimplifiedMultiMap<T, U> : MetaInfo {
    val state: String

    fun add(key: T, value: U)

    operator fun get(key: T): MutableSet<U>?

    fun remove(key: T)
}

class MutableMapWrap<T, U> : SimplifiedMultiMap<T, U> {
    private val map: MutableMap<T, MutableSet<U>> = mutableMapOf()

    override val state: String get() = "${map.size} objects in the map"

    override fun meta(): String = "Simple hash map."

    override fun add(key: T, value: U) {
        map.getOrPut(key) { mutableSetOf() }.add(value)
    }

    override fun get(key: T): MutableSet<U>? {
        return map[key]
    }

    override fun remove(key: T) {
        map.remove(key)
    }
}