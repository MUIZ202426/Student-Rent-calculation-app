package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.Expense
import com.example.data.model.ExpenseSplit
import com.example.data.model.Group
import com.example.data.model.GroupMember
import com.example.data.model.Profile
import com.example.data.model.Settlement

@Database(
    entities = [
        Profile::class,
        Group::class,
        GroupMember::class,
        Expense::class,
        ExpenseSplit::class,
        Settlement::class
    ],
    version = 1,
    exportSchema = false
)
abstract class StudySplitDatabase : RoomDatabase() {
    abstract fun dao(): StudySplitDao

    companion object {
        @Volatile
        private var INSTANCE: StudySplitDatabase? = null

        fun getDatabase(context: Context): StudySplitDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StudySplitDatabase::class.java,
                    "studysplit_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
