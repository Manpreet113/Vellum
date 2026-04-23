package com.reader.vellum.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.reader.vellum.domain.model.Book
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

data class EpubManifest(
    val title: String,
    val coverPath: String?,
    val spine: List<String>, // Paths to HTML chapters inside the zip
    val opfPath: String
)

class EpubParser(private val context: Context) {

    private val coverDir = File(context.cacheDir, "covers").apply { if (!exists()) mkdirs() }

    fun parseEpub(document: DocumentFile, collectionName: String?): Book? {
        val uri = document.uri
        val uriString = uri.toString()
        var title = document.name?.substringBeforeLast('.') ?: "Unknown EPUB"
        var coverPath: String? = null
        
        try {
            val manifest = getManifest(uri)
            title = manifest.title
            coverPath = manifest.coverPath
        } catch (e: Exception) {
            Log.e("EpubParser", "Failed to parse manifest for $uri", e)
        }

        return Book(
            id = BookIdentity.stableBookId(uriString),
            title = title,
            author = null,
            filePath = uriString,
            uriString = uriString,
            coverPath = coverPath,
            format = "epub",
            totalPages = 0,
            collectionName = collectionName
        )
    }

    fun getManifest(uri: Uri): EpubManifest {
        val uriString = uri.toString()
        ReaderContentCache.getEpubManifest(uriString)?.let { return it }

        var opfPath: String? = null
        
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "META-INF/container.xml") {
                        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(zip)
                        val rootFile = doc.getElementsByTagName("rootfile").item(0)
                        opfPath = rootFile.attributes.getNamedItem("full-path").nodeValue
                        break
                    }
                    entry = zip.nextEntry
                }
            }
        }

        val opf = opfPath ?: throw Exception("No OPF file found in EPUB")

        var title = "Unknown"
        var coverId: String? = null
        val manifestItems = mutableMapOf<String, String>()
        val spineIds = mutableListOf<String>()

        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == opf) {
                        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(zip)
                        title = doc.getElementsByTagName("dc:title").item(0)?.textContent ?: "Unknown"
                        
                        val items = doc.getElementsByTagName("item")
                        for (i in 0 until items.length) {
                            val item = items.item(i)
                            val id = item.attributes.getNamedItem("id").nodeValue
                            val href = item.attributes.getNamedItem("href").nodeValue
                            manifestItems[id] = href
                            
                            val properties = item.attributes.getNamedItem("properties")?.nodeValue
                            if (properties == "cover-image") {
                                coverId = id
                            }
                        }

                        val spineNodes = doc.getElementsByTagName("itemref")
                        for (i in 0 until spineNodes.length) {
                            spineIds.add(spineNodes.item(i).attributes.getNamedItem("idref").nodeValue)
                        }
                        break
                    }
                    entry = zip.nextEntry
                }
            }
        }

        var coverLocalPath: String? = null
        val coverKey = BookIdentity.filesystemKey(uri.toString())
        if (coverId != null) {
            val relPath = manifestItems[coverId]
            if (relPath != null) {
                val fullPath = resolvePath(opf, relPath)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    ZipInputStream(input).use { zip ->
                        var entry = zip.nextEntry
                        while (entry != null) {
                            if (entry.name == fullPath) {
                                val coverFile = File(coverDir, "${coverKey}_cover.jpg")
                                FileOutputStream(coverFile).use { out -> zip.copyTo(out) }
                                coverLocalPath = coverFile.absolutePath
                                break
                            }
                            entry = zip.nextEntry
                        }
                    }
                }
            }
        }

        val spinePaths = spineIds.mapNotNull { id ->
            manifestItems[id]?.let { rel -> resolvePath(opf, rel) }
        }

        return EpubManifest(title, coverLocalPath, spinePaths, opf).also { manifest ->
            ReaderContentCache.putEpubManifest(uriString, manifest)
        }
    }

    fun getChapterContent(uri: Uri, chapterPath: String): String {
        val uriString = uri.toString()
        ReaderContentCache.getEpubChapter(uriString, chapterPath)?.let { return it }

        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == chapterPath) {
                        return zip.bufferedReader().use { it.readText() }.also { html ->
                            ReaderContentCache.putEpubChapter(uriString, chapterPath, html)
                        }
                    }
                    entry = zip.nextEntry
                }
            }
        }
        return ""
    }

    private fun resolvePath(basePath: String, relPath: String): String {
        if (!basePath.contains("/")) return relPath
        val parent = basePath.substringBeforeLast("/")
        return "$parent/$relPath"
    }
}
