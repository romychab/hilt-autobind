package com.uandcode.hilt.autobind.app.db

import android.content.Context
import androidx.room.Room
import com.uandcode.hilt.autobind.annotations.factories.AutoScoped
import com.uandcode.hilt.autobind.annotations.factories.DelegateBindingFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

class AppDatabaseFactory @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : DelegateBindingFactory<AppDatabase> {

    @AutoScoped
    override fun provideDelegate(): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "app-database",
        ).build()
    }

}
