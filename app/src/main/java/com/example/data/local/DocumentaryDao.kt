package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentaryDao {
  @Query("SELECT * FROM documentary_projects ORDER BY updatedAtMillis DESC")
  fun observeAllProjects(): Flow<List<DocumentaryEntity>>

  @Query("SELECT * FROM documentary_projects WHERE id = :id LIMIT 1")
  suspend fun getProjectById(id: String): DocumentaryEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertProject(entity: DocumentaryEntity)

  @Query("DELETE FROM documentary_projects WHERE id = :id")
  suspend fun deleteProjectById(id: String)
}
