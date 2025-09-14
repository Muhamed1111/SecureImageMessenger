// ChatListScreen.kt
package com.example.secureimagemessenger.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.secureimagemessenger.data.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun ChatListScreen(
    onOpenThread: (UserProfile) -> Unit
) {
    val me = FirebaseAuth.getInstance().currentUser?.uid
    val db = remember { FirebaseFirestore.getInstance() }
    var users by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val snap = db.collection("users").get().await()
            users = snap.documents.mapNotNull { it.toObject(UserProfile::class.java) }
                .filter { it.uid != me }
            loading = false
        } catch (e: Exception) {
            error = e.message; loading = false
        }
    }

    if (loading) { LinearProgressIndicator(Modifier.fillMaxWidth()) }

    error?.let { Text("Greška: $it", color = MaterialTheme.colorScheme.error) }

    LazyColumn(Modifier.fillMaxSize().padding(12.dp)) {
        items(users, key = { it.uid }) { u ->
            Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)
                .clickable { onOpenThread(u) }) {
                Column(Modifier.padding(12.dp)) {
                    Text(u.displayName ?: u.email ?: u.uid, style = MaterialTheme.typography.titleMedium)
                    Text(u.email ?: "", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
