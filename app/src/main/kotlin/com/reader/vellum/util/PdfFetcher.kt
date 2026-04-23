package com.reader.vellum.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import okio.Buffer
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Request for a specific page in a PDF file.
 */
data class PdfPageRequest(val uriString: String, val pageIndex: Int)

class PdfFetcher(
    private val context: Context,
    private val request: PdfPageRequest,
    private val options: Options
) : Fetcher {

    companion object {
        // PdfRenderer is not thread-safe across different instances opening the same PFD
        private val mutex = Mutex()
    }

    override suspend fun fetch(): FetchResult = mutex.withLock {
        val uri = Uri.parse(request.uriString)
        
        // Use cache for PDF renders too!
        val cacheKey = "pdf_${request.pageIndex}"
        val cachedData = PageCache.get(request.uriString, cacheKey)
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

        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            val renderer = PdfRenderer(pfd)
            if (request.pageIndex >= renderer.pageCount) {
                renderer.close()
                throw Exception("Page index out of bounds")
            }

            renderer.openPage(request.pageIndex).use { page ->
                // Render at high quality (e.g., 2.0x)
                val scale = 2.0
                val bitmap = Bitmap.createBitmap(
                    (page.width * scale).toInt(),
                    (page.height * scale).toInt(),
                    Bitmap.Config.ARGB_8888
                )
                // PDFs often have transparent backgrounds, fill with white for correct contrast/colors
                val canvas = android.graphics.Canvas(bitmap)
                canvas.drawColor(android.graphics.Color.WHITE)
                
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                val data = stream.toByteArray()
                bitmap.recycle()

                // Cache it
                PageCache.put(request.uriString, cacheKey, data)

                return SourceFetchResult(
                    source = ImageSource(
                        source = Buffer().apply { write(data) },
                        fileSystem = options.fileSystem
                    ),
                    mimeType = "image/jpeg",
                    dataSource = DataSource.DISK
                )
            }
        } ?: throw Exception("Failed to open PDF file descriptor")
    }

    class Factory(private val context: Context) : Fetcher.Factory<PdfPageRequest> {
        override fun create(data: PdfPageRequest, options: Options, imageLoader: ImageLoader): Fetcher {
            return PdfFetcher(context, data, options)
        }
    }
}
