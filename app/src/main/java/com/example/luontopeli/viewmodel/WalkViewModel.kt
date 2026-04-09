package com.example.luontopeli.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.luontopeli.data.local.AppDatabase
import com.example.luontopeli.data.local.entity.WalkSession
import com.example.luontopeli.data.remote.firebase.AuthManager
import com.example.luontopeli.data.remote.firebase.FirestoreManager
import com.example.luontopeli.sensor.StepCounterManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * ViewModel kävelyn hallintaan.
 * Hallinnoi kävelylenkin aloittamista, askelten laskentaa ja lopetusta.
 * Jaettu MapScreen:n kanssa.
 */
class WalkViewModel(application: Application) : AndroidViewModel(application) {

    /** Askelmittarin hallintapalvelu (STEP_DETECTOR-sensori) */
    private val stepManager = StepCounterManager(application)

    /** Room-tietokantainstanssi kävelykertojen tallentamiseen */
    private val db = AppDatabase.getDatabase(application)

    /** Firebase-hallinta */
    private val authManager = AuthManager()
    private val firestoreManager = FirestoreManager()

    /** Nykyinen kävelykerta (null jos kävely ei ole käynnissä) */
    private val _currentSession = MutableStateFlow<WalkSession?>(null)
    val currentSession: StateFlow<WalkSession?> = _currentSession.asStateFlow()

    /** Kaikki tallennetut kävelykerrat tilastonäkymää varten */
    private val _sessions = MutableStateFlow<List<WalkSession>>(emptyList())
    val sessions: StateFlow<List<WalkSession>> = _sessions.asStateFlow()

    /** Onko kävely parhaillaan käynnissä */
    private val _isWalking = MutableStateFlow(false)
    val isWalking: StateFlow<Boolean> = _isWalking.asStateFlow()

    init {
        viewModelScope.launch {
            db.walkSessionDao().getAllSessions().collect { allSessions ->
                _sessions.value = allSessions
            }
        }

        viewModelScope.launch {
            backfillMissingSessionsFromFirestore()
        }
    }

    private suspend fun ensureSignedInAndGetUserId(): String {
        if (!authManager.isSignedIn) {
            authManager.signInAnonymously().getOrElse { throwable ->
                throw throwable
            }
        }
        return authManager.currentUserId.orEmpty()
    }

    private suspend fun backfillMissingSessionsFromFirestore() {
        val userId = ensureSignedInAndGetUserId()
        if (userId.isBlank()) return

        val remoteSessions = firestoreManager.getUserWalkSessions(userId).first()
        val localSessionIds = db.walkSessionDao().getAllSessions().first().map { it.id }.toSet()
        val missingSessions = remoteSessions.filter { it.id !in localSessionIds }

        missingSessions.forEach { session ->
            db.walkSessionDao().insert(session.copy(userId = userId))
        }
    }

    /**
     * Aloittaa uuden kävelylenkin.
     * 1. Luo uuden WalkSession-olion
     * 2. Asettaa tilan aktiiviseksi
     * 3. Käynnistää askelmittarin
     */
    fun startWalk() {
        // Estä päällekkäiset käynnistykset
        if (_isWalking.value) return

        val session = WalkSession()
        _currentSession.value = session
        _isWalking.value = true

        // Käynnistä askelmittari vain jos laite tukee sitä.
        // Matka synkronoidaan GPS-reitistä syncDistance()-kutsulla.
        if (stepManager.isStepSensorAvailable()) {
            stepManager.startStepCounting {
                _currentSession.update { current ->
                    current?.copy(stepCount = current.stepCount + 1)
                }
            }
        }
    }

    /**
     * Synkronoi GPS-reitistä lasketun kokonaismatkan aktiiviseen sessioon.
     * Päivittää samalla askelmäärän vähintään matkaan perustuvaan arvioon.
     */
    fun syncDistance(distanceMeters: Float) {
        if (!_isWalking.value) return

        _currentSession.update { current ->
            current?.let {
                val estimatedSteps = (distanceMeters / StepCounterManager.STEP_LENGTH_METERS).toInt()
                it.copy(
                    distanceMeters = distanceMeters,
                    stepCount = max(it.stepCount, estimatedSteps)
                )
            }
        }
    }
    /**
     * Lopettaa käynnissä olevan kävelylenkin.
     * 1. Pysäyttää askelmittarin
     * 2. Merkitsee kävelykerran päättyneeksi
     * 3. Tallentaa kävelykerran tietokantaan
     * 4. Synkronoi kävelykerran Firebaseen
     */
    fun stopWalk() {
        stepManager.stopStepCounting()
        _isWalking.value = false

        // Tallenna valmistunut kävely tietokantaan ja Firebaseen
        viewModelScope.launch {
            val userId = ensureSignedInAndGetUserId()

            _currentSession.value?.let { session ->
                val endTime = System.currentTimeMillis()
                val spotsFound = db.natureSpotDao().countSpotsInRange(session.startTime, endTime)

                val completedSession = session.copy(
                    endTime = endTime,
                    spotsFound = spotsFound,
                    isActive = false,
                    userId = userId
                )

                _currentSession.value = completedSession

                db.walkSessionDao().insert(completedSession)
                // Synkronoi Firestoreen
                firestoreManager.saveWalkSession(completedSession)
            }
        }
    }

    /** Vapauttaa sensorien resurssit ViewModelin tuhoutuessa. */
    override fun onCleared() {
        super.onCleared()
        stepManager.stopAll()
    }
}

/**
 * Muotoilee matkan metreinä luettavaan muotoon.
 * Alle 1000 m näytetään metreinä, muuten kilometreinä.
 * @param meters Matka metreinä
 * @return Muotoiltu merkkijono
 */
fun formatDistance(meters: Float): String {
    return if (meters < 1000f) {
        "${meters.toInt()} m"
    } else {
        "${"%.1f".format(meters / 1000f)} km"
    }
}

/**
 * Muotoilee keston aloitus- ja lopetusajan perusteella.
 * @param startTime Aloitusaika millisekunteina
 * @param endTime Lopetusaika millisekunteina
 * @return Muotoiltu kesto
 */
fun formatDuration(startTime: Long, endTime: Long = System.currentTimeMillis()): String {
    val seconds = (endTime - startTime) / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    return when {
        hours > 0 -> "${hours}h ${minutes % 60}min"
        minutes > 0 -> "${minutes}min ${seconds % 60}s"
        else -> "${seconds}s"
    }
}