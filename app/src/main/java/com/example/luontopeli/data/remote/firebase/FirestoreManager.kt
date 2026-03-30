package com.example.luontopeli.data.remote.firebase

import com.example.luontopeli.data.local.entity.NatureSpot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Offline-tilassa toimiva Firestore-hallinta (no-op -toteutus).
 * Korvaa alkuperäisen Firebase Firestore -toteutuksen tyhjillä operaatioilla.
 */
class FirestoreManager {

    /**
     * Simuloi luontolöydön tallennusta Firestoreen.
     * Offline-tilassa ei tee mitään.
     */
    suspend fun saveSpot(spot: NatureSpot): Result<Unit> {
        return Result.success(Unit)
    }

    /**
     * Simuloi käyttäjän löytöjen hakemista Firestoresta.
     * Palauttaa tyhjän listan.
     */
    fun getUserSpots(userId: String): Flow<List<NatureSpot>> {
        return flowOf(emptyList())
    }
}
