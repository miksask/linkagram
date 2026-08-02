package io.github.miksask.linkagram.data.history

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        HistoryEntryEntity::class,
        HistoryRedirectEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class LinkagramDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        const val NAME = "linkagram.db"

        fun create(context: Context): LinkagramDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                LinkagramDatabase::class.java,
                NAME,
            ).build()
    }
}
