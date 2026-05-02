package com.reader.vellum.util

import android.content.Context
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import okio.Buffer
import java.util.zip.ZipFile

data class CbzPageRequest(val uriString: String, val entryName: String)

class CbzFetcher(
    private val context: Context,
    private val request: CbzPageRequest,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val cachedData = PageCache.get(request.uriString, request.entryName)
        if (cachedData != null) {
            return SourceFetchResult(
                source = ImageSource(
                    source = Buffer().apply { write(cachedData) },
                    fileSystem = options.fileSystem
                ),
                mimeType = null,
                dataSource = DataSource.MEMORY
            )
        }

        val localArchive = ReaderArchiveCache.ensureLocalArchive(context, request.uriString)
        ZipFile(localArchive).use { zipFile ->
            val entry = zipFile.getEntry(request.entryName)
                ?: throw Exception("Entry ${request.entryName} not found in ${request.uriString}")
            val data = zipFile.getInputStream(entry).use { it.readBytes() }
            PageCache.put(request.uriString, request.entryName, data)

            return SourceFetchResult(
                source = ImageSource(
                    source = Buffer().apply { write(data) },
                    fileSystem = options.fileSystem
                ),
                mimeType = null,
                dataSource = DataSource.DISK
            )
        }
    }

    class Factory(private val context: Context) : Fetcher.Factory<CbzPageRequest> {
        override fun create(data: CbzPageRequest, options: Options, imageLoader: ImageLoader): Fetcher {
            return CbzFetcher(context, data, options)
        }
    }
}
