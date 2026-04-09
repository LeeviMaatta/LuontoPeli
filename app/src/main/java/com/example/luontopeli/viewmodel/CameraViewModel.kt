package com.example.luontopeli.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.luontopeli.data.local.AppDatabase
import com.example.luontopeli.data.local.entity.NatureSpot
import com.example.luontopeli.data.remote.firebase.AuthManager
import com.example.luontopeli.data.remote.firebase.FirestoreManager
import com.example.luontopeli.data.remote.firebase.StorageManager
import com.example.luontopeli.data.repository.NatureSpotRepository
import com.example.luontopeli.ml.ClassificationResult
import com.example.luontopeli.ml.PlantClassifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = NatureSpotRepository(
        dao = db.natureSpotDao(),
        firestoreManager = FirestoreManager(),
        storageManager = StorageManager(),
        authManager = AuthManager()
    )

    private val classifier = PlantClassifier()

    private val _capturedImagePath = MutableStateFlow<String?>(null)
    val capturedImagePath = _capturedImagePath.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _classificationResult = MutableStateFlow<ClassificationResult?>(null)
    val classificationResult = _classificationResult.asStateFlow()

    private val _description = MutableStateFlow("")
    val description = _description.asStateFlow()

    var currentLatitude: Double = 0.0
    var currentLongitude: Double = 0.0

    fun setCurrentLocation(latitude: Double, longitude: Double) {
        currentLatitude = latitude
        currentLongitude = longitude
    }

    fun updateDescription(text: String) {
        _description.value = text
    }

    fun takePhoto(context: Context, imageCapture: ImageCapture) {
        _isLoading.value = true

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val outputDir = File(context.filesDir, "nature_photos").apply { if (!exists()) mkdirs() }
        val outputFile = File(outputDir, "IMG_$timestamp.jpg")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    _capturedImagePath.value = outputFile.absolutePath
                    classifyImage(context, outputFile)
                }

                override fun onError(exception: ImageCaptureException) {
                    _isLoading.value = false
                    _classificationResult.value = ClassificationResult.Error("Kameravirhe: ${exception.message}")
                }
            }
        )
    }

    private fun classifyImage(context: Context, file: File) {
        viewModelScope.launch {
            try {
                val result = classifier.classify(Uri.fromFile(file), context)
                _classificationResult.value = result
            } catch (e: Exception) {
                _classificationResult.value = ClassificationResult.Error(e.message ?: "Tunnistusvirhe")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveCurrentSpot() {
        if (_isLoading.value) return

        val path = _capturedImagePath.value ?: return
        val result = _classificationResult.value as? ClassificationResult.Success ?: return

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val spot = NatureSpot(
                    name = result.label,
                    latitude = currentLatitude,
                    longitude = currentLongitude,
                    imageLocalPath = path,
                    plantLabel = result.label,
                    confidence = result.confidence.toFloat(),
                    description = _description.value.ifBlank { null }
                )
                repository.insertSpot(spot)
                clearCapturedImage()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearCapturedImage() {
        _capturedImagePath.value = null
        _classificationResult.value = null
        _description.value = ""
        _isLoading.value = false
    }

    override fun onCleared() {
        super.onCleared()
        classifier.close()
    }
}