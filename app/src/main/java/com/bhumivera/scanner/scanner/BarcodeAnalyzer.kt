package com.bhumivera.scanner.scanner

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class BarcodeAnalyzer(
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_DATA_MATRIX
        )
        .build()

    private val scanner = BarcodeScanning.getClient(options)
    private var lastScannedCode: String = ""
    private var lastScannedTimestamp: Long = 0L

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val now = System.currentTimeMillis()
                for (barcode in barcodes) {
                    val rawValue = barcode.rawValue
                    if (!rawValue.isNullOrBlank()) {
                        // Debounce identical scans within 1.2 seconds to prevent rapid duplicate triggers
                        if (rawValue != lastScannedCode || (now - lastScannedTimestamp) > 1200L) {
                            lastScannedCode = rawValue
                            lastScannedTimestamp = now
                            onBarcodeDetected(rawValue)
                            break
                        }
                    }
                }
            }
            .addOnFailureListener {
                // Ignore scanning glitches on motion blur frames
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}
