@file:Suppress("ktlint:standard:no-wildcard-imports")

package com.example.qrscanner

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
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
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var captureButton: Button
    private lateinit var libraryButton: Button
    private lateinit var imageCapture: ImageCapture
    private lateinit var barcodeScanner: BarcodeScanner
    private var lastUrl: String? = null

    private val REQUEST_CODE_PERMISSIONS = 1001
    private val REQUIRED_PERMISSIONS =
        arrayOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        )

    // Launcher để chọn ảnh từ thư viện
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
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) // Lắng nghe kết quả từ thư viện ảnh
            { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data?.data?.let { uri ->
                        scanImage(uri)
                    }
                }
            }

        libraryButton.setOnClickListener { openImagePicker() }
    }

    override fun onResume() {
        super.onResume()
        lastUrl = null // Reset lastUrl khi quay lại ứng dụng
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
                        it.setAnalyzer(
                            ContextCompat.getMainExecutor(this),
                            BarcodeAnalyzer { barcode ->
                                handleBarcodeDetected(barcode)
                            },
                        )
                    }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, analysisUseCase)
            },
            ContextCompat.getMainExecutor(this),
        )
    }

    // Kiểm tra mã QR có phải là link
    private fun handleBarcodeDetected(barcode: String) {
        if (barcode.startsWith("http") && barcode != lastUrl) {
            lastUrl = barcode
            showBarcodeDialog(barcode)
        }
    }

    private fun showBarcodeDialog(url: String) {
        val dialogFragment = DialogQRResultFragment()
        dialogFragment.show(supportFragmentManager, "qrResultDialog", url)
    }

    private fun takePhoto() {
        val photoFile = createTempFile()
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                @RequiresApi(Build.VERSION_CODES.Q)
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    scanImage(Uri.fromFile(photoFile))
                    addImageToDownloads(photoFile)
                    Toast.makeText(this@MainActivity, "Photo saved to Downloads", Toast.LENGTH_SHORT).show()
                }

                override fun onError(exception: ImageCaptureException) {
                    exception.printStackTrace()
                    Toast.makeText(this@MainActivity, "Failed to save photo: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            },
        )
    }

    // Tạo file tạm thời để lưu ảnh chụp
    private fun createTempFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timestamp}_", ".jpg", storageDir)
    }

    // Thêm ảnh vào thư mục Tải xuống
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun addImageToDownloads(photoFile: File) {
        val values =
            ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, photoFile.name)
                put(MediaStore.Downloads.MIME_TYPE, "image/jpeg")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }

        val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)

        if (uri != null) {
            try {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    photoFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream) // Sao chép dữ liệu từ file ảnh vào uri
                    }
                }

                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                contentResolver.update(uri, values, null, null)
            } catch (e: IOException) {
                e.printStackTrace()
                contentResolver.delete(uri, null, null) // Xóa file nếu có lỗi xảy ra
            }
        }
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
                    val barcodeValue = barcode.rawValue
                    if (barcodeValue != null) {
                        handleBarcodeDetected(barcodeValue)
                    }
                }
            }.addOnFailureListener { e ->
                try {
                    throw e
                } catch (ex: IOException) {
                    Toast.makeText(this, "IO Error: ${ex.localizedMessage}", Toast.LENGTH_LONG).show()
                } catch (ex: SecurityException) {
                    Toast.makeText(this, "Security Error: ${ex.localizedMessage}", Toast.LENGTH_LONG).show()
                } catch (ex: Exception) {
                    Toast.makeText(this, "Error: ${ex.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
    }

    // Kiểm tra xem tất cả các quyền đã được cấp hay chưa
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

    // Phân tích mã vạch
    private inner class BarcodeAnalyzer(
        private val onBarcodeDetected: (String) -> Unit,
    ) : ImageAnalysis.Analyzer {
        @androidx.annotation.OptIn(ExperimentalGetImage::class)
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                barcodeScanner
                    .process(image)
                    .addOnSuccessListener { barcodes ->
                        for (barcode in barcodes) {
                            val barcodeValue = barcode.rawValue
                            if (barcodeValue != null) {
                                onBarcodeDetected(barcodeValue)
                                imageProxy.close()
                                return@addOnSuccessListener
                            }
                        }
                        imageProxy.close()
                    }.addOnFailureListener {
                        imageProxy.close()
                    }
            }
        }
    }
}
