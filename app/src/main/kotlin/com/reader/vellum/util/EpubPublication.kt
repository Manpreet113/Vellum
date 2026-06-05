package com.reader.vellum.util

import android.content.Context
import android.util.LruCache
import android.webkit.MimeTypeMap
import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.domain.Resource
import nl.siegmann.epublib.domain.TOCReference
import nl.siegmann.epublib.epub.EpubReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.URLDecoder
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

data class EpubResource(
    val bytes: ByteArray,
    val mimeType: String,
    val encoding: String = "UTF-8"
)

data class EpubHrefTarget(
    val archivePath: String,
    val fragment: String? = null
)

class EpubPublication private constructor(
    private val context: Context,
    val uriString: String
) {
    private val coverDir = File(context.cacheDir, "covers").apply { mkdirs() }

    fun getManifest(): EpubManifest {
        ReaderContentCache.getEpubManifest(uriString)?.let { return it }

        val book = loadBook()
        val opfPath = book.opfResource?.href.orEmpty()
        val coverPath = extractCover(book.coverImage)
        val spinePaths = book.spine?.spineReferences
            ?.mapNotNull { spineReference -> spineReference.resource?.href?.let(::normalizeHref) }
            .orEmpty()
        val toc = book.tableOfContents?.tocReferences
            ?.flatMap(::flattenToc)
            .orEmpty()

        val manifest = EpubManifest(
            title = book.title?.trim().takeUnless { it.isNullOrEmpty() } ?: "Unknown",
            author = book.metadata?.authors
                ?.firstOrNull()
                ?.let { author ->
                    listOfNotNull(author.firstname?.trim(), author.lastname?.trim())
                        .joinToString(" ")
                        .trim()
                        .ifBlank { null }
                },
            coverPath = coverPath,
            spine = spinePaths,
            opfPath = normalizeHref(opfPath),
            toc = toc
        )

        ReaderContentCache.putEpubManifest(uriString, manifest)
        return manifest
    }

    fun getChapterContent(chapterPath: String): String {
        ReaderContentCache.getEpubChapter(uriString, chapterPath)?.let { return it }

        return readArchiveText(chapterPath)
            .orEmpty()
            .also { html ->
                ReaderContentCache.putEpubChapter(uriString, chapterPath, html)
            }
    }

    fun prefetchChapters(chapterPaths: List<String>) {
        val missingPaths = chapterPaths
            .distinct()
            .filter { chapterPath -> ReaderContentCache.getEpubChapter(uriString, chapterPath) == null }
        if (missingPaths.isEmpty()) return

        missingPaths.forEach { chapterPath ->
            readArchiveText(chapterPath)
                ?.let { html -> ReaderContentCache.putEpubChapter(uriString, chapterPath, html) }
        }
    }

    fun loadResource(archivePath: String): EpubResource? {
        val normalizedPath = normalizeHref(archivePath)
        val resource = resolveResource(normalizedPath)
        val bytes = resource?.data ?: readArchiveBytes(normalizedPath) ?: return null
        val mediaTypeName = resource?.mediaType?.name
        return EpubResource(
            bytes = bytes,
            mimeType = mediaTypeName?.takeIf { it.contains('/') } ?: mimeTypeFor(normalizedPath),
            encoding = resource?.inputEncoding?.takeUnless { it.isNullOrBlank() } ?: "UTF-8"
        )
    }

    private fun loadBook(): Book {
        return synchronized(bookCache) {
            bookCache[uriString] ?: run {
                val localFile = ReaderArchiveCache.ensureLocalArchive(context, uriString)
                FileInputStream(localFile).use { input ->
                    EpubReader().readEpub(input)
                }.also { book ->
                    bookCache.put(uriString, book)
                }
            }
        }
    }

    private fun resolveResource(archivePath: String): Resource? {
        val book = loadBook()
        val normalizedPath = normalizeHref(archivePath)
        val decodedPath = decodeHref(normalizedPath)

        return book.resources?.getByHref(normalizedPath)
            ?: book.resources?.getByHref(decodedPath)
            ?: book.resources?.getByIdOrHref(normalizedPath)
            ?: book.resources?.getByIdOrHref(decodedPath)
            ?: book.resources?.all?.firstOrNull { resource ->
                val href = normalizeHref(resource.href)
                href == normalizedPath ||
                    href == decodedPath ||
                    href.endsWith("/$normalizedPath") ||
                    href.endsWith("/$decodedPath")
            }
    }

    private fun readArchiveText(archivePath: String): String? {
        val resource = resolveResource(archivePath)
        val encoding = resource?.inputEncoding
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: "UTF-8"
        val bytes = readArchiveBytes(archivePath) ?: resource?.data ?: return null
        return runCatching {
            bytes.toString(charset(encoding))
        }.getOrElse {
            bytes.toString(Charsets.UTF_8)
        }
    }

    private fun readArchiveBytes(archivePath: String): ByteArray? {
        val localFile = ReaderArchiveCache.ensureLocalArchive(context, uriString)
        val normalizedPath = normalizeHref(archivePath)
        val decodedPath = decodeHref(normalizedPath)

        return ZipFile(localFile).use { zip ->
            findArchiveEntry(zip, normalizedPath, decodedPath)
                ?.let(zip::getInputStream)
                ?.use { input -> input.readBytes() }
        }
    }

    private fun findArchiveEntry(zip: ZipFile, normalizedPath: String, decodedPath: String): ZipEntry? {
        zip.getEntry(normalizedPath)?.let { return it }
        zip.getEntry(decodedPath)?.let { return it }

        val candidates = listOf(normalizedPath, decodedPath)
            .flatMap { path -> listOf(path, path.removePrefix("./"), path.removePrefix("/")) }
            .distinct()

        return zip.entries().asSequence().firstOrNull { entry ->
            val name = entry.name
            !entry.isDirectory && candidates.any { candidate ->
                name == candidate || name.endsWith("/$candidate")
            }
        }
    }

    private fun extractCover(coverResource: Resource?): String? {
        coverResource ?: return null
        val extension = coverResource.href
            ?.substringAfterLast('.', "")
            ?.lowercase(Locale.getDefault())
            ?.takeIf { it.isNotBlank() }
            ?: "jpg"
        val coverFile = File(coverDir, "${BookIdentity.filesystemKey(uriString)}_cover.$extension")
        FileOutputStream(coverFile).use { output ->
            output.write(coverResource.data ?: return null)
        }
        return coverFile.absolutePath
    }

    private fun flattenToc(reference: TOCReference): List<EpubTocItem> {
        val href = reference.completeHref?.takeIf { it.isNotBlank() }
            ?: reference.resource?.href?.takeIf { it.isNotBlank() }
            ?: return reference.children.flatMap(::flattenToc)

        val current = EpubTocItem(
            title = reference.title?.trim().orEmpty(),
            href = normalizeHref(href)
        )
        return buildList {
            add(current)
            addAll(reference.children.flatMap(::flattenToc))
        }
    }

    companion object {
        private val cache = object : LruCache<String, EpubPublication>(16) {}
        private val bookCache = object : LruCache<String, Book>(8) {}

        fun open(context: Context, uriString: String): EpubPublication = synchronized(cache) {
            cache.get(uriString) ?: EpubPublication(context.applicationContext, uriString).also {
                cache.put(uriString, it)
            }
        }

        fun resolveNavigationTarget(basePath: String, href: String): EpubHrefTarget? {
            if (href.isBlank()) return null
            val normalizedHref = href.trim()
            if (
                normalizedHref.startsWith("http://", ignoreCase = true) ||
                normalizedHref.startsWith("https://", ignoreCase = true) ||
                normalizedHref.startsWith("mailto:", ignoreCase = true) ||
                normalizedHref.startsWith("tel:", ignoreCase = true)
            ) {
                return null
            }

            val resolved = resolvePath(basePath, normalizedHref)
            return EpubHrefTarget(
                archivePath = resolved.substringBefore('#'),
                fragment = resolved.substringAfter('#', "").takeIf { it.isNotBlank() }
            )
        }

        private fun normalizeHref(href: String): String {
            if (href.isBlank()) return href
            val trimmed = href.trim()
            val path = trimmed.substringBefore('#').substringBefore('?').removePrefix("./").removePrefix("/")
            val fragment = trimmed.substringAfter('#', "")
            return if (fragment.isNotEmpty()) "$path#$fragment" else path
        }

        private fun decodeHref(href: String): String {
            return runCatching { URLDecoder.decode(href, Charsets.UTF_8.name()) }.getOrDefault(href)
        }

        private fun mimeTypeFor(path: String): String {
            val extension = path.substringAfterLast('.', "").lowercase(Locale.getDefault())
            return when {
                extension == "xhtml" || extension == "html" || extension == "htm" -> "text/html"
                extension == "css" -> "text/css"
                extension == "svg" -> "image/svg+xml"
                extension == "ncx" || extension == "xml" -> "application/xml"
                extension == "js" -> "application/javascript"
                extension == "otf" -> "font/otf"
                extension == "ttf" -> "font/ttf"
                extension == "woff" -> "font/woff"
                extension == "woff2" -> "font/woff2"
                else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
            }
        }

        private fun resolvePath(basePath: String, relPath: String): String {
            if (relPath.isBlank()) return relPath
            if (relPath.startsWith("/")) return relPath.removePrefix("/")

            val anchorlessBase = basePath.substringBefore('#').substringBefore('?')
            val anchorlessRelative = relPath.substringBefore('?')
            val baseDir = if (anchorlessBase.contains("/")) {
                anchorlessBase.substringBeforeLast("/") + "/"
            } else {
                ""
            }

            val resolved = java.net.URI(null, null, "/$baseDir", null)
                .resolve(anchorlessRelative)
                .normalize()
                .path
                .removePrefix("/")

            val fragment = relPath.substringAfter('#', "")
            return if (fragment.isNotEmpty()) "$resolved#$fragment" else resolved
        }
    }
}
