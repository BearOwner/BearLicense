package com.bearmod.owner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import com.bearmod.shared.repository.MockControlsRepository
import com.bearmod.shared.repository.ControlsRepository
import com.bearmod.shared.repository.SupabaseControlsRepository
import com.bearmod.shared.models.FeatureFlag
import com.bearmod.shared.models.UserControl
import com.bearmod.shared.models.Audience
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.bearmod.shared.repository.SellersRepository
import com.bearmod.shared.repository.MockSellersRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val host = SnackbarHostState()
            Scaffold(modifier = Modifier.fillMaxSize(), snackbarHost = { SnackbarHost(hostState = host) }) { inner ->
                OwnerHome(modifier = Modifier.padding(inner), host = host)
            }
        }
    }
}

@Composable
fun OwnerHome(modifier: Modifier = Modifier, host: SnackbarHostState) {
    val repo: ControlsRepository = remember {
        if (BuildConfig.USE_MOCKS) MockControlsRepository()
        else SupabaseControlsRepository(
            baseRestUrl = BuildConfig.SUPABASE_URL.trimEnd('/') + "/rest/v1",
            apiKey = BuildConfig.SUPABASE_ANON_KEY
        )
    }
    var tab by remember { mutableStateOf(0) } // 0 Flags, 1 User Controls, 2 Seller Mgmt
    Column(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Owner Control Panel", style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { tab = 0 }) { Text(if (tab == 0) "FEATURE FLAGS" else "Feature Flags") }
            OutlinedButton(onClick = { tab = 1 }) { Text(if (tab == 1) "USER CONTROLS" else "User Controls") }
            OutlinedButton(onClick = { tab = 2 }) { Text(if (tab == 2) "SELLER MGMT" else "Seller Mgmt") }
        }
        when (tab) {
            0 -> FeatureFlagsView(repo, host)
            1 -> UserControlsView(repo, host)
            else -> SellerManagementView(MockSellersRepository(), host)
        }
    }
}

@Composable
private fun FeatureFlagsView(repo: ControlsRepository, host: SnackbarHostState) {
    var items by remember { mutableStateOf(listOf<FeatureFlag>()) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        items = repo.listFlags().getOrElse { emptyList() }
    }
    var audience by remember { mutableStateOf<Audience?>(null) } // null = ALL items
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = {
            scope.launch { items = repo.listFlags().getOrElse { emptyList() } }
        }) { Text("Refresh") }
        OutlinedButton(onClick = { audience = null }) { Text("All") }
        OutlinedButton(onClick = { audience = Audience.ADMINISTRATOR }) { Text("Admins") }
        OutlinedButton(onClick = { audience = Audience.RESELLER }) { Text("Resellers") }
    }
    val filtered = items.filter { a ->
        audience == null || a.audience == audience || (audience == Audience.ADMINISTRATOR && a.audience == Audience.ALL) || (audience == Audience.RESELLER && a.audience == Audience.ALL)
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(filtered, key = { it.key + it.audience.name }) { flag ->
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = flag.key, style = MaterialTheme.typography.bodyLarge)
                    Text(text = "Audience: ${flag.audience}", style = MaterialTheme.typography.labelMedium)
                }
                var checked by remember(flag) { mutableStateOf(flag.enabled) }
                Switch(checked = checked, onCheckedChange = { v ->
                    checked = v
                    scope.launch {
                        val res = repo.upsertFlag(flag.copy(enabled = v))
                        if (res.isSuccess) host.showSnackbar("Saved ${flag.key} = $v") else host.showSnackbar("Failed to save ${flag.key}")
                    }
                }, colors = SwitchDefaults.colors())
            }
        }
    }
}

@Composable
private fun UserControlsView(repo: ControlsRepository, host: SnackbarHostState) {
    var items by remember { mutableStateOf(listOf<UserControl>()) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        items = repo.listUserControls().getOrElse { emptyList() }
    }
    var query by remember { mutableStateOf("") }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = {
            scope.launch { items = repo.listUserControls().getOrElse { emptyList() } }
        }) { Text("Refresh") }
        OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Search userId") })
    }
    val filtered = items.filter { it.userId.contains(query, ignoreCase = true) }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(filtered, key = { it.userId }) { uc ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = uc.userId, style = MaterialTheme.typography.titleSmall)
                ToggleRow("Can login", uc.canLogin) { v -> uc.copy(canLogin = v).also { scope.launch { val r = repo.upsertUserControl(it); if (r.isSuccess) host.showSnackbar("Saved canLogin=$v") else host.showSnackbar("Save failed") } } }
                ToggleRow("Can create keys", uc.canCreateKeys) { v -> uc.copy(canCreateKeys = v).also { scope.launch { val r = repo.upsertUserControl(it); if (r.isSuccess) host.showSnackbar("Saved canCreateKeys=$v") else host.showSnackbar("Save failed") } } }
                ToggleRow("Can reset keys", uc.canResetKeys) { v -> uc.copy(canResetKeys = v).also { scope.launch { val r = repo.upsertUserControl(it); if (r.isSuccess) host.showSnackbar("Saved canResetKeys=$v") else host.showSnackbar("Save failed") } } }
                ToggleRow("Can add balance", uc.canAddBalance) { v -> uc.copy(canAddBalance = v).also { scope.launch { val r = repo.upsertUserControl(it); if (r.isSuccess) host.showSnackbar("Saved canAddBalance=$v") else host.showSnackbar("Save failed") } } }
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, initial: Boolean, onChange: (Boolean) -> Unit) {
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
        Text(label)
        var checked by remember { mutableStateOf(initial) }
        Switch(checked = checked, onCheckedChange = { v ->
            checked = v
            onChange(v)
        })
    }
}

@Composable
private fun SellerManagementView(repo: SellersRepository, host: SnackbarHostState) {
    val scope = rememberCoroutineScope()
    var sellers by remember { mutableStateOf(listOf<com.bearmod.shared.models.Seller>()) }
    var expanded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<com.bearmod.shared.models.Seller?>(null) }
    var amount by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        sellers = repo.list().getOrElse { emptyList() }
        selected = sellers.firstOrNull()
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Seller Management", style = MaterialTheme.typography.titleMedium)
        Card(colors = CardDefaults.cardColors()) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Current Balance: $${String.format("%.2f", selected?.balance ?: 0.0)}")
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount ($)") })
                OutlinedButton(onClick = { expanded = true }) { Text(selected?.username ?: "Choose a reseller") }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    sellers.forEach { s ->
                        DropdownMenuItem(text = { Text(s.username) }, onClick = { selected = s; expanded = false })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val sel = selected ?: return@Button
                        val amt = amount.toDoubleOrNull() ?: 0.0
                        if (amt <= 0) { scope.launch { host.showSnackbar("Enter a positive amount") }; return@Button }
                        scope.launch {
                            val res = repo.transferBalance(sel.id, amt)
                            if (res.isSuccess) {
                                val newBal = res.getOrNull() ?: 0.0
                                sellers = sellers.map { if (it.id == sel.id) it.copy(balance = newBal) else it }
                                selected = sellers.firstOrNull { it.id == sel.id }
                                host.showSnackbar("Transferred $${String.format("%.2f", amt)} to ${sel.username}")
                            } else host.showSnackbar("Transfer failed")
                        }
                    }) { Text("Transfer") }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        selected?.let { sel ->
            Card(colors = CardDefaults.cardColors()) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Reseller Info", style = MaterialTheme.typography.labelLarge)
                    Text("User: ${sel.username}")
                    Text("Email: ${sel.email}")
                    Text("Invite Key: ${sel.inviteKey}")
                    Spacer(Modifier.height(6.dp))
                    Text("Quick Actions", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            scope.launch {
                                val r = repo.setVerified(sel.id, !sel.verified)
                                if (r.isSuccess) {
                                    sellers = sellers.map { if (it.id == sel.id) it.copy(verified = !sel.verified) else it }
                                    selected = sellers.firstOrNull { it.id == sel.id }
                                    host.showSnackbar(if (!sel.verified) "Verified" else "Unverified")
                                } else host.showSnackbar("Action failed")
                            }
                        }) { Text(if (sel.verified) "Unverify" else "Verify") }
                        Button(onClick = {
                            scope.launch {
                                val r = repo.setBanned(sel.id, !sel.banned)
                                if (r.isSuccess) {
                                    sellers = sellers.map { if (it.id == sel.id) it.copy(banned = !sel.banned) else it }
                                    selected = sellers.firstOrNull { it.id == sel.id }
                                    host.showSnackbar(if (!sel.banned) "Banned" else "Unbanned")
                                } else host.showSnackbar("Action failed")
                            }
                        }) { Text(if (sel.banned) "Unban" else "Ban") }
                        Button(onClick = {
                            scope.launch {
                                val r = repo.remove(sel.id)
                                if (r.isSuccess) {
                                    sellers = sellers.filterNot { it.id == sel.id }
                                    selected = sellers.firstOrNull()
                                    host.showSnackbar("Seller removed")
                                } else host.showSnackbar("Remove failed")
                            }
                        }) { Text("Remove") }
                    }
                }
            }
        }

        Text("All Sellers", style = MaterialTheme.typography.titleSmall)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(sellers, key = { it.id }) { s ->
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(s.username)
                        Text("$${String.format("%.2f", s.balance)}", style = MaterialTheme.typography.labelMedium)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(onClick = { selected = s }) { Text("Select") }
                        Button(onClick = {
                            scope.launch {
                                val r = repo.setBanned(s.id, !s.banned)
                                if (r.isSuccess) {
                                    sellers = sellers.map { if (it.id == s.id) it.copy(banned = !s.banned) else it }
                                    host.showSnackbar(if (!s.banned) "Banned" else "Unbanned")
                                } else host.showSnackbar("Action failed")
                            }
                        }) { Text(if (s.banned) "Unban" else "Ban") }
                    }
                }
            }
        }
    }
}
