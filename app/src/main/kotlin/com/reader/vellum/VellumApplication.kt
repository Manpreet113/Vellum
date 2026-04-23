package com.reader.vellum

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.reader.vellum.util.CbzFetcher
import com.reader.vellum.util.CbzPageRequest
import com.reader.vellum.util.PdfFetcher
import com.reader.vellum.util.PdfPageRequest
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class VellumApplication : Application(), SingletonImageLoader.Factory {
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(CbzFetcher.Factory(this@VellumApplication))
                add(PdfFetcher.Factory(this@VellumApplication))
            }
            .build()
    }
}
