package com.reader.vellum.ui.screens.reader

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import com.reader.vellum.data.repository.BookRepository
import com.reader.vellum.domain.model.Book
import com.reader.vellum.util.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.zip.ZipFile
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val bookRepository: BookRepository,
    private val hardwareEventManager: HardwareEventManager,
    private val settingsManager: SettingsManager
) : ViewModel() {
    private data class PendingProgress(
        val bookId: String,
        val progress: Double,
        val lastRead: Long
    )

    private companion object {
        const val PROGRESS_WRITE_DEBOUNCE_MS = 750L
    }

    private val _uiState = MutableStateFlow<ReaderUiState>(ReaderUiState.Loading)
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    val mangaMode = settingsManager.mangaMode
    val tapToTurn = settingsManager.tapToTurn
    val volumeKeys = settingsManager.volumeKeys
    val adaptiveChroma = settingsManager.adaptiveChroma

    private var prefetchJob: Job? = null
    private var progressUpdateJob: Job? = null
    private var pendingProgress: PendingProgress? = null
    val hardwareEvents = hardwareEventManager.events
    private val epubParser = EpubParser(context)

    fun loadBook(id: String) {
        viewModelScope.launch {
            flushPendingProgress()

            // If already loading or loaded the same book, skip
            val current = _uiState.value
            if (current is ReaderUiState.Success && current.book.id == id) return@launch
            
            _uiState.value = ReaderUiState.Loading
            val book = withContext(Dispatchers.IO) {
                bookRepository.getBookById(id)
            }
            
            if (book == null) {
                _uiState.value = ReaderUiState.Error("Book not found")
                return@launch
            }

            // Extract Accent Color in background
            val accentColor = withContext(Dispatchers.IO) {
                try {
                    book.coverPath?.let { path ->
                        val options = BitmapFactory.Options().apply { inSampleSize = 4 } // Scale down for speed
                        val bitmap = BitmapFactory.decodeFile(path, options)
                        if (bitmap != null) {
                            val palette = Palette.from(bitmap).generate()
                            val swatch = palette.vibrantSwatch ?: palette.mutedSwatch ?: palette.dominantSwatch
                            bitmap.recycle()
                            swatch?.let { Color(it.rgb) }
                        } else null
                    }
                } catch (e: Exception) { null }
            }

            when (book.format) {
                "cbz", "zip" -> {
                    val pages = withContext(Dispatchers.IO) {
                        loadCbzPages(book.uriString ?: book.filePath)
                    }
                    if (pages.isEmpty()) {
                        _uiState.value = ReaderUiState.Error("No readable pages found")
                    } else {
                        val initialPage = if (pages.isNotEmpty()) {
                            (book.progress * (pages.size - 1)).toInt().coerceIn(0, pages.size - 1)
                        } else 0
                        _uiState.value = ReaderUiState.Success(book, pages, initialPage, accentColor)
                        startPrefetching(book.uriString ?: book.filePath, pages, initialPage)
                    }
                }
                "pdf" -> {
                    val pages = (0 until book.totalPages).map { 
                        PdfPageRequest(book.uriString ?: book.filePath, it)
                    }
                    if (pages.isEmpty()) {
                        _uiState.value = ReaderUiState.Error("PDF has no pages")
                    } else {
                        val initialPage = if (pages.isNotEmpty()) {
                            (book.progress * (pages.size - 1)).toInt().coerceIn(0, pages.size - 1)
                        } else 0
                        _uiState.value = ReaderUiState.Success(book, pages, initialPage, accentColor)
                    }
                }
                "epub" -> {
                    val pages = withContext(Dispatchers.IO) {
                        try {
                            val manifest = epubParser.getManifest(Uri.parse(book.uriString ?: book.filePath))
                            manifest.spine.map { path ->
                                EpubPageRequest(book.uriString ?: book.filePath, path)
                            }
                        } catch (e: Exception) { emptyList() }
                    }
                    if (pages.isEmpty()) {
                        _uiState.value = ReaderUiState.Error("Failed to load EPUB content")
                    } else {
                        val initialPage = if (pages.isNotEmpty()) {
                            (book.progress * (pages.size - 1)).toInt().coerceIn(0, pages.size - 1)
                        } else 0
                        _uiState.value = ReaderUiState.Success(book, pages, initialPage, accentColor)
                    }
                }
                else -> {
                    _uiState.value = ReaderUiState.Error("Format ${book.format} not supported")
                }
            }
        }
    }

    private fun startPrefetching(uriString: String, pages: List<Any>, startFrom: Int) {
        if (pages.isEmpty() || pages[0] !is CbzPageRequest) return
        
        prefetchJob?.cancel()
        prefetchJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val localArchive = ReaderArchiveCache.ensureLocalArchive(context, uriString)
                val targetPages = pages.subList(startFrom, (startFrom + 5).coerceAtMost(pages.size))
                val targetNames = targetPages.filterIsInstance<CbzPageRequest>().map { it.entryName }

                ZipFile(localArchive).use { zipFile ->
                    targetNames.forEach { entryName ->
                        if (PageCache.get(uriString, entryName) == null) {
                            val entry = zipFile.getEntry(entryName) ?: return@forEach
                            val data = zipFile.getInputStream(entry).use { it.readBytes() }
                            PageCache.put(uriString, entryName, data)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ReaderViewModel", "Prefetch failed", e)
            }
        }
    }

    private fun loadCbzPages(uriString: String): List<CbzPageRequest> {
        ReaderContentCache.getCbzPageNames(uriString)?.let { pageNames ->
            return pageNames.map { entryName -> CbzPageRequest(uriString, entryName) }
        }

        val pages = mutableListOf<CbzPageRequest>()
        try {
            val localArchive = ReaderArchiveCache.ensureLocalArchive(context, uriString)
            ZipFile(localArchive).use { zipFile ->
                zipFile.entries().asSequence().forEach { entry ->
                    if (!entry.isDirectory && isImage(entry.name)) {
                        pages.add(CbzPageRequest(uriString, entry.name))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ReaderViewModel", "Error loading pages", e)
        }
        return pages
            .sortedBy { it.entryName }
            .also { sortedPages ->
                ReaderContentCache.putCbzPageNames(
                    uriString,
                    sortedPages.map { it.entryName }
                )
            }
    }

    private fun isImage(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp")
    }

    fun updateProgress(pageIndex: Int) {
        val state = uiState.value as? ReaderUiState.Success ?: return
        pendingProgress = PendingProgress(
            bookId = state.book.id,
            progress = if (state.pages.size > 1) {
                (pageIndex.toDouble() / (state.pages.size - 1)).coerceIn(0.0, 1.0)
            } else {
                1.0
            },
            lastRead = System.currentTimeMillis()
        )

        progressUpdateJob?.cancel()
        progressUpdateJob = viewModelScope.launch(Dispatchers.IO) {
            delay(PROGRESS_WRITE_DEBOUNCE_MS)
            flushPendingProgress()
        }
        
        if (state.book.format == "cbz" || state.book.format == "zip") {
            startPrefetching(state.book.uriString ?: state.book.filePath, state.pages, pageIndex)
        }
    }

    private suspend fun flushPendingProgress() {
        val update = pendingProgress ?: return
        pendingProgress = null
        bookRepository.updateReadingProgress(update.bookId, update.progress, update.lastRead)
    }

    fun setMangaMode(enabled: Boolean) = viewModelScope.launch { settingsManager.setMangaMode(enabled) }
    fun setTapToTurn(enabled: Boolean) = viewModelScope.launch { settingsManager.setTapToTurn(enabled) }
    fun setVolumeKeys(enabled: Boolean) = viewModelScope.launch { settingsManager.setVolumeKeys(enabled) }
    fun setAdaptiveChroma(enabled: Boolean) = viewModelScope.launch { settingsManager.setAdaptiveChroma(enabled) }

    suspend fun getEpubChapter(uriString: String, path: String): String = withContext(Dispatchers.IO) {
        epubParser.getChapterContent(Uri.parse(uriString), path)
    }

    override fun onCleared() {
        super.onCleared()
        prefetchJob?.cancel()
        progressUpdateJob?.cancel()
        viewModelScope.launch(Dispatchers.IO) {
            flushPendingProgress()
        }
        PageCache.clear()
    }
}

data class EpubPageRequest(val uriString: String, val chapterPath: String)

sealed class ReaderUiState {
    object Loading : ReaderUiState()
    data class Success(
        val book: Book,
        val pages: List<Any>,
        val initialPage: Int,
        val accentColor: Color? = null
    ) : ReaderUiState()
    data class Error(val message: String) : ReaderUiState()
}
