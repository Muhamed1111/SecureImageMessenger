package com.example.secureimagemessenger.screen


import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.VisualTransformation
import android.content.ContentValues
import android.content.Intent
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.secureimagemessenger.data.ApiInterface
// >>> DODANO: ako želiš slati u chat (Spark varijanta bez Storage-a)
import com.example.secureimagemessenger.data.ChatRepository
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

@Composable
fun SendScreen(
    api: ApiInterface,
    recipientName: String = "Recipient",
    // >>> DODANO: repo je opcionalan – proslijedi ga samo ako želiš “send to chat”
    repo: ChatRepository? = null
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    // state
    var message by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var coverUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var savedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }

    // >>> DODANO: UID primaoca za chat – opcionalno polje
    var toUid by remember { mutableStateOf("") }

    // picker
    val pickCover = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> coverUri = uri }

    // paleta
    val Panel = Color(0xFF2F3C4A)
    val PanelDark = Color(0xFF26323D)
    val Orange = Color(0xFFEEA43A)

    fun fileLabel(): String =
        coverUri?.lastPathSegment ?: "No image selected"

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = PanelDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Panel)
        ) {
            Text(
                "Encode",
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 18.dp, top = 16.dp, end = 18.dp)
            )
            Text(
                recipientName,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 18.dp, top = 4.dp, end = 18.dp, bottom = 12.dp)
            )

            // >>> DODANO: UID primaoca (samo tekstualno polje, ne mijenja dizajn)
            OutlinedTextField(
                value = toUid,
                onValueChange = { toUid = it },
                label = { Text("UID primaoca (opcionalno)") },
                singleLine = true,
                modifier = Modifier
                    .padding(horizontal = 18.dp)
                    .fillMaxWidth()
            )

            // Slika ili placeholder
            Card(
                modifier = Modifier
                    .padding(horizontal = 18.dp, vertical = 12.dp)
                    .fillMaxWidth()
                    .height(170.dp)
                    .clickable { pickCover.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (coverUri == null) PanelDark else Color.Black
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                if (coverUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(coverUri),
                        contentDescription = "Cover",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Outlined.Image, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Tap to choose image", color = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            InputChipRow(
                icon = Icons.Outlined.InsertDriveFile,
                text = fileLabel()
            )

            InputFieldRow(
                icon = Icons.Outlined.Edit,
                value = message,
                onValueChange = { message = it },
                placeholder = "Message"
            )

            InputChipRow(
                icon = Icons.Outlined.Security,
                text = "AES"
            )

            PasswordFieldRow(
                icon = Icons.Outlined.VpnKey,
                value = password,
                onValueChange = { password = it },
                placeholder = "Key"
            )

            Spacer(Modifier.height(14.dp))
            Divider(color = PanelDark, thickness = 1.dp)

            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        status = "Obrada…"
                        savedImageUri = null

                        try {
                            // >>> DODANO: zajednički cover bytes (treba i za repo i za API)
                            val coverBytes: ByteArray? = coverUri?.let { uri ->
                                ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                            }

                            // >>> NOVO: ako imamo repo i korisnik je unio toUid -> ŠALJI U CHAT (Firestore chunks, Spark)
                            if (repo != null && toUid.isNotBlank()) {
                                repo.sendMessage(
                                    toUid = toUid,
                                    secretText = message,
                                    password = password,
                                    carrierBytes = coverBytes
                                ).getOrThrow()

                                status = "Poruka poslana u chat."
                                // očisti samo unos poruke i lozinke (sliku ostavi ako želiš)
                                message = ""; password = ""
                            } else {
                                // >>> TVOJ POSTOJEĆI FLOW: pozovi backend i SAČUVAJ U GALERIJU
                                val textPlain = "text/plain".toMediaType()
                                val msgBody = message.toRequestBody(textPlain)
                                val passBody = password.toRequestBody(textPlain)

                                val coverPart: MultipartBody.Part? = coverBytes?.let { bytes ->
                                    // temp fajl je OK, ali možeš i direktno iz bytes
                                    val tmp = File(
                                        ctx.cacheDir,
                                        "cover_${System.currentTimeMillis()}.tmp"
                                    )
                                    tmp.outputStream().use { it.write(bytes) }
                                    MultipartBody.Part.createFormData(
                                        name = "cover",
                                        filename = "cover.png",
                                        body = tmp.asRequestBody("image/*".toMediaType())
                                    )
                                }

                                val resp = api.encryptAndEmbed(
                                    message = msgBody,
                                    password = passBody,
                                    cover = coverPart
                                )

                                if (!resp.isSuccessful || resp.body() == null) {
                                    status = "API greška: ${resp.code()}"
                                } else {
                                    val pngBytes = resp.body()!!.bytes()
                                    val uri = ctx.contentResolver.insert(
                                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                        ContentValues().apply {
                                            put(
                                                MediaStore.Images.Media.DISPLAY_NAME,
                                                "secret_${System.currentTimeMillis()}.png"
                                            )
                                            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                                        }
                                    )
                                    if (uri != null) {
                                        ctx.contentResolver.openOutputStream(uri)
                                            ?.use { it.write(pngBytes) }
                                        savedImageUri = uri
                                        status = "Slika je sačuvana u galeriji."
                                    } else status = "Nije moguće sačuvati sliku."
                                }
                            }
                        } catch (e: Exception) {
                            status = "Greška: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading && message.isNotBlank() && password.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Orange,
                    contentColor = Color.White,
                    disabledContainerColor = Orange.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    if (isLoading) "Sending…" else "Send",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            savedImageUri?.let { uri ->
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = {
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        ctx.startActivity(Intent.createChooser(share, "Share"))
                    }) { Text("Share") }
                    Spacer(Modifier.width(12.dp))
                    Text(text = status ?: "", color = Color.White)
                }

                Spacer(Modifier.height(10.dp))
                Card(
                    modifier = Modifier
                        .padding(horizontal = 18.dp)
                        .fillMaxWidth()
                        .height(120.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(uri),
                        contentDescription = "Saved image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } ?: run {
                status?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = it,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 18.dp)
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
        }
    }
}

// ===== Helpers: zalijepi ispod SendScreen(), izvan njegovih zagrada =====




@Composable
private fun InputChipRow(
    icon: ImageVector,
    text: String
) {
    val shape = RoundedCornerShape(14.dp)
    val bg = Color(0x33586A79) // translucent kao u tvojoj paleti
    Row(
        modifier = Modifier
            .padding(horizontal = 18.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(bg)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.White)
        Spacer(Modifier.width(10.dp))
        Text(text, color = Color.White)
    }
}

@Composable
private fun InputFieldRow(
    icon: ImageVector,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .padding(horizontal = 18.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0x33586A79))
            .padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.White)
        Spacer(Modifier.width(10.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            visualTransformation = visualTransformation,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Color.White,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedPlaceholderColor = Color(0xCCFFFFFF),
                unfocusedPlaceholderColor = Color(0x88FFFFFF)
            )
        )
    }
}

@Composable
private fun PasswordFieldRow(
    icon: ImageVector,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    InputFieldRow(
        icon = icon,
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        visualTransformation = PasswordVisualTransformation()
    )
}
