package com.example.secureimagemessenger.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.secureimagemessenger.data.ApiInterface
import com.example.secureimagemessenger.data.ApiService
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

@Composable
fun ReceiveScreen(api: ApiInterface) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var password by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var decodedMessage by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Photo Picker
    val pickStego = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        selectedUri = uri
        decodedMessage = null
        status = null
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Lozinka") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        if (selectedUri != null) {
            Image(
                painter = rememberAsyncImagePainter(selectedUri),
                contentDescription = "Stego slika",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentScale = ContentScale.Crop
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                pickStego.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }) {
                Text(if (selectedUri == null) "Odaberi stego sliku" else "Promijeni sliku")
            }

            Button(
                enabled = !isLoading && selectedUri != null && password.isNotBlank(),
                onClick = {
                    scope.launch {
                        isLoading = true
                        decodedMessage = null
                        status = "Dekodiram…"
                        try {
                            val temp = File(ctx.cacheDir, "stego_${System.currentTimeMillis()}.png")
                            ctx.contentResolver.openInputStream(selectedUri!!)?.use { input ->
                                temp.outputStream().use { output -> input.copyTo(output) }
                            }
                            val imagePart = MultipartBody.Part.createFormData(
                                name = "image",
                                filename = temp.name,
                                body = temp.asRequestBody("image/png".toMediaType())
                            )
                            val passBody = password.toRequestBody("text/plain".toMediaType())

                            val resp = api.extractAndDecrypt(imagePart, passBody)
                            decodedMessage = resp.message
                            status = "Poruka uspješno dekodirana."
                        } catch (e: Exception) {
                            decodedMessage = null
                            status = "Greška: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                }
            ) { Text(if (isLoading) "Obrada…" else "Dekodiraj") }
        }

        decodedMessage?.let {
            Divider()
            Text("Poruka:", style = MaterialTheme.typography.titleMedium)
            Text(it, style = MaterialTheme.typography.bodyLarge)
        }

        status?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }
}
