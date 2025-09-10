package com.bearmod.license.ui.licenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bearmod.shared.models.License
import kotlinx.coroutines.delay
import com.bearmod.shared.repository.MockControlsRepository
import com.bearmod.shared.repository.ControlsRepository
import com.bearmod.shared.models.FeatureFlag
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.graphics.Color

@Composable
fun LicenseListScreen(
    licenses: List<License>,
    onCreateNew: () -> Unit,
    onRefresh: () -> Unit = {},
    onRevoke: (License) -> Unit = {},
    onApprove: (License) -> Unit = {},
    onAutoDelete: (License) -> Unit = {}
) {
    var showAddUsers by remember { mutableStateOf(false) }
    var showAddReseller by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var filtered by remember { mutableStateOf(licenses) }
    var activeTab by remember { mutableStateOf(0) } // 0 = USERS, 1 = SELLERS
    var showWelcome by remember { mutableStateOf(true) }
    val controlsRepo: ControlsRepository = remember { MockControlsRepository() }
    var flags by remember { mutableStateOf<List<FeatureFlag>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(licenses, query) {
        filtered = if (query.isBlank()) licenses else licenses.filter {
            it.userKey.contains(query, ignoreCase = true) || it.game.contains(query, ignoreCase = true)
        }
    }
    LaunchedEffect(Unit) {
        flags = controlsRepo.listFlags().getOrElse { emptyList() }
    }
    // Auto-delete licenses that expired more than 24h ago
    LaunchedEffect(licenses) {
        val now = System.currentTimeMillis()
        val threshold = 24 * 60 * 60 * 1000L
        licenses.forEach { lic ->
            val expiredAt = lic.expiredDate
            if (expiredAt != null && now - expiredAt > threshold) {
                onAutoDelete(lic)
            }
        }
    }
    LaunchedEffect(showWelcome) {
        if (showWelcome) {
            delay(2500)
            showWelcome = false
        }
    }

    val snackbar = remember { SnackbarHostState() }
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbar) },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateNew) {
                Text("+")
            }
        }
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text(text = "Reset Panel", style = MaterialTheme.typography.headlineSmall)

            Spacer(Modifier.height(8.dp))
            // Segmented tabs (Users / Sellers)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { activeTab = 0 },
                    modifier = Modifier.weight(1f)
                ) { Text(if (activeTab == 0) "USERS" else "Users") }
                OutlinedButton(
                    onClick = { activeTab = 1 },
                    modifier = Modifier.weight(1f)
                ) { Text(if (activeTab == 1) "SELLERS" else "Sellers") }
            }

            if (activeTab == 1) {
                Spacer(Modifier.height(10.dp))
                // Reseller header card with balance pill
                Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 1.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Text(
                                text = "THORCHEAT OFFICIAL\nRESELLER",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "500.00", style = MaterialTheme.typography.labelLarge)
                                Spacer(Modifier.width(6.dp))
                                Text("⚙")
                            }
                        }
                    }
                }
            }

            // Action grid (2 rows x 4)
            val enabledKeys = flags.filter { it.enabled }.map { it.key }.toSet()
            val anyDisabled = flags.any { !it.enabled }
            val actionItems = if (activeTab == 0) {
                listOf(
                    ActionItem(label = "Add Users", enabled = "add_users" in enabledKeys) { showAddUsers = true },
                    ActionItem(label = "Delete Key", enabled = "delete_key" in enabledKeys) { /* TODO */ },
                    ActionItem(label = "Reset Password", enabled = true) { /* TODO */ },
                    ActionItem(label = "Reset Key", enabled = "reset_key" in enabledKeys) { /* TODO */ },
                )
            } else {
                listOf(
                    ActionItem(label = "Add Reseller", enabled = true) { showAddReseller = true },
                    ActionItem(label = "Add Balance", enabled = "add_balance" in enabledKeys) { /* TODO */ },
                    ActionItem(label = "Edit Name", enabled = true) { /* TODO */ },
                    ActionItem(label = "Delete Key", enabled = "delete_key" in enabledKeys) { /* TODO */ },
                    ActionItem(label = "Reset Password", enabled = true) { /* TODO */ },
                    ActionItem(label = "Reset Key", enabled = "reset_key" in enabledKeys) { /* TODO */ },
                )
            }
            ActionGrid(items = actionItems)

            if (anyDisabled) {
                Spacer(Modifier.height(6.dp))
                Surface(shape = RoundedCornerShape(8.dp), tonalElevation = 2.dp) {
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Some features are disabled by Owner. Pull to refresh or tap ⟳.", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search Key") },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                )
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { /* trigger search side-effects if needed */ }) {
                    Text("🔎")
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = (if (activeTab == 0) "No. of users  " else "Count Resellers  ") + filtered.size,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = {
                    onRefresh()
                    // also reload flags
                    scope.launch { flags = controlsRepo.listFlags().getOrElse { emptyList() } }
                }) {
                    Text("⟳")
                }
            }

            // Summary badges for key statuses
            Spacer(Modifier.height(8.dp))
            val now = System.currentTimeMillis()
            val counts = filtered.groupBy { lic ->
                val isExpired = lic.expiredDate?.let { it < now } ?: false
                val isBlocked = lic.status.equals("blocked", ignoreCase = true) || lic.status.equals("revoked", ignoreCase = true)
                val isNew = lic.createdAt?.let { now - it < 60 * 60 * 1000L } ?: false
                when {
                    isBlocked -> "BLOCKED"
                    isExpired -> "EXPIRED"
                    isNew -> "NEW"
                    else -> "ACTIVE"
                }
            }.mapValues { it.value.size }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                SummaryPill(label = "ACTIVE", count = counts["ACTIVE"] ?: 0, color = Color(0xFF66BB6A))
                SummaryPill(label = "NEW", count = counts["NEW"] ?: 0, color = Color(0xFF64B5F6))
                SummaryPill(label = "BLOCKED", count = counts["BLOCKED"] ?: 0, color = Color(0xFFFF6B6B))
                SummaryPill(label = "EXPIRED", count = counts["EXPIRED"] ?: 0, color = Color(0xFF9E9E9E))
            }

            if (licenses.isEmpty()) {
                Text(text = "No licenses yet.")
            } else {
                Spacer(Modifier.height(8.dp))
                val canDelete = "delete_key" in flags.filter { it.enabled }.map { it.key }.toSet()
                val canReset = "reset_key" in flags.filter { it.enabled }.map { it.key }.toSet()
                LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtered) { lic ->
                        LicenseRow(
                            license = lic,
                            onDelete = {
                                if (canDelete) onRevoke(lic) else scope.launch { snackbar.showSnackbar("Delete disabled by Owner") }
                            },
                            onApprove = {
                                if (canReset) onApprove(lic) else scope.launch { snackbar.showSnackbar("Reset disabled by Owner") }
                            }
                        )
                    }
                }
            }
            // Welcome pill at bottom center (over content)
            if (showWelcome) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("✔", color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text("Welcome Admin!", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
        }
    }
    }

    if (showAddUsers) {
        AddUsersDialog(
            onDismiss = { showAddUsers = false },
            onStart = { username, count, periodDays ->
                // For now, just call onCreateNew and dismiss. Later, batch create via repo.
                onCreateNew()
                showAddUsers = false
            }
        )
    }

    if (showAddReseller) {
        AddResellerDialog(
            onDismiss = { showAddReseller = false },
            onStart = { username, balance, period ->
                // TODO: call repo to add reseller
                showAddReseller = false
            }
        )
    }
}

data class ActionItem(val label: String, val enabled: Boolean, val onClick: () -> Unit)

@Composable
private fun ActionGrid(items: List<ActionItem>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        val chunked = items.chunked(4)
        chunked.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                rowItems.forEach { item ->
                    Box(modifier = Modifier.weight(1f)) {
                        ActionTile(item = item)
                    }
                }
                repeat(4 - rowItems.size) {
                    Spacer(Modifier.width(8.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ActionTile(item: ActionItem) {
    OutlinedButton(onClick = item.onClick, modifier = Modifier.fillMaxWidth(), enabled = item.enabled) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("⬤", modifier = Modifier.padding(bottom = 4.dp))
            Text(item.label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun LicenseRow(license: License, onDelete: () -> Unit, onApprove: () -> Unit) {
    Surface(
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = license.userKey,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(8.dp))
            StatusBadge(license = license)
            Spacer(Modifier.width(8.dp))
            Text("${license.durationDays}D", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.width(8.dp))
            // delete
            OutlinedButton(onClick = onDelete) { Text("🗑") }
            Spacer(Modifier.width(4.dp))
            // approve / tick
            OutlinedButton(onClick = onApprove) { Text("✔") }
        }
    }
}

@Composable
private fun StatusBadge(license: License) {
    val now = System.currentTimeMillis()
    val isExpired = license.expiredDate?.let { it < now } ?: false
    val isBlocked = license.status.equals("blocked", ignoreCase = true) || license.status.equals("revoked", ignoreCase = true)
    val isNew = license.createdAt?.let { now - it < 60 * 60 * 1000L } ?: false

    val (label, color) = when {
        isBlocked -> "BLOCKED" to Color(0xFFFF6B6B)
        isExpired -> "EXPIRED" to Color(0xFF9E9E9E)
        isNew -> "NEW" to Color(0xFF64B5F6)
        else -> "ACTIVE" to Color(0xFF66BB6A)
    }
    Surface(shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.2f)) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
@Composable
private fun SummaryPill(label: String, count: Int, color: Color) {
    Surface(shape = RoundedCornerShape(16.dp), color = color.copy(alpha = 0.2f)) {
        Text(
            text = "$label $count",
            color = color,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun AddUsersDialog(onDismiss: () -> Unit, onStart: (username: String, count: Int, periodDays: Int) -> Unit) {
    var username by remember { mutableStateOf("") }
    var count by remember { mutableStateOf("") }
    var period by remember { mutableStateOf("Days") }
    var periodExpanded by remember { mutableStateOf(false) }
    var days by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                val c = count.toIntOrNull() ?: 1
                val d = days.toIntOrNull() ?: 30
                onStart(username.trim(), c, d)
            }) { Text("START") }
        },
        dismissButton = { Button(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Add Users") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { username = "" }) { Text("↻") }
                }
                OutlinedTextField(value = count, onValueChange = { count = it }, label = { Text("No. of users") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = days, onValueChange = { days = it }, label = { Text("Period") }, modifier = Modifier.weight(1f))
                    // Unit dropdown
                    Box {
                        OutlinedButton(onClick = { periodExpanded = true }) { Text(period) }
                        androidx.compose.material3.DropdownMenu(expanded = periodExpanded, onDismissRequest = { periodExpanded = false }) {
                            androidx.compose.material3.DropdownMenuItem(text = { Text("Days") }, onClick = { period = "Days"; periodExpanded = false })
                            androidx.compose.material3.DropdownMenuItem(text = { Text("Months") }, onClick = { period = "Months"; periodExpanded = false })
                            androidx.compose.material3.DropdownMenuItem(text = { Text("Hour") }, onClick = { period = "Hour"; periodExpanded = false })
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun AddResellerDialog(onDismiss: () -> Unit, onStart: (username: String, balance: String, period: String) -> Unit) {
    var username by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf("Day") } // Day/Week/Month

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = { onStart(username.trim(), balance.trim(), selected) }) { Text("START") }
        },
        dismissButton = { Button(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Add Reseller") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { username = "" }) { Text("↻") }
                }
                OutlinedTextField(value = balance, onValueChange = { balance = it }, label = { Text("Balance") }, modifier = Modifier.fillMaxWidth())
                Text("Price Information", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Day", "Week", "Month").forEach { label ->
                        OutlinedButton(onClick = { selected = label }) {
                            Text(label)
                        }
                    }
                }
            }
        }
    )
}
