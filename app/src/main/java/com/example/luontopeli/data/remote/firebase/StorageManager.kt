package com.example.luontopeli.data.remote.firebase

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File

class StorageManager {

    private val storage = FirebaseStorage.getInstance()

    /**
     * Lataa kuvan Firebase Storageen polkuun nature_spots/{spotId}.jpg.
     * Palauttaa gs://-muotoisen URL:n.
     */
    suspend fun uploadImage(localFilePath: String, spotId: String): Result<String> {
        return try {
            val localFile = File(localFilePath)
            if (!localFile.exists()) {
                return Result.failure(IllegalArgumentException("Image file not found: $localFilePath"))
            }

            val ref = storage.reference.child("nature_spots/$spotId.jpg")
            val uri = Uri.fromFile(localFile)

            suspendCancellableCoroutine<Unit> { cont ->
                ref.putFile(uri)
                    .addOnSuccessListener { cont.resume(Unit) }
                    .addOnFailureListener { error -> cont.resumeWithException(error) }
            }

            Result.success("gs://${ref.bucket}/${ref.path.trimStart('/')}")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Poistaa kuvan pilvipalvelusta. */
    suspend fun deleteImage(spotId: String): Result<Unit> {
        return try {
            val ref = storage.reference.child("nature_spots/$spotId.jpg")
            suspendCancellableCoroutine<Unit> { cont ->
                ref.delete()
                    .addOnSuccessListener { cont.resume(Unit) }
                    .addOnFailureListener { error -> cont.resumeWithException(error) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}