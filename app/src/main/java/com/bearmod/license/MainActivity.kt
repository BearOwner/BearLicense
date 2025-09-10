package com.bearmod.license

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween as coreTween
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.launch
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bearmod.license.ui.theme.BearLicenseTheme
import com.bearmod.license.models.Role
import com.bearmod.license.models.User
import com.bearmod.license.ui.AdminDashboard
import com.bearmod.license.ui.OwnerDashboard
import com.bearmod.license.ui.ResellerDashboard
import androidx.compose.ui.platform.LocalContext
import com.bearmod.license.data.SessionManager
import com.bearmod.license.ui.licenses.LicenseListScreen
import com.bearmod.license.ui.licenses.CreateLicenseScreen
import com.bearmod.license.BuildConfig
import com.bearmod.license.network.repository.AuthRepository
import com.bearmod.license.network.repository.MockAuthRepository
import com.bearmod.license.network.repository.LicenseRepository
import com.bearmod.license.network.repository.MockLicenseRepository
import com.bearmod.license.models.License
import com.bearmod.shared.models.License as SharedLicense
import com.bearmod.license.R

enum class LoginMode { Email, AuthKey }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BearLicenseTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppRoot(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun AppRoot(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val roleState: MutableState<Role?> = remember { mutableStateOf(null) }
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()
    val lastRole = sessionManager.lastRole.collectAsState(initial = null)
    val authRepo: AuthRepository = remember { MockAuthRepository() }
    val licenseRepo: LicenseRepository = remember { MockLicenseRepository() }

    Scaffold(modifier = modifier.fillMaxSize()) { inner ->
        NavHost(
            navController = navController,
            startDestination = "login",
            modifier = Modifier.padding(inner)
        ) {
            composable("login") {
                // If we already have a session, auto-redirect to last role (default Owner)
                LaunchedEffect(lastRole.value) {
                    val loggedIn = authRepo.currentUser().getOrNull() != null
                    if (loggedIn) {
                        val role = lastRole.value ?: Role.OWNER
                        when (role) {
                            Role.OWNER -> navController.navigate("owner") { popUpTo("login") { inclusive = true } }
                            Role.ADMINISTRATOR -> navController.navigate("admin") { popUpTo("login") { inclusive = true } }
                            Role.RESELLER -> navController.navigate("reseller") { popUpTo("login") { inclusive = true } }
                        }
                    }
                }
                LoginScreen(
                    auth = authRepo,
                    onSelectRole = { role ->
                        roleState.value = role
                        scope.launch { sessionManager.setLastRole(role) }
                        when (role) {
                            Role.OWNER -> navController.navigate("owner") { popUpTo("login") { inclusive = true } }
                            // After admin login, proceed to licenses directly for key generation
                            Role.ADMINISTRATOR -> navController.navigate("licenses") { popUpTo("login") { inclusive = true } }
                            Role.RESELLER -> navController.navigate("reseller") { popUpTo("login") { inclusive = true } }
                        }
                    }
                )
            }
            composable("owner") {
                val user = User(id = "demo", username = "owner", role = Role.OWNER, email = null)
                OwnerDashboard(currentUser = user, onManageLicenses = {
                    navController.navigate("licenses")
                })
            }
            composable("admin") {
                val user = User(id = "demo", username = "admin", role = Role.ADMINISTRATOR, email = null)
                AdminDashboard(currentUser = user, onManageLicenses = {
                    navController.navigate("licenses")
                })
            }
            composable("reseller") {
                val user = User(id = "demo", username = "reseller", role = Role.RESELLER, email = null)
                ResellerDashboard(currentUser = user)
            }
            composable("licenses") {
                var items by remember { mutableStateOf(listOf<License>()) }
                suspend fun reload() {
                    val res = licenseRepo.listByUser("demo")
                    items = res.getOrElse { emptyList() }
                }
                LaunchedEffect(Unit) { reload() }
                LicenseListScreen(
                    licenses = items.map { it.toShared() },
                    onCreateNew = { navController.navigate("license-create") },
                    onRefresh = { scope.launch { reload() } },
                    onAutoDelete = { sharedLic ->
                        // revoke by id if present then refresh
                        val id = sharedLic.idKeys
                        scope.launch {
                            if (id.isNotBlank()) { runCatching { licenseRepo.revoke(id) } }
                            reload()
                        }
                    }
                )
            }
            composable("license-create") {
                CreateLicenseScreen(
                    licenseRepo = licenseRepo,
                    onCancel = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun LoginScreen(auth: com.bearmod.license.network.repository.AuthRepository, onSelectRole: (Role) -> Unit) {
    // Modes: register by email/password or auth key only

    var emailOrKey by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var mode by remember { mutableStateOf(LoginMode.Email) }
    var darkBackground by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()
    val bgColor by animateColorAsState(targetValue = if (darkBackground) Color(0xFF121212) else Color(0xFFFFFFFF), animationSpec = tween(400), label = "bg")
    val textColor = if (darkBackground) Color(0xFFEFEFEF) else Color(0xFF1A1A1A)

    Box(modifier = Modifier
        .fillMaxSize()
        .padding(0.dp)) {
        // Background tint
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = bgColor)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.Top,
        ) {
            // Header
            Row(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Users Panel",
                    color = textColor,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = { darkBackground = !darkBackground }) {
                    Text(if (darkBackground) "Light" else "Dark")
                }
            }

            Spacer(Modifier.height(24.dp))

            // Logo with subtle breathing animation
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val infinite = rememberInfiniteTransition(label = "logo")
                val scaleAnim by infinite.animateFloat(
                    initialValue = 0.95f,
                    targetValue = 1.05f,
                    animationSpec = infiniteRepeatable(animation = coreTween(durationMillis = 1600, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
                    label = "scale"
                )
                val alphaAnim by infinite.animateFloat(
                    initialValue = 0.85f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(animation = coreTween(durationMillis = 1600, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
                    label = "alpha"
                )

                Image(
                    painter = painterResource(id = R.drawable.icon1),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .height(96.dp)
                        .scale(scaleAnim)
                        .alpha(alphaAnim),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(Modifier.height(12.dp))

            // Mode toggle
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { mode = LoginMode.Email }, enabled = mode != LoginMode.Email) { Text("Email", color = if (mode == LoginMode.Email) textColor else Color.Unspecified) }
                Button(onClick = { mode = LoginMode.AuthKey }, enabled = mode != LoginMode.AuthKey) { Text("Auth key", color = if (mode == LoginMode.AuthKey) textColor else Color.Unspecified) }
            }

            Spacer(Modifier.height(16.dp))

            // Username / Email or Key field
            OutlinedTextField(
                value = emailOrKey,
                onValueChange = { emailOrKey = it },
                label = { Text(if (mode == LoginMode.Email) "Email" else "Auth key") },
                leadingIcon = { Text("🔍") },
                modifier = Modifier
                    .fillMaxSize()
            )

            // Animated password field for Email mode
            AnimatedVisibility(
                visible = mode == LoginMode.Email,
                enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
            ) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 10.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = !isLoading, onClick = {
                    error = null
                    scope.launch {
                        isLoading = true
                        val result = if (mode == LoginMode.Email) {
                            auth.login(emailOrKey.trim(), password)
                        } else {
                            // For now, pass key as email and blank password to reuse mock repo
                            auth.login(emailOrKey.trim(), "")
                        }
                        isLoading = false
                        result.onSuccess {
                            // Show role selector below after a successful login
                        }.onFailure { t ->
                            error = t.message ?: "Login failed"
                        }
                    }
                }) {
                    Text(if (isLoading) "SIGNING IN..." else "LOGIN")
                }
            }

            if (error != null) {
                Text(text = error!!, color = Color(0xFFEF5350), modifier = Modifier.padding(top = 6.dp))
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "Select role to continue",
                color = textColor,
                style = MaterialTheme.typography.bodyMedium,
            )
            RoleSelector(remember { mutableStateOf(Role.OWNER) }, onClick = onSelectRole)
        }
    }
}

@Composable
fun RoleSelector(roleState: MutableState<Role>, onClick: (Role) -> Unit = { roleState.value = it }) {
    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { onClick(Role.OWNER) }) { Text("Owner") }
        Button(onClick = { onClick(Role.ADMINISTRATOR) }) { Text("Admin") }
        Button(onClick = { onClick(Role.RESELLER) }) { Text("Reseller") }
    }
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    BearLicenseTheme {
        AppRoot()
    }
}

// Map app-layer License model to shared License model for UI that consumes shared types
private fun License.toShared(): SharedLicense = SharedLicense(
    idKeys = this.idKeys,
    game = this.game,
    userKey = this.userKey,
    durationDays = this.durationDays,
    expiredDate = this.expiredDate,
    maxDevices = this.maxDevices,
    devices = this.devices,
    status = this.status,
    registrator = this.registrator,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt
)
