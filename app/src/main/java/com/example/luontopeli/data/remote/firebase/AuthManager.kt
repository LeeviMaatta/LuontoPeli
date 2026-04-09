package com.example.luontopeli.data.remote.firebase

import com.google.firebase.auth.FirebaseAuth
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class AuthManager {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    /** Palauttaa nykyisen käyttäjän tunnisteen */
    val currentUserId: String
        get() = auth.currentUser?.uid.orEmpty()

    /** Onko käyttäjä kirjautunut Firebaseen. */
    val isSignedIn: Boolean
        get() = auth.currentUser != null

    /**
     * Kirjautuu anonyymisti Firebaseen.
     * Jos käyttäjä on jo kirjautunut, palauttaa nykyisen uid:n.
     */
    suspend fun signInAnonymously(): Result<String> {
        if (isSignedIn) {
            return Result.success(currentUserId)
        }

        return try {
            val result = suspendCancellableCoroutine<com.google.firebase.auth.AuthResult> { cont ->
                auth.signInAnonymously()
                    .addOnSuccessListener { authResult -> cont.resume(authResult) }
                    .addOnFailureListener { error -> cont.resumeWithException(error) }
            }
            Result.success(result.user?.uid.orEmpty())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Uloskirjautuminen Firebase Authista. */
    fun signOut() {
        auth.signOut()
    }
}
