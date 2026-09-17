package com.rork.blockblastsolver.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.NoPhotography
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.rork.blockblastsolver.ui.solver.BoardSource
import com.rork.blockblastsolver.ui.theme.Canvas as CanvasColor
import com.rork.blockblastsolver.ui.theme.NeonTeal
import com.rork.blockblastsolver.ui.theme.SurfaceElevated
import com.rork.blockblastsolver.ui.theme.TextPrimary
import com.rork.blockblastsolver.ui.theme.TextSecondary
import kotlinx.coroutines.launch

private const val TAG = "ScanScreen"

/**
 * The signature screen: a live viewfinder with an 8x8 framing guide, a big capture control,
 * plus fallbacks for importing a screenshot or entering the board by hand.
 */
@Composable
fun ScanScreen(
    isAnalyzing: Boolean,
    onBitmap: (Bitmap, BoardSource) -> Unit,
    onManualEntry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraAvailable by remember { mutableStateOf(true) }
    var statusText by remember { mutableStateOf("Menyiapkan kamera...") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        permissionDenied = !granted
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val bitmap = runCatching {
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }.onFailure { Log.w(TAG, "Gagal membaca gambar: ${it.message}") }.getOrNull()
        if (bitmap != null) onBitmap(bitmap, BoardSource.IMPORT)
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) return@LaunchedEffect
        val binding = runCatching { bindCamera(context, lifecycleOwner, previewView) }
            .onFailure { Log.w(TAG, "Camera unavailable: ${it.message}") }
            .getOrNull()
        imageCapture = binding?.imageCapture
        cameraAvailable = binding?.hasCamera == true
        statusText = if (cameraAvailable) "Mendeteksi grid... siap" else "Kamera tidak tersedia"
    }

    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black)
        ) {
            when {
                !hasPermission -> CameraBlockedState(
                    icon = Icons.Rounded.NoPhotography,
                    title = if (permissionDenied) "Izin kamera ditolak" else "Butuh izin kamera",
                    body = if (permissionDenied) {
                        "Aktifkan izin kamera di Pengaturan, atau pakai impor gambar & input manual."
                    } else {
                        "Aplikasi perlu kamera untuk memindai papan dari layar game."
                    },
                    actionLabel = if (permissionDenied) null else "Izinkan kamera",
                    onAction = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                )

                !cameraAvailable -> CameraBlockedState(
                    icon = Icons.Rounded.NoPhotography,
                    title = "Kamera tidak terdeteksi",
                    body = "Tidak ada kamera pada perangkat ini. Impor screenshot atau isi papan manual.",
                    actionLabel = null,
                    onAction = {}
                )

                else -> AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (hasPermission && cameraAvailable) {
                GridFramingOverlay(modifier = Modifier.fillMaxSize())
            }

            HintPill(
                text = "Arahkan ke papan & 3 blok yang tersedia",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            )

            AnalyzingOverlay(
                visible = isAnalyzing,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CanvasColor)
                .padding(top = 18.dp, bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CaptureButton(
                enabled = hasPermission && cameraAvailable && !isAnalyzing,
                onClick = {
                    val capture = imageCapture ?: return@CaptureButton
                    scope.launch {
                        runCatching { capture.captureBitmap(context) }
                            .onSuccess { onBitmap(it, BoardSource.CAMERA) }
                            .onFailure {
                                Log.w(TAG, "Capture error: ${it.message}")
                                statusText = "Gagal mengambil gambar, coba lagi"
                            }
                    }
                }
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Pindai Sekarang",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )

            Spacer(Modifier.height(10.dp))

            StatusRow(text = statusText, active = hasPermission && cameraAvailable && !isAnalyzing)

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, NeonTeal.copy(alpha = 0.45f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonTeal)
                ) {
                    Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Impor gambar", style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = onManualEntry,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, TextSecondary.copy(alpha = 0.4f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) {
                    Icon(Icons.Rounded.GridOn, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Input manual", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun AnalyzingOverlay(visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .background(CanvasColor.copy(alpha = 0.86f), RoundedCornerShape(20.dp))
                .padding(horizontal = 28.dp, vertical = 22.dp)
        ) {
            CircularProgressIndicator(color = NeonTeal, strokeWidth = 3.dp)
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Membaca papan...",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun CaptureButton(enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(104.dp)
            .alpha(if (enabled) 1f else 0.45f),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(2.dp, NeonTeal.copy(alpha = 0.55f), CircleShape)
        )
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(78.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonTeal,
                contentColor = CanvasColor,
                disabledContainerColor = NeonTeal.copy(alpha = 0.5f),
                disabledContentColor = CanvasColor
            ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.CameraAlt,
                contentDescription = "Pindai papan sekarang",
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun StatusRow(text: String, active: Boolean) {
    val transition = rememberInfiniteTransition(label = "status")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "statusAlpha"
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .alpha(if (active) alpha else 0.5f)
                .border(2.dp, if (active) NeonTeal else TextSecondary, CircleShape)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )
    }
}

@Composable
private fun HintPill(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(CanvasColor.copy(alpha = 0.82f), RoundedCornerShape(16.dp))
            .border(1.dp, NeonTeal.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.CenterFocusStrong,
            contentDescription = null,
            tint = NeonTeal,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary
        )
    }
}

/** Neon corner brackets plus a faint 8x8 lattice to help the user frame the board. */
@Composable
private fun GridFramingOverlay(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxWidth(0.84f)
                .aspectRatio(1f)
        ) {
            val unit = size.width / 8f
            for (i in 1 until 8) {
                val p = unit * i
                drawLine(
                    color = NeonTeal.copy(alpha = 0.35f),
                    start = androidx.compose.ui.geometry.Offset(p, 0f),
                    end = androidx.compose.ui.geometry.Offset(p, size.height),
                    strokeWidth = 1.2f
                )
                drawLine(
                    color = NeonTeal.copy(alpha = 0.35f),
                    start = androidx.compose.ui.geometry.Offset(0f, p),
                    end = androidx.compose.ui.geometry.Offset(size.width, p),
                    strokeWidth = 1.2f
                )
            }
            drawRoundRect(
                color = NeonTeal.copy(alpha = 0.7f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f),
                style = Stroke(width = 2.5f)
            )

            val armLength = size.width * 0.14f
            val thickness = 7f
            val corners = listOf(
                androidx.compose.ui.geometry.Offset(0f, 0f) to Pair(1f, 1f),
                androidx.compose.ui.geometry.Offset(size.width, 0f) to Pair(-1f, 1f),
                androidx.compose.ui.geometry.Offset(0f, size.height) to Pair(1f, -1f),
                androidx.compose.ui.geometry.Offset(size.width, size.height) to Pair(-1f, -1f)
            )
            for ((corner, direction) in corners) {
                drawLine(
                    color = NeonTeal,
                    start = corner,
                    end = androidx.compose.ui.geometry.Offset(corner.x + armLength * direction.first, corner.y),
                    strokeWidth = thickness,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                drawLine(
                    color = NeonTeal,
                    start = corner,
                    end = androidx.compose.ui.geometry.Offset(corner.x, corner.y + armLength * direction.second),
                    strokeWidth = thickness,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun CameraBlockedState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    actionLabel: String?,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceElevated)
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (actionLabel != null) {
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonTeal, contentColor = CanvasColor)
            ) {
                Text(actionLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
