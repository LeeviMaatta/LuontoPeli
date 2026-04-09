package com.example.luontopeli.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.luontopeli.ui.map.MapScreen
//import com.example.luontopeli.camera.CameraScreen
import com.example.luontopeli.ui.discover.DiscoverScreen
import com.example.luontopeli.ui.stats.StatsScreen
import com.example.luontopeli.camera.CameraScreen

@Composable
fun LuontopeliNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier     // Modifier parametri padding-tukea varten
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Map.route,
        modifier = modifier
    ) {
        // Jokainen composable-lohko vastaa yhtä reittiä
        composable(Screen.Map.route) {
            MapScreen()
        }
        composable(Screen.Camera.route) {
            CameraScreen()
        }
        composable(Screen.Discover.route) {
            DiscoverScreen()
        }
        composable(Screen.Stats.route) {
            StatsScreen()
        }
    }
}