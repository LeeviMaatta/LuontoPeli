package com.example.luontopeli.ui.navigation

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun LuontopeliBottomBar(navController: NavController) {
    // currentBackStackEntryAsState: seuraa aktiivista reittiä reaaliajassa
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        Screen.bottomNavScreens.forEach { screen ->
            NavigationBarItem(
                // Onko tämä reitti aktiivinen?
                selected = currentRoute == screen.route,
                // Navigoi painettaessa
                onClick = {
                    navController.navigate(screen.route) {
                        // Tyhjennä back stack etusivulle asti — ei kasata navigaatiohistoriaa
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.label
                    )
                },
                label = { Text(screen.label) }
            )
        }
    }
}