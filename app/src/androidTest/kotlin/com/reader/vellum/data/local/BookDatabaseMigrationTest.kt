package com.reader.vellum.data.local

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookDatabaseMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val dbName = "migration-test.db"

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun migrate2To3_rekeysBooksToStableUriAndPreservesProgress() {
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()

        SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { db ->
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `books` (
                    `id` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `author` TEXT,
                    `filePath` TEXT NOT NULL,
                    `uriString` TEXT,
                    `coverPath` TEXT,
                    `format` TEXT NOT NULL,
                    `progress` REAL NOT NULL,
                    `totalPages` INTEGER NOT NULL,
                    `lastRead` INTEGER NOT NULL,
                    `collectionName` TEXT,
                    `description` TEXT,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `books` (
                    `id`, `title`, `author`, `filePath`, `uriString`, `coverPath`,
                    `format`, `progress`, `totalPages`, `lastRead`, `collectionName`, `description`
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any?>(
                    "legacyHash",
                    "Sample Book",
                    null,
                    "content://books/1",
                    "content://books/1",
                    null,
                    "epub",
                    0.42,
                    12,
                    123456789L,
                    "Shelf",
                    "Desc"
                )
            )
            db.version = 2
        }

        val migrated = Room.databaseBuilder(context, BookDatabase::class.java, dbName)
            .addMigrations(MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()

        try {
            val book = runBlocking { migrated.bookDao().getBookById("content://books/1") }
            assertNotNull(book)
            requireNotNull(book)
            assertEquals("content://books/1", book.id)
            assertEquals("content://books/1", book.filePath)
            assertEquals(0.42, book.progress, 0.0)
            assertEquals("Sample Book", book.title)
        } finally {
            migrated.close()
        }
    }
}
