package com.talhastudio.callerid.data.local

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "calls")
data class CallRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val toNumber: String,
    val callerId: String,
    val startedAt: Long,
    val durationSec: Int,
    val status: String,
    val testMode: Boolean
)

@Dao
interface CallDao {
    @Query("SELECT * FROM calls WHERE toNumber LIKE '%' || :q || '%' ORDER BY startedAt DESC")
    fun search(q: String): Flow<List<CallRecord>>

    @Query("SELECT * FROM calls ORDER BY startedAt DESC LIMIT 3")
    fun recent(): Flow<List<CallRecord>>

    @Insert
    suspend fun insert(r: CallRecord): Long

    @Query("DELETE FROM calls")
    suspend fun clear()
}

@Database(entities = [CallRecord::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun calls(): CallDao

    companion object {
        @Volatile private var instance: AppDatabase? = null
        fun get(ctx: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(ctx.applicationContext, AppDatabase::class.java, "talha.db")
                .build().also { instance = it }
        }
    }
}
