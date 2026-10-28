package com.example

import com.example.data.auth.AuthOutcome
import com.example.data.model.UserProfile
import com.example.data.repository.CampusRepository
import com.example.data.repository.SeedUsers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** A campus under maintenance stops working for its own people; only the app manager decides. */
class MaintenanceRulesTest {

  private fun signInAs(repo: CampusRepository, user: UserProfile) {
    if (repo.session.value != null) repo.signOut()
    var outcome = repo.auth.signIn(user.email, SeedUsers.SAMPLE_PASSWORD)
    if (outcome is AuthOutcome.NeedCode) {
      val code = repo.auth.visibleCode.value!!
      outcome = repo.auth.submitCode(outcome.purpose, outcome.email, code)
    }
    assertTrue("sign-in should succeed for ${user.email}", outcome is AuthOutcome.Success)
  }

  private val cuom = SeedUsers.asha.universityId

  @Test
  fun `only the app manager can pause a campus`() {
    val repo = CampusRepository()
    signInAs(repo, SeedUsers.asha)
    assertFalse(repo.setCampusMaintenance(cuom, true, "x"))
    signInAs(repo, SeedUsers.mushi)
    assertFalse(repo.setCampusMaintenance(cuom, true, "x"))
    signInAs(repo, SeedUsers.emanuel)
    assertFalse(repo.setCampusMaintenance(cuom, true, "x"))
    assertNull(repo.campusMaintenance.value)
  }

  @Test
  fun `a paused campus shows its people the notice and blocks what they do`() {
    val repo = CampusRepository()
    signInAs(repo, SeedUsers.platformManager)
    assertTrue(repo.setCampusMaintenance(cuom, true, "Upgrading the database tonight"))

    signInAs(repo, SeedUsers.asha)
    val notice = repo.campusMaintenance.value
    assertNotNull(notice)
    assertEquals("Upgrading the database tonight", notice!!.note)
    assertTrue(notice.since.isNotBlank())
    // Nothing of the campus is readable while paused.
    assertTrue(repo.announcements.value.isEmpty())
    assertTrue(repo.sessions.value.isEmpty())

    signInAs(repo, SeedUsers.emanuel)
    assertNotNull(repo.campusMaintenance.value)
    assertEquals(0, repo.campusLecturers().size)

    // Another campus is untouched.
    signInAs(repo, SeedUsers.ifmStudent)
    assertNull(repo.campusMaintenance.value)
  }

  @Test
  fun `reopening a campus tells its people and everything works again`() {
    val repo = CampusRepository()
    signInAs(repo, SeedUsers.platformManager)
    repo.setCampusMaintenance(cuom, true, "Short break")
    repo.setCampusMaintenance(cuom, false)
    assertEquals("", repo.campuses.value.first { it.university.id == cuom }.university.maintenanceNote)

    signInAs(repo, SeedUsers.asha)
    assertNull(repo.campusMaintenance.value)
    assertTrue(repo.notifications.value.any { it.title == "CampusConnect is back" })
    assertTrue(repo.announcements.value.isNotEmpty())
  }

  @Test
  fun `the app manager is never paused and a platform campus cannot be switched`() {
    val repo = CampusRepository()
    signInAs(repo, SeedUsers.platformManager)
    assertFalse(repo.setCampusMaintenance(SeedUsers.PLATFORM_ID, true, "x"))
    assertFalse(repo.setCampusMaintenance("nope", true, "x"))
    repo.setCampusMaintenance(cuom, true, "x")
    assertNull(repo.campusMaintenance.value)
    assertTrue(repo.campuses.value.isNotEmpty())
  }

  @Test
  fun `campus names can be changed by the right people with a real name`() {
    val repo = CampusRepository()
    signInAs(repo, SeedUsers.asha)
    assertFalse(repo.updateCampusDetails("Hacked Name"))

    signInAs(repo, SeedUsers.emanuel)
    assertFalse(repo.updateCampusDetails("  "))
    assertFalse(repo.updateCampusDetails("ab"))
    assertTrue(repo.updateCampusDetails("Catholic University of Mbeya"))
    assertEquals("Catholic University of Mbeya", repo.catalog.universityName(cuom))
    assertFalse(repo.renameCampus(cuom, "Not Allowed Here")) // a campus administrator cannot use the manager's rename

    signInAs(repo, SeedUsers.platformManager)
    assertTrue(repo.renameCampus(cuom, "Catholic University of Mbeya (CUoM)"))
    assertFalse(repo.renameCampus(SeedUsers.PLATFORM_ID, "Renamed platform"))
  }
}
