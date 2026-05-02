package com.reader.vellum.util

import android.util.LruCache

object PageCache {
    private val cache = object : LruCache<String, ByteArray>(20 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ByteArray): Int {
            return value.size
        }
    }

    fun get(uriString: String, entryName: String): ByteArray? {
        return cache.get("$uriString|$entryName")
    }

    fun put(uriString: String, entryName: String, data: ByteArray) {
        cache.put("$uriString|$entryName", data)
    }

    fun clear() {
        cache.evictAll()
    }
}
