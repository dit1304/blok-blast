package com.rork.blockblastsolver.ui.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "CameraCapture"

/** Result of binding the camera use cases. */
data class CameraBinding(
    val imageCapture: ImageCapture?,
    val hasCamera: Boolean
)

/**
 * Binds preview + still capture to [lifecycleOwner]. The cloud emulator exposes the user's
 * webcam as a regular camera device, so the normal CameraX pipeline is used everywhere.
 */
suspend fun bindCamera(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    previewView: PreviewView
): CameraBinding {
    val provider = suspendCancellableCoroutine<ProcessCameraProvider> { continuation ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            runCatching { future.get() }
                .onSuccess { continuation.resume(it) }
                .onFailure { continuation.resumeWithException(it) }
        }, ContextCompat.getMainExecutor(context))
    }

    val selector = when {
        provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
        provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
        else -> return CameraBinding(imageCapture = null, hasCamera = false)
    }

    val preview = Preview.Builder().build().apply {
        surfaceProvider = previewView.surfaceProvider
    }
    val imageCapture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .build()

    return runCatching {
        provider.unbindAll()
        provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
        CameraBinding(imageCapture = imageCapture, hasCamera = true)
    }.getOrElse { error ->
        Log.w(TAG, "Camera binding failed: ${error.message}")
        CameraBinding(imageCapture = null, hasCamera = false)
    }
}

/** Takes a single frame and returns it as an upright bitmap. */
suspend fun ImageCapture.captureBitmap(context: Context): Bitmap =
    suspendCancellableCoroutine { continuation ->
        takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val rotation = image.imageInfo.rotationDegrees
                    val bitmap = runCatching { image.toBitmap() }.getOrNull()
                    image.close()
                    if (bitmap == null) {
                        continuation.resumeWithException(IllegalStateException("Frame tidak terbaca"))
                        return
                    }
                    continuation.resume(bitmap.rotated(rotation))
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.w(TAG, "Capture failed: ${exception.message}")
                    continuation.resumeWithException(exception)
                }
            }
        )
    }

private fun Bitmap.rotated(degrees: Int): Bitmap {
    if (degrees == 0) return this
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}
