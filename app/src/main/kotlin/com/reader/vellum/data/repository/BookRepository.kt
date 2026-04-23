package com.reader.vellum.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.reader.vellum.data.local.BookDao
import com.reader.vellum.data.local.CollectionInfo
import com.reader.vellum.domain.model.Book
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

enum class SortOrder {
    RECENT,
    TITLE_ASC,
    TITLE_DESC
}

@Singleton
class BookRepository @Inject constructor(
    private val bookDao: BookDao
) {
    private companion object {
        const val SQLITE_IN_LIMIT_SAFE = 900
    }


    fun getAllBooksPaged(
        searchQuery: String = "",
        sortOrder: SortOrder = SortOrder.RECENT,
        hideCompleted: Boolean = true
    ): Flow<PagingData<Book>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = true,
                initialLoadSize = 40
            ),
            pagingSourceFactory = {
                when (sortOrder) {
                    SortOrder.RECENT -> bookDao.searchBooksByRecent(searchQuery, hideCompleted)
                    SortOrder.TITLE_ASC -> bookDao.searchBooksByTitleAsc(searchQuery, hideCompleted)
                    SortOrder.TITLE_DESC -> bookDao.searchBooksByTitleDesc(searchQuery, hideCompleted)
                }
            }
        ).flow
    }

    fun getCompletedBooksPaged(searchQuery: String = ""): Flow<PagingData<Book>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = { bookDao.getCompletedBooksPaged(searchQuery) }
        ).flow
    }

    fun getBooksInCollectionPaged(
        collectionName: String,
        hideCompleted: Boolean = true
    ): Flow<PagingData<Book>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = { bookDao.getBooksByCollectionPaged(collectionName, hideCompleted) }
        ).flow
    }

    fun getContinueReadingBooks(): Flow<List<Book>> {
        return bookDao.getContinueReadingBooks()
    }

    fun getCollections(): Flow<List<CollectionInfo>> {
        return bookDao.getCollectionsWithCount()
    }

    suspend fun getAllBooksSync(): List<Book> {
        return bookDao.getAllBooks()
    }

    suspend fun getBookByPath(path: String): Book? {
        return bookDao.getBookByPath(path)
    }

    suspend fun getExistingFilePaths(paths: List<String>): Set<String> {
        if (paths.isEmpty()) return emptySet()
        return paths
            .chunked(SQLITE_IN_LIMIT_SAFE)
            .flatMap { chunk -> bookDao.getExistingFilePaths(chunk) }
            .toSet()
    }

    suspend fun getBookById(id: String): Book? {
        return bookDao.getBookById(id)
    }

    suspend fun getBookCount(): Int {
        return bookDao.getBookCount()
    }

    suspend fun updateBook(book: Book) {
        bookDao.updateBook(book)
    }

    suspend fun upsertBook(book: Book) {
        bookDao.insertBook(book)
    }

    suspend fun updateReadingProgress(bookId: String, progress: Double, lastRead: Long) {
        bookDao.updateReadingProgress(bookId, progress, lastRead)
    }

    suspend fun deleteBook(book: Book) {
        bookDao.deleteBook(book)
    }
}
