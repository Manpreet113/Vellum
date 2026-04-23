package com.reader.vellum.data.local

import androidx.paging.PagingSource
import androidx.room.*
import com.reader.vellum.domain.model.Book
import kotlinx.coroutines.flow.Flow

data class CollectionInfo(
    val collectionName: String?,
    val bookCount: Int
)

@Dao
interface BookDao {
    @Query("SELECT * FROM books WHERE title LIKE '%' || :query || '%' AND (CASE WHEN :hideCompleted = 1 THEN progress < 1.0 ELSE 1 END) ORDER BY lastRead DESC")
    fun searchBooksByRecent(query: String, hideCompleted: Boolean): PagingSource<Int, Book>

    @Query("SELECT * FROM books WHERE title LIKE '%' || :query || '%' AND (CASE WHEN :hideCompleted = 1 THEN progress < 1.0 ELSE 1 END) ORDER BY title ASC")
    fun searchBooksByTitleAsc(query: String, hideCompleted: Boolean): PagingSource<Int, Book>

    @Query("SELECT * FROM books WHERE title LIKE '%' || :query || '%' AND (CASE WHEN :hideCompleted = 1 THEN progress < 1.0 ELSE 1 END) ORDER BY title DESC")
    fun searchBooksByTitleDesc(query: String, hideCompleted: Boolean): PagingSource<Int, Book>

    @Query("SELECT * FROM books WHERE progress >= 1.0 AND title LIKE '%' || :query || '%' ORDER BY lastRead DESC")
    fun getCompletedBooksPaged(query: String): PagingSource<Int, Book>

    @Query("SELECT * FROM books WHERE collectionName = :collectionName AND (CASE WHEN :hideCompleted = 1 THEN progress < 1.0 ELSE 1 END) ORDER BY title ASC")
    fun getBooksByCollectionPaged(collectionName: String, hideCompleted: Boolean): PagingSource<Int, Book>

    @Query("SELECT * FROM books WHERE progress > 0 AND progress < 1 ORDER BY lastRead DESC LIMIT 10")
    fun getContinueReadingBooks(): Flow<List<Book>>

    @Query("SELECT collectionName, COUNT(*) as bookCount FROM books GROUP BY collectionName")
    fun getCollectionsWithCount(): Flow<List<CollectionInfo>>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookById(id: String): Book?

    @Query("SELECT * FROM books WHERE filePath = :filePath")
    suspend fun getBookByPath(filePath: String): Book?

    @Query("SELECT filePath FROM books WHERE filePath IN (:filePaths)")
    suspend fun getExistingFilePaths(filePaths: List<String>): List<String>

    @Query("SELECT COUNT(*) FROM books")
    suspend fun getBookCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: Book)

    @Update
    suspend fun updateBook(book: Book)

    @Query("UPDATE books SET progress = :progress, lastRead = :lastRead WHERE id = :bookId")
    suspend fun updateReadingProgress(bookId: String, progress: Double, lastRead: Long)

    @Delete
    suspend fun deleteBook(book: Book)

    @Query("SELECT * FROM books")
    suspend fun getAllBooks(): List<Book>

    @Query("DELETE FROM books")
    suspend fun deleteAllBooks()
}
