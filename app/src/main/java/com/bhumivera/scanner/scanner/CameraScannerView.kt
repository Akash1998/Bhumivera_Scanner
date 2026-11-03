package com.bhumivera.scanner.scanner

import android.content.Context
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors

@Composable
fun CameraScannerView(
    modifier: Modifier = Modifier,
    isTorchOn: Boolean = false,
    useFrontCamera: Boolean = false,
    onBarcodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var camera by remember { mutableStateOf<Camera?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    LaunchedEffect(isTorchOn, camera) {
        camera?.cameraControl?.enableTorch(isTorchOn)
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor, BarcodeAnalyzer { code ->
                                onBarcodeScanned(code)
                            })
                        }

                    val cameraSelector = if (useFrontCamera) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }

                    try {
                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                        camera?.cameraControl?.enableTorch(isTorchOn)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        // Overlay with scanning reticle
        ScannerOverlay()
    }
}

@Composable
fun ScannerOverlay() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(280.dp)) {
            val strokeWidth = 4.dp.toPx()
            val cornerLength = 36.dp.toPx()
            val emerald = Color(0xFF10B981)
            val gold = Color(0xFFD4AF37)

            // Outer subtle target box
            drawRoundRect(
                color = gold.copy(alpha = 0.35f),
                topLeft = Offset.Zero,
                size = size,
                cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // 4 High-visibility Emerald Reticle Corners
            // Top-Left
            drawLine(emerald, Offset(0f, 0f), Offset(cornerLength, 0f), strokeWidth)
            drawLine(emerald, Offset(0f, 0f), Offset(0f, cornerLength), strokeWidth)

            // Top-Right
            drawLine(emerald, Offset(size.width, 0f), Offset(size.width - cornerLength, 0f), strokeWidth)
            drawLine(emerald, Offset(size.width, 0f), Offset(size.width, cornerLength), strokeWidth)

            // Bottom-Left
            drawLine(emerald, Offset(0f, size.height), Offset(cornerLength, size.height), strokeWidth)
            drawLine(emerald, Offset(0f, size.height), Offset(0f, size.height - cornerLength), strokeWidth)

            // Bottom-Right
            drawLine(emerald, Offset(size.width, size.height), Offset(size.width - cornerLength, size.height), strokeWidth)
            drawLine(emerald, Offset(size.width, size.height), Offset(size.width, size.height - cornerLength), strokeWidth)
        }
    }
}
