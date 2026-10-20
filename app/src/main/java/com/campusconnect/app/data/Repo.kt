package com.campusconnect.app.data

import com.campusconnect.app.EAT
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.storage.storage
import java.util.UUID
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.LocalDate

class Repo(private val client: SupabaseClient) {

    private val sessionCols =
        "id,offering_id,starts_at,ends_at,venue,status,note,course_offerings(id,lecturer_id,cohort,courses(id,code,title))"
    private val announcementCols =
        "id,author_id,audience,programme_id,offering_id,title,body,status,published_at,created_at,profiles(full_name)"

    val myId: String get() = client.auth.currentUserOrNull()?.id ?: error("Not signed in")

    // ---------- auth ----------
    suspend fun signIn(mail: String, pass: String) {
        client.auth.signInWith(Email) { email = mail; password = pass }
    }

    suspend fun signUp(name: String, mail: String, pass: String) {
        client.auth.signUpWith(Email) {
            email = mail
            password = pass
            data = buildJsonObject { put("full_name", name) }
        }
    }

    suspend fun signOut() = client.auth.signOut()

    // ---------- reads ----------
    suspend fun myProfile(): List<Profile> =
        client.from("profiles").select { filter { eq("id", myId) } }.decodeList<Profile>()

    suspend fun sessions(): List<ClassSession> {
        val from = LocalDate.now(EAT).minusDays(1).atStartOfDay(EAT).toInstant().toString()
        return client.from("class_sessions").select(Columns.raw(sessionCols)) {
            filter { gte("starts_at", from) }
            order("starts_at", Order.ASCENDING)
            limit(400)
        }.decodeList<ClassSession>()
    }

    suspend fun announcements(): List<Announcement> =
        client.from("announcements").select(Columns.raw(announcementCols)) {
            order("created_at", Order.DESCENDING)
            limit(100)
        }.decodeList<Announcement>()

    suspend fun notifications(): List<AppNotification> =
        client.from("notifications").select {
            order("created_at", Order.DESCENDING)
            limit(100)
        }.decodeList<AppNotification>()

    suspend fun unreadNotifications(): List<AppNotification> =
        client.from("notifications").select {
            filter { filter("read_at", FilterOperator.IS, "null") }
            order("created_at", Order.DESCENDING)
            limit(20)
        }.decodeList<AppNotification>()

    suspend fun offerings(): List<Offering> =
        client.from("course_offerings").select(Columns.raw("id,lecturer_id,cohort,courses(id,code,title)"))
            .decodeList<Offering>().sortedBy { it.label }

    suspend fun programmes(): List<Programme> =
        client.from("programmes").select { order("code", Order.ASCENDING) }.decodeList<Programme>()

    suspend fun terms(): List<Term> =
        client.from("academic_terms").select { order("start_date", Order.DESCENDING) }.decodeList<Term>()

    suspend fun courses(): List<Course> =
        client.from("courses").select { order("code", Order.ASCENDING) }.decodeList<Course>()

    suspend fun pendingProfiles(): List<Profile> =
        client.from("profiles").select {
            filter { eq("status", "pending") }
            order("created_at", Order.ASCENDING)
        }.decodeList<Profile>()

    suspend fun activeProfilesWithRole(role: String): List<Profile> =
        client.from("profiles").select {
            filter { eq("role", role); eq("status", "active") }
            order("full_name", Order.ASCENDING)
        }.decodeList<Profile>()

    suspend fun conflicts(offeringId: String, starts: Instant, ends: Instant, venue: String?, exclude: String?): List<Conflict> =
        client.postgrest.rpc(
            "session_conflicts",
            buildJsonObject {
                put("p_offering", offeringId)
                put("p_starts", starts.toString())
                put("p_ends", ends.toString())
                put("p_venue", venue)
                put("p_exclude", exclude)
            },
        ).decodeList<Conflict>()

    // ---------- notifications ----------
    suspend fun markRead(id: String) {
        client.from("notifications").update(ReadMark(Instant.now().toString())) { filter { eq("id", id) } }
    }

    suspend fun markAllRead() {
        client.from("notifications").update(ReadMark(Instant.now().toString())) {
            filter { filter("read_at", FilterOperator.IS, "null") }
        }
    }

    // ---------- announcements ----------
    suspend fun createAnnouncement(a: NewAnnouncement) { client.from("announcements").insert(a) }

    suspend fun setAnnouncementStatus(id: String, status: String) {
        client.from("announcements").update(StatusOnly(status)) { filter { eq("id", id) } }
    }

    // ---------- timetable ----------
    suspend fun createSession(s: NewSession) { client.from("class_sessions").insert(s) }

    suspend fun updateSession(id: String, u: SessionUpdate) {
        client.from("class_sessions").update(u) { filter { eq("id", id) } }
    }

    suspend fun setSessionStatus(id: String, status: String) {
        client.from("class_sessions").update(StatusOnly(status)) { filter { eq("id", id) } }
    }

    // ---------- admin ----------
    suspend fun approve(id: String, role: String, programmeId: String?) {
        client.from("profiles").update(ProfileApproval(role, "active", programmeId)) { filter { eq("id", id) } }
    }

    suspend fun setProfileStatus(id: String, status: String) {
        client.from("profiles").update(StatusOnly(status)) { filter { eq("id", id) } }
    }

    suspend fun createProgramme(p: NewProgramme) { client.from("programmes").insert(p) }
    suspend fun createTerm(t: NewTerm) { client.from("academic_terms").insert(t) }
    suspend fun createCourse(c: NewCourse) { client.from("courses").insert(c) }
    suspend fun createOffering(o: NewOffering) { client.from("course_offerings").insert(o) }
    suspend fun enroll(e: NewEnrollment) { client.from("enrollments").insert(e) }

    // ---------- study materials ----------
    private val materialCols =
        "id,offering_id,uploader_id,title,storage_path,size_bytes,status,created_at,course_offerings(id,lecturer_id,cohort,courses(id,code,title))"

    suspend fun materials(): List<Material> =
        client.from("materials").select(Columns.raw(materialCols)) {
            order("created_at", Order.DESCENDING)
            limit(300)
        }.decodeList<Material>()

    /** Uploads the PDF to the private bucket, then records it. Rolls the file back if the record fails. */
    suspend fun uploadMaterial(offeringId: String, title: String, bytes: ByteArray) {
        val path = "$offeringId/${UUID.randomUUID()}.pdf"
        val bucket = client.storage.from("materials")
        bucket.upload(path, bytes)
        try {
            client.from("materials").insert(NewMaterial(offeringId, myId, title, path, bytes.size.toLong()))
        } catch (e: Exception) {
            try { bucket.delete(path) } catch (_: Exception) {}
            throw e
        }
    }

    suspend fun downloadMaterial(path: String): ByteArray =
        client.storage.from("materials").downloadAuthenticated(path)

    suspend fun deleteMaterial(id: String, path: String) {
        client.from("materials").delete { filter { eq("id", id) } }
        try { client.storage.from("materials").delete(path) } catch (_: Exception) {}
    }

    // ---------- deadlines ----------
    suspend fun assignments(): List<Assignment> {
        val from = Instant.now().minusSeconds(30L * 24 * 3600).toString()
        return client.from("assignments").select(
            Columns.raw("id,offering_id,created_by,title,instructions,due_at,status,course_offerings(id,lecturer_id,cohort,courses(id,code,title))"),
        ) {
            filter { gte("due_at", from) }
            order("due_at", Order.ASCENDING)
            limit(300)
        }.decodeList<Assignment>()
    }

    suspend fun createAssignment(a: NewAssignment) { client.from("assignments").insert(a) }

    suspend fun deleteAssignment(id: String) {
        client.from("assignments").delete { filter { eq("id", id) } }
    }

    // ---------- push tokens ----------
    suspend fun registerToken(token: String) {
        client.postgrest.rpc("register_device_token", buildJsonObject { put("p_token", token) })
    }

    suspend fun unregisterToken(token: String) {
        client.postgrest.rpc("unregister_device_token", buildJsonObject { put("p_token", token) })
    }
}
