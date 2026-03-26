package com.uandcode.hilt.autobind.app.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.uandcode.hilt.autobind.AutoBinds

@Database(entities = [NoteEntity::class], version = 1, exportSchema = false)
@AutoBinds(factory = AppDatabaseFactory::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}
