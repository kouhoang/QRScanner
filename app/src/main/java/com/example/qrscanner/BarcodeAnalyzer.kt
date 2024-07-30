package com.example.qrscanner

import android.content.Context
import android.widget.Toast
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class BarcodeAnalyzer(
    private val context: Context, // Thêm tham số context
    private val onBarcodeDetected: (Barcode) -> Unit,
) : ImageAnalysis.Analyzer {
    private val barcodeScanner: BarcodeScanner = BarcodeScanning.getClient()
    private var lastUrl: String? = null
    private var lastAnalysisTime = 0L
    private val ANALYSIS_INTERVAL_MS = 2000L // 2 seconds

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAnalysisTime < ANALYSIS_INTERVAL_MS) {
            image.close()
            return
        }
        lastAnalysisTime = currentTime

        val mediaImage = image.image
        if (mediaImage != null) {
            val inputImage = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
            barcodeScanner
                .process(inputImage)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        val url = barcode.rawValue
                        if (url != null && url.startsWith("http") && url != lastUrl) {
                            lastUrl = url
                            onBarcodeDetected(barcode)
                        } else if (url != null && !url.startsWith("http")) {
                            Toast.makeText(context, url, Toast.LENGTH_LONG).show() // Sử dụng context từ tham số
                        }
                    }
                }.addOnFailureListener { e ->
                    e.printStackTrace()
                }.addOnCompleteListener {
                    image.close()
                }
        }
    }
}
