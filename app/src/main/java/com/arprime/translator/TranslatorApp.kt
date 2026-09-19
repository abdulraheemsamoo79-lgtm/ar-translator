package com.arprime.translator

import android.app.Application
import androidx.room.Room
import com.arprime.translator.data.AppDatabase

class TranslatorApp : Application() {

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(this, AppDatabase::class.java, "translator.db")
            .fallbackToDestructiveMigration()
            .build()
    }
}
