package com.reader.vellum.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ReaderArchiveCache {
    private const val ARCHIVE_DIR = "reader_archives"

    fun ensureLocalArchive(context: Context, uriString: String): File {
        val archiveDir = File(context.cacheDir, ARCHIVE_DIR).apply { mkdirs() }
        val archiveFile = File(archiveDir, "${BookIdentity.filesystemKey(uriString)}.zip")
        if (archiveFile.exists() && archiveFile.length() > 0L) {
            return archiveFile
        }

        val uri = Uri.parse(uriString)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(archiveFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Failed to open archive stream for $uriString")

        return archiveFile
    }
}
