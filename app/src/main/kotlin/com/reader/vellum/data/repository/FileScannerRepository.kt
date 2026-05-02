package com.reader.vellum.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.reader.vellum.util.BookParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

data class ScanProgress(
    val current: Int,
    val total: Int,
    val currentFileName: String = ""
)

@Singleton
class FileScannerRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val bookRepository: BookRepository,
    private val bookParser: BookParser
) {
    private data class ScannedFile(
        val document: DocumentFile,
        val collectionName: String?,
        val uriString: String
    )

    private companion object {
        const val PARSE_CONCURRENCY = 4
    }

    fun scanDirectory(rootUriString: String): Flow<ScanProgress> = channelFlow {
        Log.d("FileScanner", "Starting scan for: $rootUriString")
        val rootUri = Uri.parse(rootUriString)
        val rootDoc = DocumentFile.fromTreeUri(context, rootUri)
        
        if (rootDoc == null || !rootDoc.isDirectory) {
            Log.e("FileScanner", "Invalid directory URI or not a directory: $rootUriString")
            return@channelFlow
        }

        val allSupportedFiles = mutableListOf<ScannedFile>()

        send(ScanProgress(0, 0, "Initializing scan..."))

        suspend fun walk(doc: DocumentFile, collectionName: String?) {
            val files = withContext(Dispatchers.IO) { doc.listFiles() }
            Log.d("FileScanner", "Walking ${doc.name}, found ${files.size} items")
            
            for (child in files) {
                if (child.isDirectory) {
                    walk(child, child.name ?: collectionName)
                } else if (child.isFile) {
                    val name = child.name ?: continue
                    if (name.startsWith(".") || name.contains(".trashed")) continue
                    val ext = name.substringAfterLast('.', "").lowercase()
                    if (isSupportedFormat(ext)) {
                        allSupportedFiles.add(
                            ScannedFile(
                                document = child,
                                collectionName = collectionName,
                                uriString = child.uri.toString()
                            )
                        )
                        if (allSupportedFiles.size % 10 == 0) {
                            send(ScanProgress(0, 0, "Found ${allSupportedFiles.size} files..."))
                        }
                    }
                }
            }
        }

        try {
            walk(rootDoc, null)
        } catch (e: Exception) {
            Log.e("FileScanner", "Error during directory walk", e)
        }

        val total = allSupportedFiles.size
        Log.d("FileScanner", "Walk complete. Found $total potential books")

        if (total == 0) {
            Log.d("FileScanner", "No supported files found.")
            send(ScanProgress(0, 0, "No supported files found"))
            return@channelFlow
        }

        val currentCount = AtomicInteger(0)
        val existingPaths = bookRepository.getExistingFilePaths(allSupportedFiles.map { it.uriString })
        val filesToProcess = allSupportedFiles.filterNot { it.uriString in existingPaths }
        val semaphore = Semaphore(PARSE_CONCURRENCY)

        if (existingPaths.isNotEmpty()) {
            val skipped = currentCount.addAndGet(existingPaths.size)
            send(ScanProgress(skipped, total, "Skipped ${existingPaths.size} existing books"))
        }

        withContext(Dispatchers.IO) {
            supervisorScope {
                filesToProcess.forEach { scannedFile ->
                    launch {
                        semaphore.withPermit {
                            try {
                                Log.d("FileScanner", "Parsing new book: ${scannedFile.document.name}")
                                val book = bookParser.parseDocumentFile(
                                    scannedFile.document,
                                    scannedFile.collectionName
                                )
                                if (book != null) {
                                    bookRepository.upsertBook(book)
                                    Log.d("FileScanner", "Inserted: ${book.title}")
                                }
                            } catch (e: Exception) {
                                Log.e("FileScanner", "Error processing ${scannedFile.document.uri}", e)
                            } finally {
                                val progress = currentCount.incrementAndGet()
                                send(ScanProgress(progress, total, scannedFile.document.name ?: ""))
                            }
                        }
                    }
                }
            }
        }
        Log.d("FileScanner", "All processing jobs launched and awaited")
    }

    private fun isSupportedFormat(extension: String): Boolean {
        return listOf("cbz", "zip", "pdf", "epub").contains(extension.lowercase())
    }
}
