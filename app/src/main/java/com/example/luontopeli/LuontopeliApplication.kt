package com.example.luontopeli

import android.app.Application
import com.example.luontopeli.data.remote.firebase.AuthManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// @HiltAndroidApp generoi Hilt-koodin — pakollinen Hiltin käytölle
@HiltAndroidApp
class LuontopeliApplication : Application() {

	private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

	override fun onCreate() {
		super.onCreate()

		// Käynnistä anonyymi kirjautuminen heti sovelluksen alussa.
		appScope.launch {
			AuthManager().signInAnonymously()
		}
	}
}