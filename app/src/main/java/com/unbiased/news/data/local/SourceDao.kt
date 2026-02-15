package com.unbiased.news.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.unbiased.news.data.model.SourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceDao {

    @Query("SELECT * FROM sources WHERE isEnabled = 1")
    fun getEnabledSources(): Flow<List<SourceEntity>>

    @Query("SELECT * FROM sources")
    fun getAllSources(): Flow<List<SourceEntity>>

    @Query("SELECT * FROM sources WHERE sourceId = :sourceId")
    suspend fun getSourceBySourceId(sourceId: String): SourceEntity?

    @Query("SELECT * FROM sources WHERE category = :category AND isEnabled = 1")
    fun getSourcesByCategory(category: String): Flow<List<SourceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSources(sources: List<SourceEntity>)

    @Query("UPDATE sources SET isEnabled = :enabled WHERE sourceId = :sourceId")
    suspend fun updateSourceEnabled(sourceId: String, enabled: Boolean)

    @Query("DELETE FROM sources")
    suspend fun deleteAllSources()

    @Query("SELECT COUNT(*) FROM sources")
    suspend fun getSourceCount(): Int
}
