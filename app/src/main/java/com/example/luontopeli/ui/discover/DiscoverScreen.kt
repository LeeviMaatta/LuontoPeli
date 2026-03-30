package com.example.luontopeli.ui.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Park           // ← replaces NatureOutlined
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.luontopeli.data.local.entity.NatureSpot
import com.example.luontopeli.viewmodel.toFormattedDate
import java.io.File

@Composable
fun DiscoverScreen(viewModel: DiscoverViewModel = viewModel()) {
    val spots by viewModel.allSpots.collectAsState()

    if (spots.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Park, null,           // ← was NatureOutlined
                    modifier = Modifier.size(64.dp), tint = Color.Gray
                )
                Text("Ei löytöjä vielä", modifier = Modifier.padding(8.dp))
                Text(
                    "Ota kuva kasveista kameralla!",
                    style = MaterialTheme.typography.bodySmall, color = Color.Gray
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "${spots.size} löytöä",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(spots, key = { it.id }) { spot ->
                NatureSpotCard(spot = spot)
            }
        }
    }
}

@Composable
fun NatureSpotCard(spot: NatureSpot) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            // Kuva vasemmalla
            val imageModel = spot.imageFirebaseUrl ?: spot.imageLocalPath?.let { File(it) }
            if (imageModel != null) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = spot.plantLabel,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Park, null)      // ← was NatureOutlined
                }
            }

            Spacer(Modifier.width(12.dp))

            // Tiedot oikealla
            Column(modifier = Modifier.weight(1f)) {
                // Kasvilaji + synkronointimerkki
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = spot.plantLabel ?: "Tuntematon",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    if (spot.synced) {
                        Icon(
                            Icons.Default.Cloud, null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            Icons.Default.CloudOff, null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.Gray
                        )
                    }
                }

                // Varmuusprosentti
                spot.confidence?.let { conf ->
                    Text(
                        text = "${"%.0f".format(conf * 100)}% varmuus",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (conf > 0.8f) Color(0xFF2E7D32) else Color.Gray
                    )
                }

                // Päivämäärä
                Text(
                    text = spot.timestamp.toFormattedDate(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}