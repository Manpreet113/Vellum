package com.reader.vellum.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.reader.vellum.domain.model.Book

@Database(entities = [Book::class], version = 3, exportSchema = true)
abstract class BookDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
}
