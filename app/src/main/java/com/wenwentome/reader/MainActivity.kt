package com.wenwentome.reader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.wenwentome.reader.di.AppContainer

class MainActivity : ComponentActivity() {
    private val appContainer by lazy { AppContainer(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ReaderApp(appContainer = appContainer)
        }
    }
}
