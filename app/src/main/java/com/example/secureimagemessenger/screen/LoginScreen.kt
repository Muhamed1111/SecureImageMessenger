package com.example.secureimagemessenger.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.secureimagemessenger.auth.FirebaseAuthManager
import com.example.secureimagemessenger.data.ensureUserProfile
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    auth: FirebaseAuthManager,
    onLoggedIn: () -> Unit,
    onCreateAccount: () -> Unit = {} // ako želiš poseban flow
) {
    // ---- State
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // ---- Paleta / stilovi (po slici)
    val Yellow = Color(0xFFF5C84C)    // “pastel yellow”
    val Black = Color(0xFF000000)
    val FieldShape = RoundedCornerShape(28.dp)
    val ButtonShape = RoundedCornerShape(28.dp)

    fun handleResult(result: Result<Unit>) {
        result
            .onSuccess {
                // sačuvaj /users/{uid}
                kotlinx.coroutines.runBlocking { ensureUserProfile() }
                onLoggedIn()
            }

            .onFailure { e -> error = e.message ?: "Greška pri prijavi/registraciji." }
        isLoading = false
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddings ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Yellow)
                .padding(paddings)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(28.dp))

                // Gornja sijalica (logo)
                runCatching { painterResource(id = com.example.secureimagemessenger.R.drawable.ic_lightbulb) }
                    .onSuccess {
                        Image(
                            painter = it,
                            contentDescription = null,
                            modifier = Modifier
                                .size(96.dp)
                                .padding(top = 8.dp)
                        )
                    }

                Spacer(Modifier.height(12.dp))

                // Naslov u dvije linije
                Text(
                    text = "SecureImage\nMessenger",
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Black
                )

                Spacer(Modifier.height(24.dp))

                // “Sign In”
                Text(
                    "Sign In",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Black
                    ),
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(start = 4.dp)
                )

                Spacer(Modifier.height(10.dp))

                // Email
                TextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("Email") },
                    singleLine = true,
                    shape = FieldShape,
                    modifier = Modifier
                        .fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Black
                    )
                )

                Spacer(Modifier.height(10.dp))

                // Password
                TextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    shape = FieldShape,
                    modifier = Modifier
                        .fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Black
                    )
                )

                Spacer(Modifier.height(8.dp))

                // Remember + Forgot
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = rememberMe,
                        onCheckedChange = { rememberMe = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Black,
                            checkmarkColor = Yellow,
                            uncheckedColor = Black
                        )
                    )
                    Text("Remember", color = Black, modifier = Modifier.padding(start = 2.dp))

                    Spacer(Modifier.weight(1f))

                    TextButton(
                        onClick = {
                            if (email.isBlank()) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Unesite email za reset lozinke.")
                                }
                            } else {
                                isLoading = true; error = null
                                scope.launch {
                                    val res = auth.sendPasswordReset(email)
                                    isLoading = false
                                    res.onSuccess {
                                        snackbarHostState.showSnackbar("Email za reset lozinke je poslan.")
                                    }.onFailure {
                                        error = it.message ?: "Neuspješno slanje reset emaila."
                                    }
                                }
                            }
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Forget Password?", color = Black)
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Crno dugme (Sign In)
                Button(
                    enabled = !isLoading,
                    onClick = {
                        isLoading = true; error = null
                        // (opcionalno) zapamti rememberMe u prefs
                        scope.launch { handleResult(auth.signIn(email, password)) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(ButtonShape),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Black,
                        contentColor = Color.White,
                        disabledContainerColor = Black.copy(alpha = 0.5f)
                    )
                ) {
                    Text(if (isLoading) "Signing in..." else "Sign In")
                }

                // Ilustracija
                Spacer(Modifier.height(24.dp))
                runCatching { painterResource(id = com.example.secureimagemessenger.R.drawable.ill_people) }
                    .onSuccess {
                        Image(
                            painter = it,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        )
                    }

                Spacer(Modifier.height(24.dp))

                // Donji red: "Don't have account?  Create Account"
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Don't have account?  ", color = Black)
                    Text(
                        "Create Account",
                        color = Black,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable(enabled = !isLoading) {
                            // Ako želiš poseban ekran za registraciju:
                            if (onCreateAccount != {}) onCreateAccount()
                            else {
                                // ili – brzi signup s postojećim poljima:
                                isLoading = true; error = null
                                scope.launch { handleResult(auth.signUp(email, password)) }
                            }
                        }
                    )
                }

                // Error ispod
                error?.let { msg ->
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
