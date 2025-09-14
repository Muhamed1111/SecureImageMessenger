package com.example.secureimagemessenger.screen

import android.util.Log
import androidx.compose.runtime.LaunchedEffect
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.secureimagemessenger.data.ApiInterface
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    api: ApiInterface,
    onLogout: () -> Unit = {}
) {
    val db = Firebase.firestore

    LaunchedEffect(Unit) {
        try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
                ?: throw IllegalStateException("Nisi ulogovan")

            // WRITE (u /users/{uid})
            db.collection("users").document(uid)
                .set(mapOf("lastPing" to FieldValue.serverTimestamp()), SetOptions.merge())
                .await()

            // READ
            val snap = db.collection("users").document(uid).get().await()
            Log.d("FIRESTORE", "Ping OK, data=${snap.data}")
        } catch (e: Exception) {
            Log.e("FIRESTORE", "Ping FAIL", e)
        }
    }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var tab = rememberSaveable { mutableIntStateOf(0) }

    // boje
    val Yellow = Color(0xFFF5C84C)
    val Line = Color(0x66000000)
    val PanelDark = Color(0xFF26323D) // ista kao u SendScreen

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            // === ŽUTI DRAWER ===
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.82f),
                color = Yellow,
                shape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lightbulb,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Black
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Image\nSteganography",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.Black,
                        textAlign = TextAlign.Start
                    )
                    Spacer(Modifier.height(16.dp))
                    Divider(thickness = 1.dp, color = Line)

                    @Composable
                    fun DrawerItem(title: String, insetStart: Dp = 0.dp, onClick: () -> Unit = {}) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp)
                                .padding(start = insetStart)
                                .clickable {
                                    scope.launch { drawerState.close() }
                                    onClick()
                                }
                        ) {
                            Text(title, color = Color.Black, style = MaterialTheme.typography.titleMedium)
                        }
                        Divider(thickness = 1.dp, color = Line)
                    }

                    Spacer(Modifier.height(4.dp))
                    DrawerItem("Home") { }
                    DrawerItem("About Us", insetStart = 24.dp) { }
                    DrawerItem("Services") { }
                    DrawerItem("Chating") { }
                    DrawerItem("Premium") { }
                    DrawerItem("Tutorial") { }

                    Spacer(Modifier.weight(1f))
                    DrawerItem("Logout") { onLogout() }
                }
            }
        }
    ) {
        // === GLAVNI SADRŽAJ NA TAMNOJ POZADINI ===
        Scaffold(
            containerColor = PanelDark,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = PanelDark),
                    title = { Text("Secure Image Messenger", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = Color.White)
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(PanelDark)
                    .padding(16.dp)
            ) {
                when (tab.intValue) {
                    0 -> SendScreen(api = api, recipientName = "Recipient")
                    else -> ReceiveScreen(api)
                }
            }
        }
    }
}

