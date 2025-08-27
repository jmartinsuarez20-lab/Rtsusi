package com.ritsu.ai_assistant

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "memories")
data class Memory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "fact") val fact: String
)

@Dao
interface MemoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(memory: Memory)

    @Query("SELECT * FROM memories ORDER BY id DESC")
    fun getAllMemories(): Flow<List<Memory>>

    @Query("SELECT * FROM memories ORDER BY id DESC")
    suspend fun getAllMemoriesList(): List<Memory>
}

@Database(entities = [Memory::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
}
