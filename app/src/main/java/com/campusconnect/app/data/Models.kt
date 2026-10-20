package com.campusconnect.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    @SerialName("full_name") val fullName: String,
    val email: String? = null,
    val role: String = "student",
    val status: String = "pending",
    @SerialName("programme_id") val programmeId: String? = null,
    @SerialName("student_number") val studentNumber: String? = null,
    @SerialName("staff_number") val staffNumber: String? = null,
)

@Serializable
data class Programme(val id: String, val code: String, val name: String)

@Serializable
data class Term(
    val id: String,
    val name: String,
    @SerialName("is_current") val isCurrent: Boolean = false,
)

@Serializable
data class Course(val id: String, val code: String, val title: String)

@Serializable
data class Offering(
    val id: String,
    @SerialName("lecturer_id") val lecturerId: String? = null,
    val cohort: String = "main",
    val courses: Course? = null,
) {
    val label: String get() = (courses?.code ?: "Course") + " - " + (courses?.title ?: "")
}

@Serializable
data class ClassSession(
    val id: String,
    @SerialName("offering_id") val offeringId: String,
    @SerialName("starts_at") val startsAt: String,
    @SerialName("ends_at") val endsAt: String,
    val venue: String? = null,
    val status: String = "scheduled",
    val note: String? = null,
    @SerialName("course_offerings") val offering: Offering? = null,
)

@Serializable
data class AuthorName(@SerialName("full_name") val fullName: String)

@Serializable
data class Announcement(
    val id: String,
    @SerialName("author_id") val authorId: String,
    val audience: String,
    @SerialName("programme_id") val programmeId: String? = null,
    @SerialName("offering_id") val offeringId: String? = null,
    val title: String,
    val body: String,
    val status: String,
    @SerialName("published_at") val publishedAt: String? = null,
    @SerialName("created_at") val createdAt: String,
    val profiles: AuthorName? = null,
)

@Serializable
data class AppNotification(
    val id: String,
    val kind: String = "announcement",
    @SerialName("announcement_id") val announcementId: String? = null,
    @SerialName("session_id") val sessionId: String? = null,
    @SerialName("assignment_id") val assignmentId: String? = null,
    @SerialName("material_id") val materialId: String? = null,
    val title: String,
    val body: String? = null,
    @SerialName("read_at") val readAt: String? = null,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class Conflict(
    @SerialName("conflict_kind") val kind: String,
    @SerialName("conflict_session_id") val sessionId: String,
    @SerialName("conflict_venue") val venue: String? = null,
    @SerialName("conflict_starts_at") val startsAt: String,
    @SerialName("conflict_ends_at") val endsAt: String,
)

// ---- write payloads ----
@Serializable
data class NewAnnouncement(
    @SerialName("author_id") val authorId: String,
    val audience: String,
    @SerialName("programme_id") val programmeId: String?,
    @SerialName("offering_id") val offeringId: String?,
    val title: String,
    val body: String,
    val status: String,
)

@Serializable
data class NewSession(
    @SerialName("offering_id") val offeringId: String,
    @SerialName("starts_at") val startsAt: String,
    @SerialName("ends_at") val endsAt: String,
    val venue: String?,
    @SerialName("created_by") val createdBy: String,
)

@Serializable
data class SessionUpdate(
    @SerialName("starts_at") val startsAt: String,
    @SerialName("ends_at") val endsAt: String,
    val venue: String?,
)

@Serializable data class StatusOnly(val status: String)
@Serializable data class ReadMark(@SerialName("read_at") val readAt: String)

@Serializable
data class ProfileApproval(
    val role: String,
    val status: String,
    @SerialName("programme_id") val programmeId: String?,
)

@Serializable data class NewProgramme(val code: String, val name: String)

@Serializable
data class NewTerm(
    val name: String,
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String,
    @SerialName("is_current") val isCurrent: Boolean,
)

@Serializable
data class NewCourse(
    val code: String,
    val title: String,
    @SerialName("programme_id") val programmeId: String?,
)

@Serializable
data class NewOffering(
    @SerialName("course_id") val courseId: String,
    @SerialName("term_id") val termId: String,
    @SerialName("lecturer_id") val lecturerId: String?,
    val cohort: String,
)

@Serializable
data class NewEnrollment(
    @SerialName("offering_id") val offeringId: String,
    @SerialName("student_id") val studentId: String,
)

// ---- study materials and deadlines ----
@Serializable
data class Material(
    val id: String,
    @SerialName("offering_id") val offeringId: String,
    @SerialName("uploader_id") val uploaderId: String,
    val title: String,
    @SerialName("storage_path") val storagePath: String,
    @SerialName("size_bytes") val sizeBytes: Long? = null,
    val status: String = "approved",
    @SerialName("created_at") val createdAt: String,
    @SerialName("course_offerings") val offering: Offering? = null,
)

@Serializable
data class NewMaterial(
    @SerialName("offering_id") val offeringId: String,
    @SerialName("uploader_id") val uploaderId: String,
    val title: String,
    @SerialName("storage_path") val storagePath: String,
    @SerialName("size_bytes") val sizeBytes: Long,
)

@Serializable
data class Assignment(
    val id: String,
    @SerialName("offering_id") val offeringId: String,
    @SerialName("created_by") val createdBy: String,
    val title: String,
    val instructions: String? = null,
    @SerialName("due_at") val dueAt: String,
    val status: String = "published",
    @SerialName("course_offerings") val offering: Offering? = null,
)

@Serializable
data class NewAssignment(
    @SerialName("offering_id") val offeringId: String,
    @SerialName("created_by") val createdBy: String,
    val title: String,
    val instructions: String?,
    @SerialName("due_at") val dueAt: String,
    val status: String,
)

/** Where a tapped push notification should take the user. */
data class DeepLink(val kind: String, val id: String?)
