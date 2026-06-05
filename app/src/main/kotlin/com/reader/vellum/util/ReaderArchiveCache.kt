package com.reader.vellum.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ReaderArchiveCache {
    private const val ARCHIVE_DIR = "reader_archives"

    fun ensureLocalArchive(context: Context, uriString: String): File {
        val archiveDir = File(context.cacheDir, ARCHIVE_DIR).apply { mkdirs() }
        val finalFile = File(archiveDir, "${BookIdentity.filesystemKey(uriString)}.zip")
        if (finalFile.exists() && finalFile.length() > 0L) {
            return finalFile
        }

        val tempFile = File(archiveDir, "temp_${BookIdentity.filesystemKey(uriString)}.zip")
        val uri = Uri.parse(uriString)
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException("Failed to open archive stream for $uriString")

            if (!tempFile.renameTo(finalFile)) {
                throw IllegalStateException("Failed to rename temporary archive to final archive file")
            }
        } catch (e: Exception) {
            if (tempFile.exists()) {
                tempFile.delete()
            }
            throw e
        }

        return finalFile
    }
}
