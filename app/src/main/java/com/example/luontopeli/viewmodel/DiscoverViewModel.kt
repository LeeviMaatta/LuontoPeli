package com.example.luontopeli.ui.discover

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.luontopeli.data.local.AppDatabase
import com.example.luontopeli.data.local.entity.NatureSpot
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted

class DiscoverViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).natureSpotDao()

    val allSpots: StateFlow<List<NatureSpot>> = dao.getAllSpots()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}