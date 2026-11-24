package com.ottawa.wastesorter.core.database

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/** Une identification enregistrée localement (aucun compte requis). */
@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemName: String,
    /** Matériaux séparés par des virgules (valeurs de l'énumération Material). */
    val materials: String,
    /** Destinations séparées par des virgules (valeurs de l'énumération Destination). */
    val destinations: String,
    /** Résultat complet sérialisé (ClassificationResult) pour réafficher l'écran de résultat. */
    val classificationJson: String,
    /** "AI" ou "MANUAL". */
    val method: String,
    val confidence: Double?,
    val thumbnailPath: String?,
    val createdAt: Long,
)

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE itemName LIKE '%' || :query || '%' OR materials LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun search(query: String): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE id = :id")
    suspend fun get(id: Long): HistoryEntity?

    @Insert
    suspend fun insert(entity: HistoryEntity): Long

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT thumbnailPath FROM history WHERE thumbnailPath IS NOT NULL")
    suspend fun allThumbnailPaths(): List<String>

    @Query("DELETE FROM history")
    suspend fun clear()
}

@Database(entities = [HistoryEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "waste-sorter.db").build()
    }
}
