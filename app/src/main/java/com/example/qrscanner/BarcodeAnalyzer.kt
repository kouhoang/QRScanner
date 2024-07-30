package com.example.qrscanner

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class BarcodeAnalyzer(
    private val onBarcodeDetected: (Barcode) -> Unit,
) : ImageAnalysis.Analyzer {
    private val barcodeScanner: BarcodeScanner = BarcodeScanning.getClient()

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        val mediaImage = image.image
        if (mediaImage != null) {
            val inputImage = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
            barcodeScanner
                .process(inputImage)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        onBarcodeDetected(barcode)
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
