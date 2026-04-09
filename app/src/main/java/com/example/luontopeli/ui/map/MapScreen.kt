package com.example.luontopeli.ui.map

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.luontopeli.viewmodel.MapViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import com.example.luontopeli.viewmodel.WalkViewModel
import com.example.luontopeli.viewmodel.formatDuration
import com.example.luontopeli.viewmodel.formatDistance
import androidx.compose.runtime.mutableLongStateOf
import kotlinx.coroutines.delay
import android.location.Location

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    mapViewModel: MapViewModel = viewModel(),
    walkViewModel: WalkViewModel = viewModel()
) {
    val context = LocalContext.current

    // --- Lupapyynti ---
    val permissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BODY_SENSORS  // Add this
        )
    )

    // ACTIVITY_RECOGNITION tarvitaan Android 10+ askelmittarille
    val activityRecognitionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activityRecognitionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }

    // Näytä lupapyyntö-UI jos luvat puuttuu
    if (!permissionState.allPermissionsGranted) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Sijaintilupa tarvitaan karttaa varten")
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { permissionState.launchMultiplePermissionRequest() }) {
                Text("Myönnä lupa")
            }
        }
        return
    }

    // --- Tila ---
    val isWalking by walkViewModel.isWalking.collectAsState()
    val routePoints by mapViewModel.routePoints.collectAsState()
    val currentLocation by mapViewModel.currentLocation.collectAsState()
    val natureSpots by mapViewModel.natureSpots.collectAsState()
    val routeDistanceMeters by remember(routePoints) {
        derivedStateOf { calculateRouteDistance(routePoints) }
    }

    // Aloita/lopeta sijaintiseuranta kävelyn tilan mukaan
    LaunchedEffect(isWalking) {
        if (isWalking) mapViewModel.startTracking()
        else mapViewModel.stopTracking()
    }

    LaunchedEffect(isWalking, routeDistanceMeters) {
        if (isWalking) {
            walkViewModel.syncDistance(routeDistanceMeters)
        }
    }

    // Oulu oletussijaintina (koordinaatit: lat 65.01, lon 25.47)
    val defaultPosition = GeoPoint(65.0121, 25.4651)

    // Aseta osmdroidin User Agent — PAKOLLINEN, muuten kartta ei lataudu
    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // --- Karttanäkymä ---
        Box(modifier = Modifier.weight(1f)) {

            // remember: MapView-instanssi muistetaan rekompositionien yli
            val mapViewState = remember { MapView(context) }
            val routeOverlay = remember {
                Polyline().apply {
                    outlinePaint.color = 0xFF2E7D32.toInt()
                    outlinePaint.strokeWidth = 8f
                }
            }
            val markersById = remember { mutableMapOf<String, Marker>() }
            var initialCenterDone by remember { mutableStateOf(false) }

            DisposableEffect(mapViewState) {
                // Karttatyyli: MAPNIK = OpenStreetMap-oletustiilet
                mapViewState.setTileSource(TileSourceFactory.MAPNIK)
                // Mahdollista monisormipinch-zoom
                mapViewState.setMultiTouchControls(true)
                mapViewState.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                mapViewState.controller.setZoom(15.0)
                mapViewState.controller.setCenter(
                    currentLocation?.let { GeoPoint(it.latitude, it.longitude) }
                        ?: defaultPosition
                )
                mapViewState.overlays.add(routeOverlay)

                onDispose {
                    // Vapauta resurssit kun Composable poistuu
                    markersById.clear()
                    mapViewState.onDetach()
                }
            }

            AndroidView(
                factory = { mapViewState },
                modifier = Modifier.fillMaxSize(),
                // update kutsutaan kun routePoints, currentLocation tai natureSpots muuttuu
                update = { mapView ->
                    // Päivitä reittipolyline ilman että overlays-lista rakennetaan uusiksi.
                    routeOverlay.setPoints(routePoints)

                    // --- Luontokohteiden markkerit (inkrementaalinen päivitys) ---
                    val latestIds = natureSpots.map { it.id }.toSet()
                    val removedIds = markersById.keys.filterNot { it in latestIds }
                    removedIds.forEach { id ->
                        markersById.remove(id)?.let { mapView.overlays.remove(it) }
                    }

                    natureSpots.forEach { spot ->
                        if (markersById[spot.id] == null) {
                            val marker = Marker(mapView).apply {
                                position = GeoPoint(spot.latitude, spot.longitude)
                                title = spot.plantLabel ?: spot.name
                                snippet = buildString {
                                    spot.description?.takeIf { it.isNotBlank() }?.let {
                                        append(it)
                                        append("\n")
                                    }
                                    append(formatTimestamp(spot.timestamp))
                                }
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            }
                            markersById[spot.id] = marker
                            mapView.overlays.add(marker)
                        }
                    }

                    // --- Seuraa nykyistä sijaintia kevyesti (ei jatkuvaa animaatiota) ---
                    currentLocation?.let { loc ->
                        val target = GeoPoint(loc.latitude, loc.longitude)
                        if (!initialCenterDone) {
                            mapView.controller.setCenter(target)
                            initialCenterDone = true
                        } else if (isWalking) {
                            mapView.controller.setCenter(target)
                        }
                    }

                    mapView.invalidate()  // Piirretään kartta uudelleen
                }
            )
        }

        // --- Kävelytilasto-kortti alareunassa ---
        WalkStatsCard(walkViewModel)
    }
}

@Composable
fun WalkStatsCard(viewModel: WalkViewModel) {
    val session by viewModel.currentSession.collectAsState()
    val isWalking by viewModel.isWalking.collectAsState()

    // Ticker to update time display every second while walking
    var currentTime by remember(session?.startTime) {
        mutableLongStateOf(session?.startTime ?: System.currentTimeMillis())
    }

    LaunchedEffect(isWalking) {
        if (isWalking) {
            // While walking, update every second
            while (isWalking) {
                kotlinx.coroutines.delay(1000)
                currentTime = System.currentTimeMillis()
            }
        } else {
            // When walk ends, use the session's endTime
            session?.endTime?.let {
                currentTime = it
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (isWalking) "Kävely käynnissä" else "Kävely pysäytetty",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            // Näytä tilastot vain jos sessio on olemassa
            session?.let { s ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatTile(
                        value = s.stepCount.toString(),
                        label = "askelta",
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        value = formatDistance(s.distanceMeters),
                        label = "matka",
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        value = formatDuration(s.startTime, currentTime),
                        label = "aika",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                if (!isWalking) {
                    Button(
                        onClick = { viewModel.startWalk() },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        Text("Aloita kävely", style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    OutlinedButton(
                        onClick = { viewModel.stopWalk() },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        Text("Lopeta", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun calculateRouteDistance(points: List<GeoPoint>): Float {
    if (points.size < 2) return 0f

    var distance = 0f
    for (i in 1 until points.size) {
        val result = FloatArray(1)
        Location.distanceBetween(
            points[i - 1].latitude,
            points[i - 1].longitude,
            points[i].latitude,
            points[i].longitude,
            result
        )
        distance += result[0]
    }
    return distance
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}