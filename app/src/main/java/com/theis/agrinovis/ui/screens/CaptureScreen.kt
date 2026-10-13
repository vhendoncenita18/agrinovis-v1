package com.theis.agrinovis.ui.screens

import android.Manifest
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.theis.agrinovis.data.session.MonitoringSession
import com.theis.agrinovis.ml.cv.CvMode
import com.theis.agrinovis.ml.cv.TFLiteCvEngine
import com.theis.agrinovis.ui.components.AgrinovisScaffold
import com.theis.agrinovis.ui.components.CameraPreview
import com.theis.agrinovis.ui.navigation.Screen
import com.theis.agrinovis.ui.theme.NeonGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executor

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CaptureScreen(navController: NavController, mode: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cvEngine = remember { TFLiteCvEngine(context) }
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var captureSuccess by remember { mutableStateOf(false) }
    var isAnalyzing by remember { mutableStateOf(false) }
    
    // Preview States
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isPreviewMode by remember { mutableStateOf(false) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val mainExecutor: Executor = ContextCompat.getMainExecutor(context)

    // Automatically request permission on entry
    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    if (captureSuccess) {
        LaunchedEffect(Unit) {
            delay(800) // Show success for 800ms
            if (mode == "Diameter") {
                navController.navigate(Screen.CaptureHeight.route)
            } else {
                navController.navigate(Screen.Verification.route)
            }
            captureSuccess = false
            isPreviewMode = false
            capturedImageUri = null
        }
    }

    fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val photoFile = File(
            context.cacheDir,
            "capture_${mode}_${System.currentTimeMillis()}.jpg"
        )
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            mainExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val uri = Uri.fromFile(photoFile)
                    capturedImageUri = uri
                    isPreviewMode = true

                    if (mode == "Diameter") {
                        MonitoringSession.diameterImageUri = uri.toString()
                    } else {
                        MonitoringSession.heightImageUri = uri.toString()
                    }

                    // Automatically feed image to TFLite model directly
                    isAnalyzing = true
                    scope.launch {
                        try {
                            val startTime = System.currentTimeMillis()

                            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                            } else {
                                @Suppress("DEPRECATION")
                                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                            }
                            val cvMode = if (mode == "Diameter") CvMode.DIAMETER else CvMode.HEIGHT
                            val result = cvEngine.processImage(bitmap, cvMode)

                            val elapsed = System.currentTimeMillis() - startTime
                            if (elapsed < 1200) {
                                delay(1200 - elapsed)
                            }

                            if (mode == "Diameter") {
                                MonitoringSession.diameterCvResult = result
                            } else {
                                MonitoringSession.heightCvResult = result
                            }

                            isAnalyzing = false
                        } catch (e: Exception) {
                            isAnalyzing = false
                            Log.e("CaptureScreen", "CV Extraction exception: ${e.message}")
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CaptureScreen", "Image capture failed: ${exception.message}")
                }
            }
        )
    }

    AgrinovisScaffold(
        navController = navController,
        title = "CAPTURE ${mode.uppercase()}",
        showBottomBar = false
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // STEP INDICATOR
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = if (isAnalyzing) "STEP: EXECUTING TFLITE MODEL INFERENCE..." 
                           else if (isPreviewMode) "STEP: REVIEW DETECTED SPECIMEN" 
                           else "STEP: POSITION PLANT IN FRAME",
                    modifier = Modifier.padding(12.dp),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            }

            // CAMERA VIEWFINDER / PREVIEW WITH RAW SEGMENTATION OVERLAY
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (isPreviewMode && capturedImageUri != null) {
                    AsyncImage(
                        model = capturedImageUri,
                        contentDescription = "Captured Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Draw Bounding Box, Segmentation Contour & Feret Diameter Line Overlay
                    val cvResult = if (mode == "Diameter") MonitoringSession.diameterCvResult else MonitoringSession.heightCvResult
                    if (!isAnalyzing && cvResult != null) {
                        if (cvResult.isSuccessful) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val scaleX = size.width / 640f
                                val scaleY = size.height / 640f

                                // Draw Segmentation Contour Path if available
                                if (cvResult.contourPoints.isNotEmpty()) {
                                    val path = androidx.compose.ui.graphics.Path().apply {
                                        cvResult.contourPoints.forEachIndexed { index, pt ->
                                            val sx = pt.first * scaleX
                                            val sy = pt.second * scaleY
                                            if (index == 0) moveTo(sx, sy) else lineTo(sx, sy)
                                        }
                                        close()
                                    }
                                    drawPath(
                                        path = path,
                                        color = Color(0xFF00E5FF).copy(alpha = 0.5f),
                                        style = Stroke(width = 2.dp.toPx())
                                    )
                                    drawPath(
                                        path = path,
                                        color = Color(0xFF00E5FF).copy(alpha = 0.15f)
                                    )
                                }

                                // Draw Maximum Feret Diameter or Vertical Height Line
                                if (mode == "Diameter") {
                                    if (cvResult.measurementSource == "FERET_DIAMETER" && cvResult.feretDiameterPx > 0f) {
                                        val p1 = Offset(cvResult.feretPoint1X * scaleX, cvResult.feretPoint1Y * scaleY)
                                        val p2 = Offset(cvResult.feretPoint2X * scaleX, cvResult.feretPoint2Y * scaleY)

                                        // Feret diameter line (Electric Cyan)
                                        drawLine(
                                            color = Color(0xFF00E5FF),
                                            start = p1,
                                            end = p2,
                                            strokeWidth = 4.dp.toPx(),
                                            cap = StrokeCap.Round
                                        )

                                        // Endpoints
                                        drawCircle(color = Color.White, radius = 7.dp.toPx(), center = p1)
                                        drawCircle(color = Color(0xFF00E5FF), radius = 4.dp.toPx(), center = p1)
                                        drawCircle(color = Color.White, radius = 7.dp.toPx(), center = p2)
                                        drawCircle(color = Color(0xFF00E5FF), radius = 4.dp.toPx(), center = p2)
                                    } else {
                                        val left = cvResult.bboxLeft * scaleX
                                        val top = cvResult.bboxTop * scaleY
                                        val right = cvResult.bboxRight * scaleX
                                        val bottom = cvResult.bboxBottom * scaleY

                                        drawRect(
                                            color = NeonGreen,
                                            topLeft = Offset(left, top),
                                            size = Size(right - left, bottom - top),
                                            style = Stroke(width = 3.dp.toPx())
                                        )
                                    }
                                } else {
                                    if (cvResult.heightPx > 0f) {
                                        val topPt = Offset(cvResult.heightTopX * scaleX, cvResult.heightTopY * scaleY)
                                        val bottomPt = Offset(cvResult.heightBottomX * scaleX, cvResult.heightBottomY * scaleY)

                                        // Vertical height line (Neon Green)
                                        drawLine(
                                            color = NeonGreen,
                                            start = topPt,
                                            end = bottomPt,
                                            strokeWidth = 4.dp.toPx(),
                                            cap = StrokeCap.Round
                                        )

                                        // Top marker (Highest leaf)
                                        drawCircle(color = Color.White, radius = 7.dp.toPx(), center = topPt)
                                        drawCircle(color = NeonGreen, radius = 4.dp.toPx(), center = topPt)

                                        // Bottom marker (Lowest point of lettuce plant)
                                        drawCircle(color = Color.White, radius = 7.dp.toPx(), center = bottomPt)
                                        drawCircle(color = NeonGreen, radius = 4.dp.toPx(), center = bottomPt)
                                    } else {
                                        val left = cvResult.bboxLeft * scaleX
                                        val top = cvResult.bboxTop * scaleY
                                        val right = cvResult.bboxRight * scaleX
                                        val bottom = cvResult.bboxBottom * scaleY

                                        drawRect(
                                            color = NeonGreen,
                                            topLeft = Offset(left, top),
                                            size = Size(right - left, bottom - top),
                                            style = Stroke(width = 3.dp.toPx())
                                        )
                                    }
                                }
                            }

                            // Floating Raw Confidence & Dimension Badge
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(16.dp),
                                color = Color.Black.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, NeonGreen)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "RAW TFLITE CONFIDENCE: %.1f%%".format(cvResult.confidence * 100),
                                            color = NeonGreen,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.sp,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (mode == "Diameter") "CALCULATED DIAMETER: %.1f cm".format(cvResult.diameterCm) else "CALCULATED HEIGHT: %.1f cm".format(cvResult.heightCm),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            // Warning Badge when Lettuce is NOT detected
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(16.dp),
                                color = Color.Black.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "UNABLE TO DETECT LETTUCE",
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.sp,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = cvResult.errorMessage ?: "Please ensure the plant is well-lit and centered in the frame, then try again.",
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                } else if (cameraPermissionState.status.isGranted) {
                    CameraPreview(
                        modifier = Modifier.fillMaxSize(),
                        onImageCaptureCreated = { imageCapture = it }
                    )
                } else {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "SENSOR ACCESS REQUIRED",
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Please enable camera access to start the AI measurement process.",
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { cameraPermissionState.launchPermissionRequest() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("INITIALIZE CAMERA", color = Color.Black)
                        }
                    }
                }

                // Analyzing Overlay (Visible AI Inference Feed)
                if (isAnalyzing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.88f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            CircularProgressIndicator(color = NeonGreen, strokeWidth = 3.dp)
                            Text(
                                text = "EXECUTING TFLITE MODEL INFERENCE...",
                                color = NeonGreen,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                letterSpacing = 1.5.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Scanning 8,400 anchors & decoding YOLOV8-SEG87MAP tensor...",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Success Overlay
                if (captureSuccess) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White)
                            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "MEASUREMENT CONFIRMED",
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }

            // CONTROLS & INSTRUCTIONS
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val instructionText = if (isPreviewMode)
                    "REVIEW MODEL DETECTED SEGMENTATION. CONFIRM OR RETAKE PHOTO."
                else if (mode == "Diameter")
                    "POSITION CAMERA DIRECTLY OVER THE LETTUCE CORE, 14 INCHES AWAY FROM THE PLANT FOR PRECISION DIAMETER ANALYSIS."
                else
                    "POSITION CAMERA AT THE BASE LEVEL, 14 INCHES AWAY FROM THE PLANT FOR SIDE-VIEW HEIGHT EXTRACTION."

                val annotatedInstruction = remember(instructionText) {
                    buildAnnotatedString {
                        append(instructionText)
                        val target = "14 INCHES"
                        val startIndex = instructionText.indexOf(target)
                        if (startIndex != -1) {
                            addStyle(
                                SpanStyle(fontWeight = FontWeight.Bold, color = NeonGreen),
                                startIndex,
                                startIndex + target.length
                            )
                        }
                    }
                }

                Text(
                    text = annotatedInstruction,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    lineHeight = 18.sp,
                    letterSpacing = 0.5.sp
                )

                val activeCvResult = if (mode == "Diameter") MonitoringSession.diameterCvResult else MonitoringSession.heightCvResult
                val isConfirmEnabled = if (isPreviewMode) {
                    !isAnalyzing && !captureSuccess && (activeCvResult?.isSuccessful == true)
                } else {
                    cameraPermissionState.status.isGranted && !captureSuccess && !isAnalyzing
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            isPreviewMode = false
                            capturedImageUri = null
                            if (mode == "Diameter") {
                                MonitoringSession.diameterCvResult = null
                            } else {
                                MonitoringSession.heightCvResult = null
                            }
                        },
                        enabled = !isAnalyzing && !captureSuccess,
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen),
                        border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "RETAKE",
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    Button(
                        onClick = {
                            if (isPreviewMode) {
                                if (isAnalyzing || captureSuccess || activeCvResult?.isSuccessful != true) return@Button
                                captureSuccess = true
                            } else {
                                takePhoto()
                            }
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        enabled = isConfirmEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonGreen,
                            disabledContainerColor = NeonGreen.copy(alpha = 0.2f)
                        )
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = if (isPreviewMode) "CONFIRM" else "CAPTURE",
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }

                TextButton(
                    onClick = { navController.popBackStack() },
                    enabled = !isAnalyzing
                ) {
                    Text("ABORT MONITORING", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, letterSpacing = 1.sp)
                }
            }
        }
    }
}
