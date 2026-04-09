package com.example.luontopeli.data.remote.firebase

import com.example.luontopeli.data.local.entity.NatureSpot
import com.example.luontopeli.data.local.entity.WalkSession
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class FirestoreManager {

    private val firestore = FirebaseFirestore.getInstance()
    private val spotsCollection = firestore.collection("nature_spots")
    private val walkSessionsCollection = firestore.collection("walk_sessions")

    /**
     * Tallentaa luontolöydön Firestoreen polkuun nature_spots/{spotId}.
     */
    suspend fun saveSpot(spot: NatureSpot): Result<Unit> {
        return try {
            val payload = hashMapOf(
                "id" to spot.id,
                "name" to spot.name,
                "latitude" to spot.latitude,
                "longitude" to spot.longitude,
                "plantLabel" to spot.plantLabel,
                "confidence" to spot.confidence,
                "description" to (spot.description ?: ""),
                "imageFirebaseUrl" to spot.imageFirebaseUrl,
                "userId" to spot.userId,
                "timestamp" to spot.timestamp
            )

            suspendCancellableCoroutine<Unit> { cont ->
                spotsCollection.document(spot.id)
                    .set(payload)
                    .addOnSuccessListener { cont.resume(Unit) }
                    .addOnFailureListener { error -> cont.resumeWithException(error) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Hakee käyttäjän löydöt Firestoresta reaaliaikaisena virtana.
     */
    fun getUserSpots(userId: String): Flow<List<NatureSpot>> {
        return callbackFlow {
            val registration = spotsCollection
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }

                    val spots = snapshot?.documents?.mapNotNull { doc ->
                        val id = doc.getString("id") ?: return@mapNotNull null
                        val name = doc.getString("name") ?: return@mapNotNull null
                        val latitude = doc.getDouble("latitude") ?: return@mapNotNull null
                        val longitude = doc.getDouble("longitude") ?: return@mapNotNull null
                        val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

                        NatureSpot(
                            id = id,
                            name = name,
                            latitude = latitude,
                            longitude = longitude,
                            plantLabel = doc.getString("plantLabel"),
                            confidence = doc.getDouble("confidence")?.toFloat(),
                            description = doc.getString("description"),
                            imageFirebaseUrl = doc.getString("imageFirebaseUrl"),
                            userId = doc.getString("userId"),
                            timestamp = timestamp,
                            synced = true
                        )
                    } ?: emptyList()

                    trySend(spots)
                }

            awaitClose { registration.remove() }
        }
    }

    /**
     * Tallentaa kävelysession Firestoreen polkuun walk_sessions/{sessionId}.
     */
    suspend fun saveWalkSession(session: WalkSession): Result<Unit> {
        return try {
            val payload = hashMapOf(
                "id" to session.id,
                "startTime" to session.startTime,
                "endTime" to (session.endTime ?: System.currentTimeMillis()),
                "stepCount" to session.stepCount,
                "distanceMeters" to session.distanceMeters,
                "spotsFound" to session.spotsFound,
                "isActive" to session.isActive,
                "userId" to session.userId
            )

            suspendCancellableCoroutine<Unit> { cont ->
                walkSessionsCollection.document(session.id)
                    .set(payload)
                    .addOnSuccessListener { cont.resume(Unit) }
                    .addOnFailureListener { error -> cont.resumeWithException(error) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Hakee käyttäjän kävelysessiot Firestoresta reaaliaikaisena virtana.
     */
    fun getUserWalkSessions(userId: String): Flow<List<WalkSession>> {
        return callbackFlow {
            val registration = walkSessionsCollection
                .whereEqualTo("userId", userId)
                .orderBy("startTime", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }

                    val sessions = snapshot?.documents?.mapNotNull { doc ->
                        val id = doc.getString("id") ?: return@mapNotNull null
                        val startTime = doc.getLong("startTime") ?: return@mapNotNull null
                        val endTime = doc.getLong("endTime")
                        val stepCount = (doc.get("stepCount") as? Number)?.toInt() ?: 0
                        val distanceMeters = (doc.get("distanceMeters") as? Number)?.toFloat() ?: 0f
                        val spotsFound = (doc.get("spotsFound") as? Number)?.toInt() ?: 0
                        val isActive = doc.getBoolean("isActive") ?: false
                        val remoteUserId = doc.getString("userId")

                        WalkSession(
                            id = id,
                            startTime = startTime,
                            endTime = endTime,
                            stepCount = stepCount,
                            distanceMeters = distanceMeters,
                            spotsFound = spotsFound,
                            isActive = isActive,
                            userId = remoteUserId
                        )
                    } ?: emptyList()

                    trySend(sessions)
                }

            awaitClose { registration.remove() }
        }
    }
}
