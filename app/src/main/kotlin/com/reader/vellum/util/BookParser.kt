package com.reader.vellum.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.reader.vellum.domain.model.Book
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

class BookParser(private val context: Context) {

    private val coverDir = File(context.cacheDir, "covers").apply { if (!exists()) mkdirs() }
    private val epubParser = EpubParser(context)

    fun parseDocumentFile(document: DocumentFile, collectionName: String?): Book? {
        val extension = document.name?.substringAfterLast('.', "")?.lowercase() ?: ""
        return try {
            when (extension) {
                "cbz", "zip" -> parseCbz(document, collectionName)
                "pdf" -> parsePdf(document, collectionName)
                "epub" -> epubParser.parseEpub(document, collectionName)
                else -> null
            }
        } catch (e: Exception) {
            Log.e("BookParser", "Error parsing ${document.uri}", e)
            null
        }
    }

    private fun parseCbz(document: DocumentFile, collectionName: String?): Book {
        var coverPath: String? = null
        var totalPages = 0
        val uriString = document.uri.toString()
        val coverKey = BookIdentity.filesystemKey(uriString)
        
        context.contentResolver.openInputStream(document.uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && isImage(entry.name)) {
                        totalPages++
                        if (coverPath == null) {
                            val coverFile = File(coverDir, "${coverKey}_cover.jpg")
                            FileOutputStream(coverFile).use { output ->
                                zip.copyTo(output)
                            }
                            coverPath = coverFile.absolutePath
                        }
                    }
                    entry = zip.nextEntry
                }
            }
        }

        return Book(
            id = BookIdentity.stableBookId(uriString),
            title = document.name?.substringBeforeLast('.') ?: "Unknown",
            author = null,
            filePath = uriString,
            uriString = uriString,
            coverPath = coverPath,
            format = "cbz",
            totalPages = totalPages,
            collectionName = collectionName
        )
    }

    private fun parsePdf(document: DocumentFile, collectionName: String?): Book {
        var coverPath: String? = null
        var totalPages = 0
        val uriString = document.uri.toString()
        val coverKey = BookIdentity.filesystemKey(uriString)

        context.contentResolver.openFileDescriptor(document.uri, "r")?.use { pfd ->
            val renderer = PdfRenderer(pfd)
            totalPages = renderer.pageCount

            if (totalPages > 0) {
                renderer.openPage(0).use { page ->
                    val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    
                    val coverFile = File(coverDir, "${coverKey}_cover.jpg")
                    FileOutputStream(coverFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    }
                    coverPath = coverFile.absolutePath
                    bitmap.recycle()
                }
            }
            renderer.close()
        }

        return Book(
            id = BookIdentity.stableBookId(uriString),
            title = document.name?.substringBeforeLast('.') ?: "Unknown",
            author = null,
            filePath = uriString,
            uriString = uriString,
            coverPath = coverPath,
            format = "pdf",
            totalPages = totalPages,
            collectionName = collectionName
        )
    }

    private fun isImage(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp")
    }
}
