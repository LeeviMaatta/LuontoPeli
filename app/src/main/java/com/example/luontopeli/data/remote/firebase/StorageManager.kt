package com.example.luontopeli.data.remote.firebase

/**
 * Offline-tilassa toimiva tallennushallinta (no-op -toteutus).
 * Korvaa alkuperäisen Firebase Storage -toteutuksen.
 */
class StorageManager {

    /**
     * Simuloi kuvan lataamista pilvipalveluun.
     * Offline-tilassa palauttaa paikallisen tiedostopolun.
     */
    suspend fun uploadImage(localFilePath: String, spotId: String): Result<String> {
        return Result.success(localFilePath)
    }

    /** Simuloi kuvan poistamista pilvipalvelusta. Offline-tilassa ei tee mitään. */
    suspend fun deleteImage(spotId: String): Result<Unit> {
        return Result.success(Unit)
    }
}