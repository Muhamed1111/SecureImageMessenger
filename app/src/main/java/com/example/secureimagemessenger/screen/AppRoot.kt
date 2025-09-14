package com.example.secureimagemessenger.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.material3.DrawerValue.Closed
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.secureimagemessenger.auth.FirebaseAuthManager
import com.example.secureimagemessenger.data.ApiInterface
import kotlinx.coroutines.launch

@Composable
fun AppRoot(
    api: ApiInterface,
    auth: FirebaseAuthManager = FirebaseAuthManager()
) {
    // Pratimo login stanje preko Firebase listenera
    var loggedIn by remember { mutableStateOf(auth.isLoggedIn()) }

    DisposableEffect(Unit) {
        val listener = com.google.firebase.auth.FirebaseAuth.AuthStateListener {
            loggedIn = auth.isLoggedIn()
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    if (!loggedIn) {
        LoginScreen(
            auth = auth,
            onLoggedIn = { loggedIn = true }
        )
        return
    }

    val drawerState = rememberDrawerState(initialValue = Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "Menu",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )
                NavigationDrawerItem(
                    label = { Text("Home") },
                    selected = true,
                    onClick = { scope.launch { drawerState.close() } }
                )
                NavigationDrawerItem(
                    label = { Text("Tutorial") },
                    selected = false,
                    onClick = { /* TODO: otvoriti tutorial screen kad ga dodam */ }
                )
                NavigationDrawerItem(
                    label = { Text("Logout") },
                    selected = false,
                    onClick = {
                        auth.signOut()
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        HomeScreen(
            api = api,
            onLogout = { auth.signOut() }
        )
    }
}
