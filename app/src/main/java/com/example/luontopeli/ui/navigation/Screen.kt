package com.example.luontopeli.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

// Sealed class kuvaa kaikki sovelluksen reitit
// Sealed class = rajoitettu periytyminen, kaikki aliluokat tunnetaan käännösaikana
sealed class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    // Karttanäkymä: GPS-reitti ja havaintojen sijainnit
    object Map : Screen("map", "Kartta", Icons.Filled.Map)
    // Kameranäkymä: CameraX-esikatselu + kuvaaminen
    object Camera : Screen("camera", "Kamera", Icons.Filled.CameraAlt)
    // Löydöt: muiden käyttäjien havainnot Firebasesta
    object Discover : Screen("discover", "Löydöt", Icons.Filled.Explore)
    // Tilastot: askeleet, matka, omat havainnot
    object Stats : Screen("stats", "Tilastot", Icons.Filled.BarChart)

    companion object {
        // Lista kaikista bottom nav -kohteista
        val bottomNavScreens = listOf(Map, Camera, Discover, Stats)
    }
}