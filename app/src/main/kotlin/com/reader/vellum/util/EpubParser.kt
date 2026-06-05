package com.reader.vellum.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.reader.vellum.domain.model.Book

data class EpubTocItem(
    val title: String,
    val href: String
)

data class EpubManifest(
    val title: String,
    val author: String? = null,
    val coverPath: String?,
    val spine: List<String>,
    val opfPath: String,
    val toc: List<EpubTocItem> = emptyList()
)

class EpubParser(private val context: Context) {
    fun parseEpub(document: DocumentFile, originalUriString: String, collectionName: String?): Book? {
        val filePath = document.uri.toString()
        val manifest = runCatching { getManifest(document.uri) }.getOrNull()

        return Book(
            id = BookIdentity.stableBookId(originalUriString),
            title = manifest?.title ?: document.name?.substringBeforeLast('.') ?: "Unknown EPUB",
            author = manifest?.author,
            filePath = filePath,
            uriString = originalUriString,
            coverPath = manifest?.coverPath,
            format = "epub",
            totalPages = 0,
            collectionName = collectionName
        )
    }

    fun getManifest(uri: Uri): EpubManifest {
        return EpubPublication.open(context, uri.toString()).getManifest()
    }

    fun getChapterContent(uri: Uri, chapterPath: String): String {
        return EpubPublication.open(context, uri.toString()).getChapterContent(chapterPath)
    }

    fun prefetchChapters(uri: Uri, chapterPaths: List<String>) {
        EpubPublication.open(context, uri.toString()).prefetchChapters(chapterPaths)
    }

    fun loadResource(uri: Uri, archivePath: String): EpubResource? {
        return EpubPublication.open(context, uri.toString()).loadResource(archivePath)
    }
}
