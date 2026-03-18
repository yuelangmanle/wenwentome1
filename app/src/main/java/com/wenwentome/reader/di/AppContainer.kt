package com.wenwentome.reader.di

import android.app.Application
import android.content.Context

class AppContainer(private val application: Application) {
    val appContext: Context = application
}
