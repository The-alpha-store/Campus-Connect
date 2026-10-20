package com.campusconnect.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.campusconnect.app.push.Push
import com.campusconnect.app.tr
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.campusconnect.app.data.AppContainer
import com.campusconnect.app.data.Profile
import com.campusconnect.app.sync.SyncWorker
import com.campusconnect.app.ui.auth.AuthFlow
import com.campusconnect.app.ui.auth.BlockedScreen
import com.campusconnect.app.ui.auth.PendingScreen
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.launch

@Composable
fun AppRoot(c: AppContainer) {
    val status by c.client.auth.sessionStatus.collectAsState()
    val hasUser = c.client.auth.currentUserOrNull() != null
    when {
        status is SessionStatus.Authenticated || (status !is SessionStatus.NotAuthenticated && hasUser) -> SignedIn(c)
        status is SessionStatus.NotAuthenticated -> AuthFlow(c)
        else -> Splash()
    }
}

@Composable
fun Splash() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
private fun SignedIn(c: AppContainer) {
    val uid = c.userId
    if (uid == null) { Splash(); return }
    val scope = rememberCoroutineScope()
    val profileVm = vm(uid) { ProfileVm(c) }
    val state by profileVm.list.state.collectAsState()
    val me = state.items.firstOrNull()
    val signOut: () -> Unit = { scope.launch { try { c.signOut() } catch (_: Exception) {} } }

    when {
        me == null && state.loading -> Splash()
        me == null -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(state.error ?: tr("Could not load your account."), modifier = Modifier.padding(24.dp))
            Button(onClick = { profileVm.list.refresh() }) { Text(tr("Try again")) }
            TextButton(onClick = signOut) { Text(tr("Sign out")) }
        }
        me.status == "pending" -> PendingScreen(me, state.loading, { profileVm.list.refresh() }, signOut)
        me.status != "active" -> BlockedScreen(signOut)
        else -> MainShell(c, me, onRefreshProfile = { profileVm.list.refresh() }, onSignOut = signOut)
    }
}

private data class NavTab(val route: String, val label: String, val icon: ImageVector)

@Composable
private fun NotificationSetup() {
    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        SyncWorker.schedule(ctx)   // fallback for phones without Google Play Services
    }
}

@Composable
private fun MainShell(c: AppContainer, me: Profile, onRefreshProfile: () -> Unit, onSignOut: () -> Unit) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val isStaff = me.role != "student"
    val isAdmin = me.role == "admin"

    val sessionsVm = vm(me.id) { SessionsVm(c) }
    val annVm = vm(me.id) { AnnouncementsVm(c) }
    val notifVm = vm(me.id) { NotificationsVm(c) }
    val lookups = vm(me.id) { LookupsVm(c) }
    val materialsVm = vm(me.id) { MaterialsVm(c) }
    val assignmentsVm = vm(me.id) { AssignmentsVm(c) }
    val adminVm = if (isAdmin) vm(me.id) { AdminVm(c, lookups) } else null

    MessageEffect(sessionsVm, snackbar)
    MessageEffect(annVm, snackbar)
    MessageEffect(notifVm, snackbar)
    MessageEffect(materialsVm, snackbar)
    MessageEffect(assignmentsVm, snackbar)
    if (adminVm != null) MessageEffect(adminVm, snackbar)
    NotificationSetup()

    // Register this phone for push notifications (no-op if Firebase is not configured).
    LaunchedEffect(me.id) { try { Push.register(c) } catch (_: Exception) {} }

    val sessions by sessionsVm.list.state.collectAsState()
    val announcements by annVm.list.state.collectAsState()
    val notifications by notifVm.list.state.collectAsState()
    val offerings by lookups.offerings.state.collectAsState()
    val programmes by lookups.programmes.state.collectAsState()
    val materials by materialsVm.list.state.collectAsState()
    val assignments by assignmentsVm.list.state.collectAsState()
    val lang by c.settings.lang.collectAsState()
    val pending = adminVm?.pending?.state?.collectAsState()?.value?.items?.size ?: 0
    val unread = notifications.items.count { it.readAt == null }

    val tabs = listOf(
        NavTab("home", "Home", Icons.Default.Home),
        NavTab("timetable", "Timetable", Icons.Default.CalendarMonth),
        NavTab("materials", "Materials", Icons.Default.MenuBook),
        NavTab("calendar", "Calendar", Icons.Default.EventNote),
        NavTab("profile", "Profile", Icons.Default.Person),
    )
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    val showBar = tabs.any { route?.startsWith(it.route) == true }

    fun goTab(r: String) = nav.navigate(r) {
        popUpTo("home") { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    // Tapping a push or system notification opens the matching screen.
    val link by c.pendingLink.collectAsState()
    LaunchedEffect(link) {
        link?.let {
            when (it.kind) {
                "announcement" -> if (it.id != null) nav.navigate("notice/${it.id}") else nav.navigate("notices")
                "timetable" -> goTab("timetable")
                "deadline" -> goTab("calendar")
                "material" -> goTab("materials")
                else -> nav.navigate("notifications")
            }
            c.pendingLink.value = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (showBar) NavigationBar {
                tabs.forEach { t ->
                    NavigationBarItem(
                        selected = route?.startsWith(t.route) == true,
                        onClick = { goTab(t.route) },
                        icon = {
                            if (t.route == "profile" && pending > 0) BadgedBox(badge = { Badge { Text("$pending") } }) { Icon(t.icon, tr(t.label)) }
                            else Icon(t.icon, tr(t.label))
                        },
                        label = { Text(tr(t.label), maxLines = 1) },
                    )
                }
            }
        },
    ) { pad ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(pad)) {
            composable("home") {
                HomeScreen(
                    me, sessions, announcements, assignments, unread, pending,
                    onBell = { nav.navigate("notifications") },
                    onOpenNotice = { nav.navigate("notice/$it") },
                    onOpenNotices = { nav.navigate("notices") },
                    onOpenTimetable = { goTab("timetable") },
                    onOpenCalendar = { goTab("calendar") },
                    onAddNotice = { nav.navigate("compose") },
                    onAddSession = { nav.navigate("timetable?add=true") },
                    onOpenAdmin = { nav.navigate("admin") },
                )
            }
            composable(
                tr("timetable?add={add}"),
                arguments = listOf(navArgument("add") { type = NavType.BoolType; defaultValue = false }),
            ) { entry ->
                TimetableScreen(me, sessions, offerings, sessionsVm, openEditor = entry.arguments?.getBoolean("add") == true)
            }
            composable("materials") {
                MaterialsScreen(me, materials, offerings, materialsVm, onView = { nav.navigate("pdf/$it") })
            }
            composable("pdf/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                PdfViewerScreen(entry.arguments?.getString("id").orEmpty(), materials, materialsVm, onBack = { nav.popBackStack() })
            }
            composable("calendar") {
                CalendarScreen(me, assignments, sessions, offerings, assignmentsVm)
            }
            composable("notices") {
                AnnouncementsScreen(
                    isStaff, announcements, onRefresh = { annVm.list.refresh() },
                    onOpen = { nav.navigate("notice/$it") }, onAdd = { nav.navigate("compose") },
                    onBack = { nav.popBackStack() },
                )
            }
            composable("notice/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                AnnouncementDetailScreen(
                    entry.arguments?.getString("id").orEmpty(), me, announcements, offerings, programmes, annVm,
                    onBack = { nav.popBackStack() },
                )
            }
            composable("compose") {
                ComposeAnnouncementScreen(me, offerings, programmes, annVm, onBack = { nav.popBackStack() })
            }
            composable("notifications") {
                NotificationsScreen(
                    notifications, notifVm, onBack = { nav.popBackStack() },
                    onOpenNotice = { nav.navigate("notice/$it") },
                    onOpenTimetable = { goTab("timetable") },
                    onOpenCalendar = { goTab("calendar") },
                    onOpenMaterials = { goTab("materials") },
                )
            }
            composable("profile") {
                ProfileScreen(
                    me, programmes.items, lang = lang, onLang = { c.settings.setLang(it) },
                    pendingApprovals = pending, onOpenAdmin = { nav.navigate("admin") }, onOpenNotices = { nav.navigate("notices") },
                    onRefresh = {
                        onRefreshProfile(); sessionsVm.list.refresh(); annVm.list.refresh(); notifVm.list.refresh()
                        materialsVm.list.refresh(); assignmentsVm.list.refresh()
                    },
                    onSignOut = onSignOut,
                )
            }
            composable("admin") {
                if (adminVm != null) AdminScreen(adminVm, lookups, onBack = { nav.popBackStack() })
            }
        }
    }
}
