package com.example.meowtronic

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CaptureActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private var isCapturing = false
    private var frameCount = 0
    private val captureIntervalMs = 10L
    private val captureTotalTimeMs = 10_000L
    private var captureStartTime: Long = 0

    private val CAMERA_REQUEST_CODE = 1234
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var cameraExecutor: ExecutorService

    // Where we store captured file paths
    private val capturedFilePaths = arrayListOf<String>()

    // Directory for images
    private lateinit var tempDir: File

    // Change this to your server’s IP/port or domain
    private val serverUrl = "http://192.168.90.147:5000/upload"

    private val captureRunnable = object : Runnable {
        override fun run() {
            if (!isCapturing) return
            val elapsed = System.currentTimeMillis() - captureStartTime
            if (elapsed > captureTotalTimeMs) {
                // After 10s -> stop capturing
                finishCapturing()
            } else {
                takePhoto()
                handler.postDelayed(this, captureIntervalMs)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture)

        // Check camera permission
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
        }

        // Find UI views
        previewView = findViewById(R.id.previewView)
        findViewById<android.widget.Button>(R.id.btnStart).setOnClickListener {
            startCapturing()
        }
        findViewById<android.widget.Button>(R.id.btnPause).setOnClickListener {
            pauseCapturing()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Prepare temp directory
        tempDir = File(cacheDir, "tempCaptures").apply {
            if (!exists()) mkdirs()
        }
    }

    override fun onResume() {
        super.onResume()
        if (checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED && cameraProvider == null) {
            startCamera()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_REQUEST_CODE && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            finish()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e("CaptureActivity", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun unbindCamera() {
        cameraProvider?.unbindAll()
        cameraProvider = null
    }

    private fun startCapturing() {
        if (!isCapturing) {
            isCapturing = true
            frameCount = 0
            captureStartTime = System.currentTimeMillis()
            handler.post(captureRunnable)
        }
    }

    private fun pauseCapturing() {
        if (isCapturing) {
            isCapturing = false
            handler.removeCallbacks(captureRunnable)
            unbindCamera()
        }
    }

    private fun finishCapturing() {
        isCapturing = false
        handler.removeCallbacks(captureRunnable)
        unbindCamera()


        sendImagesToServer(capturedFilePaths)

        // Optionally: close activity
        finish()
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US)
            .format(System.currentTimeMillis())
        val photoFile = File(tempDir, "frame_$timeStamp.jpg")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        if(isCapturing){
            imageCapture.takePicture(
                outputOptions,
                cameraExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
                        Log.e("CaptureActivity", "Photo capture failed: ${exc.message}", exc)
                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        frameCount++
                        capturedFilePaths.add(photoFile.absolutePath)
                        Log.d("CaptureActivity", "Photo captured: ${photoFile.absolutePath}")
                    }
                }
            )
        }

    }

    /**
     * Sends each image via multipart POST to the server.
     * If successful, optionally delete the local file.
     */
    private fun sendImagesToServer(filePaths: List<String>) {
        // OkHttp client
        val client = OkHttpClient()

        // For each file, do a POST
        for (path in filePaths) {
            val file = File(path)
            if (!file.exists()) continue

            val requestBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val multipartBody = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("image", file.name, requestBody)
                .build()

            val request = Request.Builder()
                .url(serverUrl)  // e.g. "http://192.168.0.42:5000/upload"
                .post(multipartBody)
                .build()

            // You might want to do this off the main thread
            // but for simplicity let's just do it in the same thread
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    Log.d("CaptureActivity", "Uploaded file: ${file.name}")
                    // Optionally delete local file on success:
                    // file.delete()
                } else {
                    Log.e("CaptureActivity", "Upload error ${response.code}")
                }
            } catch (e: Exception) {
                Log.e("CaptureActivity", "Upload failed: ${e.message}", e)
            }
        }

        // Optionally, if all done, remove the entire tempDir
        // tempDir.deleteRecursively()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        handler.removeCallbacks(captureRunnable)
    }
}
