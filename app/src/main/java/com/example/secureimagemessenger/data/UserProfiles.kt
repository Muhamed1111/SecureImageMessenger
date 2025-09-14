// UserProfiles.kt
package com.example.secureimagemessenger.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

suspend fun ensureUserProfile(
    auth: FirebaseAuth = FirebaseAuth.getInstance(),
    db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    val u = auth.currentUser ?: return
    val prof = mapOf(
        "uid" to u.uid,
        "email" to u.email,
        "displayName" to (u.displayName ?: u.email?.substringBefore("@"))
    )
    db.collection("users").document(u.uid).set(prof, SetOptions.merge()).await()
}
