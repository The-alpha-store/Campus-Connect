package com.campusconnect.app.ui

import com.campusconnect.app.trf

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.campusconnect.app.EAT
import com.campusconnect.app.data.*
import com.campusconnect.app.friendlyError
import com.campusconnect.app.tr
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import java.time.LocalDateTime

/** Creates a ViewModel scoped to the signed-in user, so a new login never sees old state. */
@Composable
inline fun <reified T : ViewModel> vm(key: String, crossinline create: () -> T): T =
    viewModel(key = key + ":" + T::class.java.simpleName, factory = viewModelFactory { initializer<T> { create() } })

abstract class BaseVm : ViewModel() {
    val busy = MutableStateFlow(false)
    val message = MutableStateFlow<String?>(null)

    protected fun act(
        okMsg: String? = null,
        onOk: () -> Unit = {},
        onErr: ((String) -> Unit)? = null,
        block: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            busy.value = true
            try {
                block()
                okMsg?.let { message.value = tr(it) }
                onOk()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val m = friendlyError(e)
                if (onErr != null) onErr(m) else message.value = m
            } finally {
                busy.value = false
            }
        }
    }

    fun clearMessage() { message.value = null }
}

class AuthVm(private val c: AppContainer) : BaseVm() {
    fun signIn(email: String, pass: String) = act { c.repo.signIn(email.trim(), pass) }

    fun signUp(name: String, email: String, pass: String, onOk: () -> Unit) =
        act(okMsg = "Account created. If asked, confirm your email, then wait for approval.", onOk = onOk) {
            c.repo.signUp(name.trim(), email.trim(), pass)
        }
}

class ProfileVm(private val c: AppContainer) : BaseVm() {
    val list = CachedList(viewModelScope, c.cache, "${c.userId}_profile", ListSerializer(Profile.serializer())) {
        c.repo.myProfile()
    }
}

class SessionsVm(private val c: AppContainer) : BaseVm() {
    val list = CachedList(viewModelScope, c.cache, "${c.userId}_sessions", ListSerializer(ClassSession.serializer())) {
        c.repo.sessions()
    }

    fun save(
        existingId: String?,
        offeringId: String,
        venue: String,
        start: LocalDateTime,
        end: LocalDateTime,
        force: Boolean,
        onConflicts: (List<Conflict>) -> Unit,
        onError: (String) -> Unit,
        onDone: () -> Unit,
    ) = act(onErr = onError) {
        val s = start.atZone(EAT).toInstant()
        val e = end.atZone(EAT).toInstant()
        if (!e.isAfter(s)) throw IllegalArgumentException(tr("End time must be after start time."))
        val v = venue.trim().ifBlank { null }
        if (!force) {
            val cs = c.repo.conflicts(offeringId, s, e, v, existingId)
            if (cs.isNotEmpty()) { onConflicts(cs); return@act }
        }
        if (existingId == null) {
            c.repo.createSession(NewSession(offeringId, s.toString(), e.toString(), v, c.repo.myId))
        } else {
            c.repo.updateSession(existingId, SessionUpdate(s.toString(), e.toString(), v))
        }
        list.refresh()
        onDone()
    }

    fun cancel(id: String) = act("Class cancelled. Students were notified.") {
        c.repo.setSessionStatus(id, "cancelled"); list.refresh()
    }

    fun restore(id: String) = act("Class restored.") {
        c.repo.setSessionStatus(id, "scheduled"); list.refresh()
    }
}

class AnnouncementsVm(private val c: AppContainer) : BaseVm() {
    val list = CachedList(viewModelScope, c.cache, "${c.userId}_announcements", ListSerializer(Announcement.serializer())) {
        c.repo.announcements()
    }

    fun publish(a: NewAnnouncement, onDone: () -> Unit) =
        act(if (a.status == "published") "Announcement published." else "Draft saved.", onDone) {
            c.repo.createAnnouncement(a); list.refresh()
        }

    fun publishDraft(id: String) = act("Announcement published.") {
        c.repo.setAnnouncementStatus(id, "published"); list.refresh()
    }
}

class NotificationsVm(private val c: AppContainer) : BaseVm() {
    val list = CachedList(viewModelScope, c.cache, "${c.userId}_notifications", ListSerializer(AppNotification.serializer())) {
        c.repo.notifications()
    }

    fun markRead(id: String) = viewModelScope.launch {
        try { c.repo.markRead(id); list.refresh() } catch (_: Exception) {}
    }

    fun markAllRead() = act { c.repo.markAllRead(); list.refresh() }
}

/** Pick-lists shared by the editors (course offerings, programmes and, for admins, people). */
class LookupsVm(private val c: AppContainer) : BaseVm() {
    val offerings = CachedList(viewModelScope, c.cache, "${c.userId}_offerings", ListSerializer(Offering.serializer())) {
        c.repo.offerings()
    }
    val programmes = CachedList(viewModelScope, c.cache, "${c.userId}_programmes", ListSerializer(Programme.serializer())) {
        c.repo.programmes()
    }
    val courses by lazy {
        CachedList(viewModelScope, c.cache, "${c.userId}_courses", ListSerializer(Course.serializer())) { c.repo.courses() }
    }
    val terms by lazy {
        CachedList(viewModelScope, c.cache, "${c.userId}_terms", ListSerializer(Term.serializer())) { c.repo.terms() }
    }
    val lecturers by lazy {
        CachedList(viewModelScope, c.cache, "${c.userId}_lecturers", ListSerializer(Profile.serializer())) {
            c.repo.activeProfilesWithRole("lecturer")
        }
    }
    val students by lazy {
        CachedList(viewModelScope, c.cache, "${c.userId}_students", ListSerializer(Profile.serializer())) {
            c.repo.activeProfilesWithRole("student")
        }
    }
}

class AdminVm(private val c: AppContainer, private val lookups: LookupsVm) : BaseVm() {
    val pending = CachedList(viewModelScope, c.cache, "${c.userId}_pending", ListSerializer(Profile.serializer())) {
        c.repo.pendingProfiles()
    }

    fun approve(p: Profile, role: String, programmeId: String?) = act(trf("%s approved as %s.", p.fullName, roleLabel(role))) {
        c.repo.approve(p.id, role, programmeId); pending.refresh(); lookups.students.refresh(); lookups.lecturers.refresh()
    }

    fun reject(p: Profile) = act("Account rejected.") {
        c.repo.setProfileStatus(p.id, "suspended"); pending.refresh()
    }

    fun createProgramme(code: String, name: String, done: () -> Unit) = act("Programme created.", done) {
        c.repo.createProgramme(NewProgramme(code.trim().uppercase(), name.trim())); lookups.programmes.refresh()
    }

    fun createTerm(name: String, start: String, end: String, current: Boolean, done: () -> Unit) =
        act("Term created.", done) {
            c.repo.createTerm(NewTerm(name.trim(), start, end, current)); lookups.terms.refresh()
        }

    fun createCourse(code: String, title: String, programmeId: String?, done: () -> Unit) =
        act("Course created.", done) {
            c.repo.createCourse(NewCourse(code.trim().uppercase(), title.trim(), programmeId)); lookups.courses.refresh()
        }

    fun createOffering(courseId: String, termId: String, lecturerId: String?, cohort: String, done: () -> Unit) =
        act("Class offering created.", done) {
            c.repo.createOffering(NewOffering(courseId, termId, lecturerId, cohort.trim().ifBlank { "main" }))
            lookups.offerings.refresh()
        }

    fun enroll(offeringId: String, studentId: String, done: () -> Unit) = act("Student enrolled.", done) {
        c.repo.enroll(NewEnrollment(offeringId, studentId))
    }
}


private const val MAX_PDF_BYTES = 10 * 1024 * 1024

private fun readCapped(input: InputStream, max: Int): ByteArray {
    val out = ByteArrayOutputStream()
    val buf = ByteArray(16 * 1024)
    var total = 0
    while (true) {
        val n = input.read(buf)
        if (n < 0) break
        total += n
        if (total > max) throw IllegalArgumentException(tr("PDF is larger than 10 MB."))
        out.write(buf, 0, n)
    }
    return out.toByteArray()
}

class MaterialsVm(private val c: AppContainer) : BaseVm() {
    val list = CachedList(viewModelScope, c.cache, "${c.userId}_materials", ListSerializer(Material.serializer())) {
        c.repo.materials()
    }
    val downloading = MutableStateFlow<String?>(null)
    val savedTick = MutableStateFlow(0)

    fun localFile(m: Material): File = File(File(c.appContext.filesDir, "materials").apply { mkdirs() }, "${m.id}.pdf")
    fun isSaved(m: Material) = localFile(m).exists()

    /** Opens from the offline copy if present, otherwise downloads it first (then it works offline). */
    fun open(m: Material, onReady: () -> Unit) {
        val file = localFile(m)
        if (file.exists()) { onReady(); return }
        act {
            downloading.value = m.id
            try {
                val bytes = c.repo.downloadMaterial(m.storagePath)
                withContext(Dispatchers.IO) { file.writeBytes(bytes) }
            } finally {
                downloading.value = null
                savedTick.value++
            }
            onReady()
        }
    }

    fun removeOfflineCopy(m: Material) {
        localFile(m).delete(); savedTick.value++
    }

    fun upload(offeringId: String, title: String, uri: Uri, onDone: () -> Unit) =
        act(tr("Material uploaded."), onDone) {
            val bytes = withContext(Dispatchers.IO) {
                c.appContext.contentResolver.openInputStream(uri)?.use { readCapped(it, MAX_PDF_BYTES) }
                    ?: throw IllegalArgumentException(tr("Could not read that file."))
            }
            if (bytes.size < 5 || String(bytes, 0, 4, Charsets.ISO_8859_1) != "%PDF") {
                throw IllegalArgumentException(tr("That file is not a PDF."))
            }
            c.repo.uploadMaterial(offeringId, title.trim(), bytes)
            list.refresh()
        }

    fun delete(m: Material) = act(tr("Material deleted.")) {
        c.repo.deleteMaterial(m.id, m.storagePath)
        localFile(m).delete(); savedTick.value++
        list.refresh()
    }
}

class AssignmentsVm(private val c: AppContainer) : BaseVm() {
    val list = CachedList(viewModelScope, c.cache, "${c.userId}_assignments", ListSerializer(Assignment.serializer())) {
        c.repo.assignments()
    }

    fun create(offeringId: String, title: String, instructions: String, due: LocalDateTime, onDone: () -> Unit) =
        act(tr("Deadline added. Students were notified."), onDone) {
            c.repo.createAssignment(
                NewAssignment(
                    offeringId, c.repo.myId, title.trim(), instructions.trim().ifBlank { null },
                    due.atZone(EAT).toInstant().toString(), "published",
                ),
            )
            list.refresh()
        }

    fun delete(id: String) = act(tr("Deadline deleted.")) {
        c.repo.deleteAssignment(id); list.refresh()
    }
}
