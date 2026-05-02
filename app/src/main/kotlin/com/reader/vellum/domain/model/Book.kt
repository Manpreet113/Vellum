package com.reader.vellum.domain.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "books",
    indices = [
        Index(value = ["filePath"], unique = true),
        Index(value = ["lastRead"]),
        Index(value = ["title"]),
        Index(value = ["collectionName"]),
        Index(value = ["progress"]),
        Index(value = ["collectionName", "title"]),
        Index(value = ["progress", "lastRead"])
    ]
)
data class Book(
    @PrimaryKey val id: String,
    val title: String,
    val author: String?,
    val filePath: String,
    val uriString: String? = null,
    val coverPath: String?,
    val format: String,
    val progress: Double = 0.0,
    val totalPages: Int = 0,
    val lastRead: Long = System.currentTimeMillis(),
    val collectionName: String? = null,
    val description: String? = null
)
