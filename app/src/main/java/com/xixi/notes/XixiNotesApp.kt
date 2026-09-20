package com.xixi.notes

import android.app.Application
import com.xixi.notes.di.AppContainer

class XixiNotesApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(applicationContext)
    }
}
