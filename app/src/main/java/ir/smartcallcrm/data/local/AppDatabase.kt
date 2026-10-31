package ir.smartcallcrm.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ir.smartcallcrm.data.local.dao.CallDao
import ir.smartcallcrm.data.local.dao.CustomerDao
import ir.smartcallcrm.data.local.dao.FollowUpDao
import ir.smartcallcrm.data.local.entity.CallEntity
import ir.smartcallcrm.data.local.entity.CustomerEntity
import ir.smartcallcrm.data.local.entity.FollowUpEntity

@Database(
    entities = [CallEntity::class, CustomerEntity::class, FollowUpEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun callDao(): CallDao
    abstract fun customerDao(): CustomerDao
    abstract fun followUpDao(): FollowUpDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smartcall.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
