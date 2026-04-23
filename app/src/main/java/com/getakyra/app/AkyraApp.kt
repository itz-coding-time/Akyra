package com.getakyra.app

import android.app.Application
import com.getakyra.app.data.ShiftRepository
import com.getakyra.app.data.AkyraDatabase

class AkyraApp : Application() {

    val database by lazy { AkyraDatabase.getDatabase(this) }
    val repository by lazy { ShiftRepository.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
    }
}