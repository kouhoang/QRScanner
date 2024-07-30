package com.example.qrscanner

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalGetImage::class)
class MainActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var captureButton: Button
    private lateinit var libraryButton: Button
    private lateinit var imageCapture: ImageCapture
    private lateinit var barcodeScanner: BarcodeScanner

    private val REQUEST_CODE_PERMISSIONS = 1001
    private val REQUIRED_PERMISSIONS =
        arrayOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        )

    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        captureButton = findViewById(R.id.captureButton)
        libraryButton = findViewById(R.id.library)

        barcodeScanner = BarcodeScanning.getClient()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        captureButton.setOnClickListener { takePhoto() }

        pickImageLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data?.data?.let { uri ->
                        scanImage(uri)
                    }
                }
            }

        libraryButton.setOnClickListener { openImagePicker() }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(
            Runnable {
                val cameraProvider = cameraProviderFuture.get()

                val preview =
                    Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                imageCapture = ImageCapture.Builder().build()

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                val analysisUseCase =
                    ImageAnalysis.Builder().build().also {
                        it.setAnalyzer(ContextCompat.getMainExecutor(this), BarcodeAnalyzer())
                    }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, analysisUseCase)
            },
            ContextCompat.getMainExecutor(this),
        )
    }

    private inner class BarcodeAnalyzer : ImageAnalysis.Analyzer {
        @androidx.annotation.OptIn(ExperimentalGetImage::class)
        override fun analyze(image: ImageProxy) {
            val mediaImage = image.image
            if (mediaImage != null) {
                val inputImage = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
                barcodeScanner
                    .process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        for (barcode in barcodes) {
                            val url = barcode.rawValue
                            if (url != null && url.startsWith("http")) {
                                // Open URL
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                startActivity(intent)
                            } else {
                                // Show QR content
                                Toast.makeText(this@MainActivity, url, Toast.LENGTH_LONG).show()
                            }
                        }
                    }.addOnFailureListener { e ->
                        // Handle error
                        e.printStackTrace()
                    }.addOnCompleteListener {
                        image.close() // Important to close the image to avoid memory leaks
                    }
            }
        }
    }

    private fun takePhoto() {
        val photoFile = createFile()
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    scanImage(Uri.fromFile(photoFile))
                }

                override fun onError(exception: ImageCaptureException) {
                    // Handle error
                    exception.printStackTrace()
                }
            },
        )
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

    private fun scanImage(uri: Uri) {
        val image = InputImage.fromFilePath(this, uri)

        barcodeScanner
            .process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val url = barcode.rawValue
                    if (url != null && url.startsWith("http")) {
                        // Open URL
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                    } else {
                        // Show QR content
                        Toast.makeText(this, url, Toast.LENGTH_LONG).show()
                    }
                }
            }.addOnFailureListener { e ->
                try {
                    throw e
                } catch (ex: IOException) {
                    // Handle IO Exception
                    Toast.makeText(this, "IO Error: ${ex.localizedMessage}", Toast.LENGTH_LONG).show()
                    ex.printStackTrace()
                } catch (ex: SecurityException) {
                    // Handle Security Exception
                    Toast.makeText(this, "Security Error: ${ex.localizedMessage}", Toast.LENGTH_LONG).show()
                    ex.printStackTrace()
                } catch (ex: Exception) {
                    // Handle General Exception
                    Toast.makeText(this, "Error: ${ex.localizedMessage}", Toast.LENGTH_LONG).show()
                    ex.printStackTrace()
                }
            }
    }

    private fun createFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timestamp}_", ".jpg", storageDir)
    }

    private fun allPermissionsGranted() =
        REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
        }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
