package com.reader.vellum.di

import android.content.Context
import com.reader.vellum.util.BookParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideBookParser(@ApplicationContext context: Context): BookParser {
        return BookParser(context)
    }
}
