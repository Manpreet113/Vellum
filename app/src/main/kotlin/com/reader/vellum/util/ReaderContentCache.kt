package com.reader.vellum.util

import android.util.LruCache

object ReaderContentCache {
    private val cbzPageLists = object : LruCache<String, List<String>>(64) {}
    private val epubManifests = object : LruCache<String, EpubManifest>(32) {}
    private val epubChapters = object : LruCache<String, String>(32) {
        override fun sizeOf(key: String, value: String): Int = value.length
    }

    fun getCbzPageNames(uriString: String): List<String>? = synchronized(cbzPageLists) {
        cbzPageLists.get(uriString)
    }

    fun putCbzPageNames(uriString: String, pageNames: List<String>) = synchronized(cbzPageLists) {
        cbzPageLists.put(uriString, pageNames)
    }

    fun getEpubManifest(uriString: String): EpubManifest? = synchronized(epubManifests) {
        epubManifests.get(uriString)
    }

    fun putEpubManifest(uriString: String, manifest: EpubManifest) = synchronized(epubManifests) {
        epubManifests.put(uriString, manifest)
    }

    fun getEpubChapter(uriString: String, chapterPath: String): String? = synchronized(epubChapters) {
        epubChapters.get("$uriString|$chapterPath")
    }

    fun putEpubChapter(uriString: String, chapterPath: String, html: String) = synchronized(epubChapters) {
        epubChapters.put("$uriString|$chapterPath", html)
    }
}
