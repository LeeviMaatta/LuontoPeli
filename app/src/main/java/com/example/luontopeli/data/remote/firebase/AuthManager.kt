package com.example.luontopeli.data.remote.firebase

/**
 * Offline-tilassa toimiva käyttäjähallinta (AuthManager).
 * Korvaa alkuperäisen Firebase Authentication -toteutuksen
 * paikallisella UUID-pohjaisella käyttäjätunnisteella.
 */
class AuthManager {
    /** Paikallisesti generoitu uniikki käyttäjätunniste (UUID) */
    private val localUserId: String = java.util.UUID.randomUUID().toString()

    /** Palauttaa nykyisen käyttäjän tunnisteen */
    val currentUserId: String
        get() = localUserId

    /** Offline-tilassa käyttäjä on aina "kirjautunut sisään" */
    val isSignedIn: Boolean
        get() = true

    /** Simuloi anonyymiä kirjautumista. Palauttaa aina onnistuneen tuloksen paikallisella UUID:lla. */
    suspend fun signInAnonymously(): Result<String> {
        return Result.success(localUserId)
    }

    /** Uloskirjautuminen – ei toiminnallisuutta offline-tilassa. */
    fun signOut() {
        // No-op offline-tilassa
    }
}
