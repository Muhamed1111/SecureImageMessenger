package com.example.secureimagemessenger.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.secureimagemessenger.data.ChatRepository
import com.example.secureimagemessenger.data.UserProfile
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatThreadScreen(
    repo: ChatRepository,
    peer: UserProfile,
    onBack: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // stream poruka
    val messages = remember { mutableStateListOf<ChatRepository.ChatMessage>() }
    DisposableEffect(peer.uid) {
        messages.clear()
        val reg = repo.observeChat(peer.uid) { msg -> messages.add(msg) }
        onDispose { reg.remove() }
    }

    // slanje
    var msgText by remember { mutableStateOf("") }
    var passSend by remember { mutableStateOf("") }
    var coverUri by remember { mutableStateOf<Uri?>(null) }
    val pickCover = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> coverUri = uri }

    // decode dialog
    var selected by remember { mutableStateOf<ChatRepository.ChatMessage?>(null) }
    var previewBytes by remember { mutableStateOf<ByteArray?>(null) }
    var passDecode by remember { mutableStateOf("") }
    var decoded by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(peer.displayName ?: peer.email ?: peer.uid) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { pads ->
        Column(Modifier.padding(pads).fillMaxSize()) {

            // LISTA PORUKA
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { m ->
                    Card {
                        Column(Modifier.padding(12.dp)) {
                            Text("from: ${m.from.take(6)}…   chunks: ${m.chunkCount}",
                                style = MaterialTheme.typography.bodySmall)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = {
                                    selected = m; decoded = null; error = null
                                    scope.launch {
                                        try {
                                            previewBytes = repo.loadPngBytes(m)
                                        } catch (e: Exception) {
                                            error = e.message
                                        }
                                    }
                                }) { Text("Preview") }
                                Spacer(Modifier.width(8.dp))
                                TextButton(onClick = {
                                    selected = m; decoded = null; error = null
                                    passDecode = ""
                                    previewBytes = null
                                }) { Text("Open") }
                            }
                        }
                    }
                }
            }

            // INPUT ZA SLANJE
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                OutlinedTextField(
                    value = msgText,
                    onValueChange = { msgText = it },
                    label = { Text("Message") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = passSend,
                    onValueChange = { passSend = it },
                    label = { Text("Key (password)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = {
                        pickCover.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }) {
                        Text(if (coverUri == null) "Choose image (optional)" else "Change image")
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        enabled = msgText.isNotBlank() && passSend.isNotBlank(),
                        onClick = {
                            scope.launch {
                                try {
                                    val coverBytes = coverUri?.let { uri ->
                                        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                                    }
                                    repo.sendMessage(
                                        toUid = peer.uid,
                                        secretText = msgText,
                                        password = passSend,
                                        carrierBytes = coverBytes
                                    ).getOrThrow()

                                    msgText = ""
                                    passSend = ""
                                    coverUri = null
                                } catch (e: Exception) {
                                    error = e.message
                                }
                            }
                        }
                    ) { Text("Send") }
                }
            }
        }
    }

    // DIALOG ZA DECODE
    if (selected != null) {
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text("Open message") },
            text = {
                Column {
                    previewBytes?.let {
                        AsyncImage(
                            model = it,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    OutlinedTextField(
                        value = passDecode,
                        onValueChange = { passDecode = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    decoded?.let { Text("Message:\n$it") }
                    error?.let { Text("Error: $it", color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val m = selected ?: return@TextButton
                    val p = passDecode
                    if (p.isBlank()) return@TextButton
                    scope.launch {
                        repo.readHiddenText(m, p)
                            .onSuccess { decoded = it }
                            .onFailure { e -> error = e.message; decoded = null }
                    }
                }) { Text("Decode") }
            },
            dismissButton = {
                TextButton(onClick = { selected = null }) { Text("Close") }
            }
        )
    }
}

