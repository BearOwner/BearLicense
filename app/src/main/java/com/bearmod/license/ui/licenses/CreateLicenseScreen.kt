package com.bearmod.license.ui.licenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.bearmod.license.network.repository.LicenseRepository
import com.bearmod.license.models.License
import com.bearmod.license.data.SellerAccountManager
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

private fun randomName(length: Int): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    return (1..length).map { chars[Random.nextInt(chars.length)] }.joinToString("")
}

private fun randomKey(length: Int): String {
    return randomName(length)
}

@Composable
fun CreateLicenseScreen(licenseRepo: LicenseRepository, onCancel: () -> Unit) {
    val game = remember { mutableStateOf("") }
    val duration = remember { mutableStateOf("") }
    val maxDevices = remember { mutableStateOf("") }
    val count = remember { mutableStateOf("1") }
    val system = remember { mutableStateOf("System A") } // both key systems option
    val pricePerKey = remember { mutableStateOf("0.00") }
    val sellerBase = remember { mutableStateOf("") }
    val startNo = remember { mutableStateOf("1") }
    val padDigits = remember { mutableStateOf("3") }
    val statusText = remember { mutableStateOf<String?>(null) }
    val generated = remember { mutableStateOf(listOf<String>()) }
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val sellerManager = remember { SellerAccountManager.get(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(text = "Create License", style = MaterialTheme.typography.headlineSmall)
        Text(text = "Balance: ${"%.2f".format(sellerManager.getBalance())}")
        // Seller base name with trailing refresh to randomize
        OutlinedTextField(
            value = sellerBase.value,
            onValueChange = { sellerBase.value = it },
            label = { Text("Seller base name (e.g., BearSeller)") },
            trailingIcon = {
                androidx.compose.material3.IconButton(onClick = {
                    // Fill with a random 8-char seller name when requested
                    sellerBase.value = randomName(8)
                }) { Text("↻") }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 8.dp)
        )
        OutlinedTextField(value = game.value, onValueChange = { game.value = it }, label = { Text("Game") })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = duration.value, onValueChange = { duration.value = it }, label = { Text("Duration (days)") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = maxDevices.value, onValueChange = { maxDevices.value = it }, label = { Text("Max Devices") }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = count.value, onValueChange = { count.value = it }, label = { Text("Count (max 100)") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = system.value, onValueChange = { system.value = it }, label = { Text("Key System") }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = startNo.value, onValueChange = { startNo.value = it }, label = { Text("Start No.") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = padDigits.value, onValueChange = { padDigits.value = it }, label = { Text("Pad Digits") }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = pricePerKey.value, onValueChange = { pricePerKey.value = it }, label = { Text("Price per key") })

        Button(onClick = {
            val dur = duration.value.toIntOrNull() ?: 0
            val max = maxDevices.value.toIntOrNull() ?: 1
            val c = (count.value.toIntOrNull() ?: 1).coerceIn(1, 100)
            val price = pricePerKey.value.toDoubleOrNull() ?: 0.0
            val totalCost = price * c

            scope.launch {
                if (!sellerManager.tryDeduct(totalCost)) {
                    statusText.value = "Insufficient balance. Need ${"%.2f".format(totalCost)}"
                    return@launch
                }
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val dateStr = sdf.format(Date())
                val sb = StringBuilder()
                val list = mutableListOf<String>()
                var success = 0
                val start = startNo.value.toIntOrNull() ?: 1
                val pad = padDigits.value.toIntOrNull()?.coerceIn(0, 6) ?: 0
                repeat(c) { idx ->
                    val invoice = (System.currentTimeMillis() / 1000L).toString() + "-${idx + 1}"
                    val key = randomKey(16)
                    val base = sellerBase.value.trim()
                    val sellerName = if (base.isBlank()) {
                        // Random seller name (Base62), 8 chars when base is blank
                        randomName(8)
                    } else if (c == 1) {
                        base
                    } else {
                        val num = (start + idx).toString().padStart(pad, '0')
                        "$base$num"
                    }
                    val label = "$sellerName:$key"
                    val lic = License(
                        idKeys = "",
                        game = game.value.trim(),
                        userKey = key,
                        durationDays = dur,
                        expiredDate = if (dur > 0) System.currentTimeMillis() + dur * 24L * 60L * 60L * 1000L else null,
                        maxDevices = max,
                        devices = 0,
                        status = "active",
                        registrator = sellerName,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = null
                    )
                    val res = licenseRepo.create(lic)
                    if (res.isSuccess) {
                        success++
                        // Append to file content: invoice, sellerName:key, game, date, system, price
                        sb.appendLine("$invoice,$label,${lic.game},$dateStr,${system.value},$price")
                        list += label
                    }
                }

                // Write to BearUsers.txt in app external files dir
                // TODO: Offer a Storage Access Framework (SAF) picker to let the seller
                // choose a public folder like Downloads and write the file there.
                // This will make the file visible outside the app without root.
                val root = context.getExternalFilesDir(null) ?: context.filesDir
                val file = File(root, "BearUsers.txt")
                try {
                    file.appendText(sb.toString())
                    statusText.value = "Generated $success/${c} keys. Charged ${"%.2f".format(totalCost)}. New balance: ${"%.2f".format(sellerManager.getBalance())}. Saved to ${file.absolutePath}"
                    generated.value = list
                } catch (t: Throwable) {
                    statusText.value = "Generated $success/${c} keys. Failed to write file: ${t.message}"
                }
            }
        }, modifier = Modifier.padding(top = 16.dp)) {
            Text("Generate")
        }
        Button(onClick = onCancel, modifier = Modifier.padding(top = 8.dp)) {
            Text("Cancel")
        }

        if (statusText.value != null) {
            Spacer(Modifier.height(8.dp))
            Text(statusText.value!!, color = MaterialTheme.colorScheme.primary)
        }

        if (generated.value.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("Generated (tap Copy to clipboard):", style = MaterialTheme.typography.labelLarge)
            // Show preview block
            androidx.compose.material3.Surface(tonalElevation = 1.dp) {
                Text(
                    generated.value.joinToString("\n"),
                    modifier = Modifier.padding(12.dp)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                Button(onClick = { scope.launch { clipboard.setText(generated.value.joinToString("\n")) } }) {
                    Text("COPY")
                }
            }
        }
    }
}
