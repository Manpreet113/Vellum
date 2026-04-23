package com.reader.vellum.ui.screens.library

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.reader.vellum.data.local.CollectionInfo
import com.reader.vellum.data.repository.BookRepository
import com.reader.vellum.data.repository.FileScannerRepository
import com.reader.vellum.data.repository.ScanProgress
import com.reader.vellum.data.repository.SortOrder
import com.reader.vellum.domain.model.Book
import com.reader.vellum.util.SettingsManager
import com.reader.vellum.util.TiltSensorManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

enum class LibraryTab {
    COLLECTIONS,
    BOOKS,
    COMPLETED
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val bookRepository: BookRepository,
    private val fileScannerRepository: FileScannerRepository,
    private val bookParser: com.reader.vellum.util.BookParser,
    private val settingsManager: com.reader.vellum.util.SettingsManager,
    private val tiltSensorManager: com.reader.vellum.util.TiltSensorManager
) : ViewModel() {

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanProgress = MutableStateFlow<ScanProgress?>(null)
    val scanProgress: StateFlow<ScanProgress?> = _scanProgress.asStateFlow()

    private val _bookCount = MutableStateFlow(0)
    val bookCount: StateFlow<Int> = _bookCount.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.RECENT)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _selectedTab = MutableStateFlow(LibraryTab.COLLECTIONS)
    val selectedTab: StateFlow<LibraryTab> = _selectedTab.asStateFlow()

    val tilt = tiltSensorManager.tiltFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), androidx.compose.ui.geometry.Offset.Zero)

    val hideCompleted = settingsManager.hideCompleted
    val mangaMode = settingsManager.mangaMode
    val tapToTurn = settingsManager.tapToTurn
    val volumeKeys = settingsManager.volumeKeys
    val adaptiveChroma = settingsManager.adaptiveChroma

    val continueReadingBooks: Flow<List<Book>> = bookRepository.getContinueReadingBooks()

    val collections: Flow<List<CollectionInfo>> = bookRepository.getCollections()

    @OptIn(ExperimentalCoroutinesApi::class)
    val books: Flow<PagingData<Book>> = combine(_searchQuery, _sortOrder, hideCompleted) { query, sort, hide ->
        Triple(query, sort, hide)
    }.flatMapLatest { (query, sort, hide) ->
        bookRepository.getAllBooksPaged(query, sort, hide)
    }.cachedIn(viewModelScope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val completedBooks: Flow<PagingData<Book>> = _searchQuery.flatMapLatest { query ->
        bookRepository.getCompletedBooksPaged(query)
    }.cachedIn(viewModelScope)

    private val _selectedCollection = MutableStateFlow<String?>(null)
    val selectedCollection: StateFlow<String?> = _selectedCollection.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val booksInCollection: Flow<PagingData<Book>> = combine(_selectedCollection, hideCompleted) { name, hide ->
        name to hide
    }.flatMapLatest { (name, hide) ->
        if (name != null) bookRepository.getBooksInCollectionPaged(name, hide)
        else flowOf(PagingData.empty())
    }.cachedIn(viewModelScope)

    init {
        updateBookCount()
    }

    fun onCollectionSelected(name: String?) {
        _selectedCollection.value = name
    }

    fun onTabSelected(tab: LibraryTab) {
        _selectedTab.value = tab
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onSortOrderChanged(sort: SortOrder) {
        _sortOrder.value = sort
    }

    fun updateBookCount() {
        viewModelScope.launch {
            _bookCount.value = bookRepository.getBookCount()
        }
    }

    fun importFile(uri: Uri, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            val uriString = uri.toString()
            val existingBook = bookRepository.getBookByPath(uriString)
            if (existingBook != null) {
                onComplete(existingBook.id)
            } else {
                val docFile = androidx.documentfile.provider.DocumentFile.fromSingleUri(context, uri)
                if (docFile != null && docFile.exists()) {
                    val book = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        bookParser.parseDocumentFile(docFile, "Inbox")
                    }
                    if (book != null) {
                        bookRepository.upsertBook(book)
                        updateBookCount()
                        onComplete(book.id)
                    }
                }
            }
        }
    }

    fun scanDirectory(path: String) {
        viewModelScope.launch {
            _isScanning.value = true
            _scanProgress.value = ScanProgress(0, 0)
            fileScannerRepository.scanDirectory(path).collect { progress ->
                _scanProgress.value = progress
            }
            updateBookCount()
            _isScanning.value = false
            _scanProgress.value = null
        }
    }

    fun backupProgress(uri: Uri) {
        viewModelScope.launch {
            try {
                val books = bookRepository.getAllBooksSync()
                val json = JSONArray()
                books.forEach { book ->
                    val obj = JSONObject()
                    obj.put("path", book.filePath)
                    obj.put("progress", book.progress)
                    obj.put("lastRead", book.lastRead)
                    json.put(obj)
                }
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toString().toByteArray())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun restoreProgress(uri: Uri) {
        viewModelScope.launch {
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader().readText()
                } ?: return@launch
                val json = JSONArray(jsonString)
                for (i in 0 until json.length()) {
                    val obj = json.getJSONObject(i)
                    val path = obj.getString("path")
                    val progress = obj.getDouble("progress")
                    val lastRead = obj.getLong("lastRead")
                    
                    val book = bookRepository.getBookByPath(path)
                    if (book != null) {
                        bookRepository.updateBook(book.copy(progress = progress, lastRead = lastRead))
                    }
                }
                updateBookCount()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Settings methods
    fun setMangaMode(enabled: Boolean) = viewModelScope.launch { settingsManager.setMangaMode(enabled) }
    fun setTapToTurn(enabled: Boolean) = viewModelScope.launch { settingsManager.setTapToTurn(enabled) }
    fun setVolumeKeys(enabled: Boolean) = viewModelScope.launch { settingsManager.setVolumeKeys(enabled) }
    fun setAdaptiveChroma(enabled: Boolean) = viewModelScope.launch { settingsManager.setAdaptiveChroma(enabled) }
    fun setHideCompleted(enabled: Boolean) = viewModelScope.launch { settingsManager.setHideCompleted(enabled) }
}
