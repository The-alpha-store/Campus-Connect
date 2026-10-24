package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProcessedDocumentDaoTest {
  private lateinit var db: AppDatabase
  private lateinit var dao: ProcessedDocumentDao

  @Before fun createDatabase() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    dao = db.processedDocumentDao()
  }

  @After fun closeDatabase() { db.close() }

  private fun document(id: String, course: String = "CS210", title: String = "Algorithm analysis") =
    ProcessedDocumentEntity(id, title, "$id.pdf", 1024, "application/pdf", course, "Computer Science", "Test University", "Test Lecturer", 1000L, "Big O and algorithm complexity", "Big-O, Memoization", "Explain complexity", "Practise examples", 250, 2, 2, "COMPLETED")

  @Test fun schemaCreatesAndStartsEmpty() = runBlocking { assertEquals(0, dao.getAllDocuments().first().size) }
  @Test fun insertAndGetById() = runBlocking { dao.insertDocument(document("one")); assertNotNull(dao.getDocumentById("one")) }
  @Test fun flowReflectsInsertedRows() = runBlocking { dao.insertDocument(document("one")); assertEquals(1, dao.getAllDocuments().first().size) }
  @Test fun courseFilterReturnsOnlyMatchingRows() = runBlocking { dao.insertDocuments(listOf(document("one", "CS210"), document("two", "IS201"))); assertEquals(listOf("one"), dao.getDocumentsByCourse("CS210").first().map { it.id }) }
  @Test fun keywordSearchFindsSummaryTopicsAndCode() = runBlocking { dao.insertDocuments(listOf(document("one"), document("two", "IS201", "Normalization"))); assertEquals("one", dao.searchDocuments("Memoization").first().single().id); assertEquals("two", dao.searchDocuments("Normalization").first().single().id); assertEquals(1, dao.searchDocuments("CS210").first().size) }
  @Test fun updateChangesStoredDocument() = runBlocking { dao.insertDocument(document("one")); dao.updateDocument(document("one", title = "Updated title")); assertEquals("Updated title", dao.getDocumentById("one")?.title) }
  @Test fun deleteByIdRemovesOnlyTarget() = runBlocking { dao.insertDocuments(listOf(document("one"), document("two"))); dao.deleteDocumentById("one"); assertNull(dao.getDocumentById("one")); assertEquals(1, dao.getAllDocuments().first().size) }
  @Test fun deleteAllRemovesEveryDocument() = runBlocking { dao.insertDocuments(listOf(document("one"), document("two"))); dao.deleteAllDocuments(); assertEquals(0, dao.getAllDocuments().first().size) }
}
