// ChatRepository.kt
package com.example.secureimagemessenger.data

import android.util.Base64
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import kotlin.math.min

class ChatRepository(
    private val api: ApiInterface,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private fun chatIdFor(a: String, b: String) =
        listOf(a, b).sorted().joinToString("_")

    /**
     * carrierBytes: opcionalno — bajtovi cover slike (JPEG/PNG).
     * Implementation BEZ Storage-a: PNG sa skrivenom porukom čuvamo u Firestore
     * kao više Base64 "chunkova".
     */
    suspend fun sendMessage(
        toUid: String,
        secretText: String,
        password: String,
        carrierBytes: ByteArray? = null
    ): Result<Unit> = runCatching {
        val fromUid = auth.currentUser?.uid ?: error("Not logged in")

        // 1) Backend -> stego PNG bajtovi
        val resp = api.encryptAndEmbed(
            message = secretText.toTextBody(),
            password = password.toTextBody(),
            cover = carrierBytes?.toPngPart("cover", "cover.png")
        )
        require(resp.isSuccessful) { "Backend error: ${resp.code()}" }
        val stegoBytes = resp.body()?.bytes() ?: error("Empty body")

        // 2) Raspodijeli u chunkove koji staju u Firestore dokument
        val CHUNK_SIZE = 700_000 // ~700kB => Base64 < 1MB po dokumentu
        val rawChunks = stegoBytes.chunkedBytes(CHUNK_SIZE)
        val b64Chunks = rawChunks.map { bytes ->
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        }

        // 3) Upis u /chats/{chatId}
        val chatId = chatIdFor(fromUid, toUid)
        val msgRef = db.collection("chats").document(chatId)
            .collection("messages").document()

        // chat dokument (members + updatedAt) — merge
        db.collection("chats").document(chatId).set(
            mapOf(
                "members" to listOf(fromUid, toUid),
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        ).await()

        // osnovni meta dokument za poruku
        val msgDoc = hashMapOf(
            "from" to fromUid,
            "to" to toUid,
            "seen" to false,
            "ts" to FieldValue.serverTimestamp(),
            "mime" to "image/png",
            "size" to stegoBytes.size,
            "chunkCount" to b64Chunks.size
        )
        msgRef.set(msgDoc).await()

        // 4) podkolekcija chunks: /chats/{chatId}/messages/{msgId}/chunks/{i}
        val chunksCol = msgRef.collection("chunks")
        b64Chunks.forEachIndexed { i, s ->
            chunksCol.document(i.toString())
                .set(mapOf("i" to i, "data" to s))
                .await()
        }
    }

    data class ChatMessage(
        val id: String,
        val from: String,
        val to: String,
        val chunkCount: Int
    )

    fun observeChat(withUid: String, onNew: (ChatMessage) -> Unit): ListenerRegistration {
        val me = auth.currentUser?.uid ?: return ListenerRegistration { }
        val chatId = chatIdFor(me, withUid)
        return db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("ts")
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                for (dc in snap.documentChanges) {
                    if (dc.type == DocumentChange.Type.ADDED) {
                        val d = dc.document.data
                        onNew(
                            ChatMessage(
                                id = dc.document.id,
                                from = d["from"] as String,
                                to = d["to"] as String,
                                chunkCount = (d["chunkCount"] as? Number)?.toInt() ?: 0
                            )
                        )
                    }
                }
            }
    }
    // DODAJ OVO UNUTAR KLASE ChatRepository
    suspend fun loadPngBytes(message: ChatMessage): ByteArray {
        // isti algoritam za chatId kao i drugdje
        val chatId = chatIdFor(message.from, message.to)

        // /chats/{chatId}/messages/{msgId}/chunks/{i}
        val chunksSnap = db.collection("chats").document(chatId)
            .collection("messages").document(message.id)
            .collection("chunks").orderBy("i")
            .get().await()

        val parts = mutableListOf<ByteArray>()
        for (doc in chunksSnap.documents) {
            val b64 = doc.getString("data") ?: continue
            parts += Base64.decode(b64, Base64.NO_WRAP)
        }

        val total = parts.sumOf { it.size }
        val out = ByteArray(total)
        var pos = 0
        for (p in parts) {
            System.arraycopy(p, 0, out, pos, p.size)
            pos += p.size
        }
        return out
    }

    /** Skupi chunkove u PNG i izvuci tekst preko backend-a. */
    suspend fun readHiddenText(message: ChatMessage, password: String): Result<String> =
        runCatching {
            val chatId = chatIdFor(message.from, message.to)
            val chunksSnap = db.collection("chats").document(chatId)
                .collection("messages").document(message.id)
                .collection("chunks").orderBy("i").get().await()

            val allBytes = mutableListOf<ByteArray>()
            for (doc in chunksSnap.documents) {
                val b64 = doc.getString("data") ?: continue
                allBytes += Base64.decode(b64, Base64.NO_WRAP)
            }
            val pngBytes = allBytes.flattenToByteArray()

            val part = pngBytes.toPngPart("image", "secret.png")
            val out = api.extractAndDecrypt(part, password.toTextBody())
            out.message
        }
}

private fun FirebaseAuth.Companion.getInstance() {
    TODO("Not yet implemented")
}

/* ------------ helpers ------------- */

private fun ByteArray.chunkedBytes(max: Int): List<ByteArray> {
    if (this.size <= max) return listOf(this)
    val out = ArrayList<ByteArray>()
    var i = 0
    while (i < this.size) {
        val end = min(this.size, i + max)
        out += this.copyOfRange(i, end)
        i = end
    }
    return out
}

private fun List<ByteArray>.flattenToByteArray(): ByteArray {
    val total = this.sumOf { it.size }
    val out = ByteArray(total)
    var pos = 0
    for (arr in this) {
        System.arraycopy(arr, 0, out, pos, arr.size)
        pos += arr.size
    }
    return out
}
