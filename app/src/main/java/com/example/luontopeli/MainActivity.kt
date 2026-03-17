package com.example.luontopeli

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.luontopeli.ui.navigation.LuontopeliBottomBar
import com.example.luontopeli.ui.navigation.LuontopeliNavHost
import com.example.luontopeli.ui.theme.LuontopeliTheme
import dagger.hilt.android.AndroidEntryPoint

// @AndroidEntryPoint aktivoi Hilt-riippuvuusinjektoin tässä Activityssa
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LuontopeliTheme {
                LuontopeliApp()
            }
        }
    }
}

@Composable
fun LuontopeliApp() {
    // rememberNavController: NavController muistetaan rekomposition yli
    val navController = rememberNavController()

    Scaffold(
        // Alanavigaatiopalkki kaikilla näkymillä
        bottomBar = {
            LuontopeliBottomBar(navController = navController)
        }
    ) { innerPadding ->
        // NavHost täyttää koko tilan, padding estää sisällön menemisen bottom bar -alle
        LuontopeliNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding)  // vältetään sisällön piiloutuminen BottomBarin alle
        )
    }
}