package com.reader.vellum

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reader.vellum.data.repository.BookRepository
import com.reader.vellum.util.BookParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val bookParser: BookParser,
    @param:dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val _navigateToReader = MutableSharedFlow<String>()
    val navigateToReader = _navigateToReader.asSharedFlow()

    fun handleIntentUri(uri: Uri) {
        viewModelScope.launch {
            val uriString = uri.toString()
            val existingBook = bookRepository.getBookByPath(uriString)
            
            if (existingBook != null) {
                _navigateToReader.emit(existingBook.id)
            } else {
                val isContentUri = uri.scheme == "content"
                val docFile = DocumentFile.fromSingleUri(context, uri)
                if (docFile != null && (isContentUri || docFile.exists())) {
                    val book = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        bookParser.parseDocumentFile(docFile, "Inbox")
                    }
                    if (book != null) {
                        bookRepository.upsertBook(book)
                        _navigateToReader.emit(book.id)
                    }
                }
            }
        }
    }
}
