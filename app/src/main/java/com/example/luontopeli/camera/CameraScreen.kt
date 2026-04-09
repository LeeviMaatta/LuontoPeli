package com.example.luontopeli.camera

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager as AndroidLocationManager
import android.util.Log
import android.view.ScaleGestureDetector
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.luontopeli.ml.ClassificationResult
import com.example.luontopeli.viewmodel.CameraViewModel
import java.io.File

@Composable
fun CameraScreen(viewModel: CameraViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // State for camera readiness
    var isCameraReady by remember { mutableStateOf(false) }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    val capturedImagePath by viewModel.capturedImagePath.collectAsState()
    val classificationResult by viewModel.classificationResult.collectAsState()
    val description by viewModel.description.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            getLastKnownLocation(context)?.let { location ->
                viewModel.setCurrentLocation(location.latitude, location.longitude)
            }
        }
    }

    if (!hasCameraPermission) {
        PermissionDeniedView { permissionLauncher.launch(Manifest.permission.CAMERA) }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (capturedImagePath == null) {
            // Camera Preview logic
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    var boundCamera: Camera? = null

                    val scaleGestureDetector = ScaleGestureDetector(ctx,
                        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                            override fun onScale(detector: ScaleGestureDetector): Boolean {
                                val camera = boundCamera ?: return false
                                val zoomState = camera.cameraInfo.zoomState.value ?: return false
                                val newZoomRatio = (zoomState.zoomRatio * detector.scaleFactor)
                                    .coerceIn(zoomState.minZoomRatio, zoomState.maxZoomRatio)
                                camera.cameraControl.setZoomRatio(newZoomRatio)
                                return true
                            }
                        }
                    )

                    previewView.setOnTouchListener { _, event ->
                        scaleGestureDetector.onTouchEvent(event)
                        true
                    }

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        try {
                            cameraProvider.unbindAll()
                            boundCamera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageCapture
                            )
                            isCameraReady = true
                        } catch (e: Exception) {
                            Log.e("Camera", "Binding failed", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Capture Button Overlay
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                FloatingActionButton(
                    onClick = { if (isCameraReady && !isLoading) viewModel.takePhoto(context, imageCapture) },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.Camera, "Ota kuva", tint = Color.White)
                    }
                }
            }
        } else {
            // Show the result screen
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) {
                    CapturedImageView(
                        imagePath = capturedImagePath!!,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Show classification result card on top of the image
                    Box(
                        modifier = Modifier.fillMaxSize().padding(bottom = 16.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        classificationResult?.let { ClassificationResultCard(it) }
                    }
                }

                // Description Input
                if (classificationResult is ClassificationResult.Success) {
                    TextField(
                        value = description,
                        onValueChange = { viewModel.updateDescription(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        label = { Text("Kuvaus (valinnainen)") },
                        singleLine = false,
                        maxLines = 3,
                        placeholder = { Text("Lisää kuvaus löydöstä...") }
                    )
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(onClick = { viewModel.clearCapturedImage() }) {
                        Icon(Icons.Default.Refresh, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Ota uudelleen")
                    }
                    Button(
                        onClick = { viewModel.saveCurrentSpot() },
                        enabled = classificationResult is ClassificationResult.Success && !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Save, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Tallenna löytö")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionDeniedView(onRequestPermission: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(64.dp), tint = Color.Gray)
            Text("Kameran lupa tarvitaan", modifier = Modifier.padding(8.dp))
            Button(onClick = onRequestPermission) { Text("Myönnä lupa") }
        }
    }
}

@Composable
fun CapturedImageView(imagePath: String, modifier: Modifier = Modifier) {
    AsyncImage(
        model = File(imagePath),
        contentDescription = "Otettu kuva",
        contentScale = ContentScale.Fit,
        modifier = modifier.background(Color.Black)
    )
}

@Composable
fun ClassificationResultCard(result: ClassificationResult) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            when (result) {
                is ClassificationResult.Success -> {
                    Text("Tunnistettu:", style = MaterialTheme.typography.labelLarge)
                    Text(result.label, style = MaterialTheme.typography.headlineSmall)
                    LinearProgressIndicator(
                        progress = { result.confidence },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(8.dp).clip(CircleShape)
                    )
                }
                is ClassificationResult.NotNature -> Text("Ei luontokohde")
                is ClassificationResult.Error -> Text("Virhe: ${result.message}")
            }
        }
    }
}

private fun getLastKnownLocation(context: android.content.Context): Location? {
    val hasFine = ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val hasCoarse = ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    if (!hasFine && !hasCoarse) return null

    val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as AndroidLocationManager
    val providers = listOf(
        AndroidLocationManager.GPS_PROVIDER,
        AndroidLocationManager.NETWORK_PROVIDER,
        AndroidLocationManager.PASSIVE_PROVIDER
    )

    return providers
        .mapNotNull { provider -> runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() }
        .maxByOrNull { it.time }
}