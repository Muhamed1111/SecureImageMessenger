package com.example.secureimagemessenger.auth

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class FirebaseAuthManager(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    suspend fun signUp(email: String, password: String): Result<Unit> = runCatching {
        auth.createUserWithEmailAndPassword(email, password).await(); Unit
    }

    suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        auth.signInWithEmailAndPassword(email, password).await(); Unit
    }
    fun addAuthStateListener(listener: FirebaseAuth.AuthStateListener) {
        auth.addAuthStateListener(listener)
    }

    fun removeAuthStateListener(listener: FirebaseAuth.AuthStateListener) {
        auth.removeAuthStateListener(listener)
    }

    // NOVO: reset lozinke
    suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        require(email.isNotBlank()) { "Unesi email za reset lozinke." }
        auth.sendPasswordResetEmail(email).await(); Unit
    }

    fun isLoggedIn(): Boolean = auth.currentUser != null
    fun signOut() = auth.signOut()
}
