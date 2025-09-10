package com.bearmod.license.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.bearmod.license.models.User

@Composable
fun ResellerDashboard(currentUser: User?) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Reseller Dashboard",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(text = "Welcome ${currentUser?.username ?: "Reseller"}")
    }
}
