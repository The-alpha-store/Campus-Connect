package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProcessedDocumentDao {
  @Query("SELECT * FROM processed_documents ORDER BY timestamp DESC")
  fun getAllDocuments(): Flow<List<ProcessedDocumentEntity>>

  @Query("SELECT * FROM processed_documents WHERE id = :id LIMIT 1")
  suspend fun getDocumentById(id: String): ProcessedDocumentEntity?

  @Query("SELECT * FROM processed_documents WHERE courseCode = :courseCode ORDER BY timestamp DESC")
  fun getDocumentsByCourse(courseCode: String): Flow<List<ProcessedDocumentEntity>>

  @Query("""
    SELECT * FROM processed_documents
    WHERE title LIKE '%' || :query || '%'
       OR fileName LIKE '%' || :query || '%'
       OR extractedSummary LIKE '%' || :query || '%'
       OR keyTopics LIKE '%' || :query || '%'
       OR courseCode LIKE '%' || :query || '%'
       OR courseTitle LIKE '%' || :query || '%'
       OR category LIKE '%' || :query || '%'
       OR tags LIKE '%' || :query || '%'
    ORDER BY timestamp DESC
  """)
  fun searchDocuments(query: String): Flow<List<ProcessedDocumentEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertDocument(document: ProcessedDocumentEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertDocuments(documents: List<ProcessedDocumentEntity>)

  @Update
  suspend fun updateDocument(document: ProcessedDocumentEntity)

  @Query("DELETE FROM processed_documents WHERE id = :id")
  suspend fun deleteDocumentById(id: String)

  @Query("DELETE FROM processed_documents")
  suspend fun deleteAllDocuments()
}
