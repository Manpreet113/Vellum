package com.reader.vellum.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `books_new` (
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
            INSERT OR REPLACE INTO `books_new` (
                `id`, `title`, `author`, `filePath`, `uriString`, `coverPath`,
                `format`, `progress`, `totalPages`, `lastRead`, `collectionName`, `description`
            )
            SELECT
                `filePath` AS `id`,
                `title`,
                `author`,
                `filePath`,
                `uriString`,
                `coverPath`,
                `format`,
                `progress`,
                `totalPages`,
                `lastRead`,
                `collectionName`,
                `description`
            FROM `books`
            """.trimIndent()
        )

        db.execSQL("DROP TABLE `books`")
        db.execSQL("ALTER TABLE `books_new` RENAME TO `books`")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_books_filePath` ON `books` (`filePath`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_books_lastRead` ON `books` (`lastRead`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_books_title` ON `books` (`title`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_books_collectionName` ON `books` (`collectionName`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_books_progress` ON `books` (`progress`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_books_collectionName_title` ON `books` (`collectionName`, `title`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_books_progress_lastRead` ON `books` (`progress`, `lastRead`)")
    }
}
