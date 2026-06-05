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
import androidx.sqlite.db.SimpleSQLiteQuery
import java.util.Locale

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


    private fun buildSmartSearchQuery(
        query: String,
        hideCompleted: Boolean,
        sortOrder: SortOrder,
        completedOnly: Boolean = false
    ): SimpleSQLiteQuery {
        val terms = query.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        val sql = StringBuilder("SELECT * FROM books")
        val args = mutableListOf<Any>()
        
        val conditions = mutableListOf<String>()
        
        if (completedOnly) {
            conditions.add("progress >= 1.0")
        } else if (hideCompleted) {
            conditions.add("progress < 1.0")
        }
        
        for (term in terms) {
            val cleanTerm = term.replace(Regex("[^a-zA-Z0-9]"), "")
            if (cleanTerm.isBlank()) continue
            
            val isNumeric = cleanTerm.all { it.isDigit() }
            if (isNumeric) {
                val numVal = cleanTerm.toIntOrNull()
                if (numVal != null) {
                    val var1 = numVal.toString()
                    val var2 = var1.padStart(2, '0')
                    val var3 = var1.padStart(3, '0')
                    
                    conditions.add("(title LIKE ? OR title LIKE ? OR title LIKE ?)")
                    args.add("%$var1%")
                    args.add("%$var2%")
                    args.add("%$var3%")
                } else {
                    conditions.add("title LIKE ?")
                    args.add("%$cleanTerm%")
                }
            } else {
                conditions.add("title LIKE ?")
                args.add("%$cleanTerm%")
            }
        }
        
        if (conditions.isNotEmpty()) {
            sql.append(" WHERE ").append(conditions.joinToString(" AND "))
        }
        
        when (sortOrder) {
            SortOrder.RECENT -> sql.append(" ORDER BY lastRead DESC")
            SortOrder.TITLE_ASC -> sql.append(" ORDER BY title ASC")
            SortOrder.TITLE_DESC -> sql.append(" ORDER BY title DESC")
        }
        
        return SimpleSQLiteQuery(sql.toString(), args.toTypedArray())
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
                val query = buildSmartSearchQuery(searchQuery, hideCompleted, sortOrder)
                bookDao.searchBooksRaw(query)
            }
        ).flow
    }

    fun getCompletedBooksPaged(searchQuery: String = ""): Flow<PagingData<Book>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = {
                val query = buildSmartSearchQuery(searchQuery, hideCompleted = false, sortOrder = SortOrder.RECENT, completedOnly = true)
                bookDao.searchBooksRaw(query)
            }
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

    fun getCompletedCollectionInfo(): Flow<List<CollectionInfo>> {
        return bookDao.getCompletedCollectionInfo()
    }

    suspend fun getAllBooksSync(): List<Book> {
        return bookDao.getAllBooks()
    }

    suspend fun getBookByPath(path: String): Book? {
        return bookDao.getBookByPath(path)
    }

    suspend fun getBookByPathOrUri(path: String): Book? {
        return bookDao.getBookByPathOrUri(path)
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
