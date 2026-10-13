package com.theis.agrinovis.ml.cv

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.exp

/**
 * TensorFlow Lite execution engine for YOLOv8-seg Computer Vision dimension extraction.
 *
 * Loads `models/cv_model.tflite` (`YOLOV8-SEG87MAP.tflite`) from APK assets and executes real model inference.
 * Supports both NCHW [1, 3, 640, 640] and NHWC [1, 640, 640, 3] layouts dynamically.
 */
class TFLiteCvEngine(private val context: Context) : ComputerVisionModel {

    companion object {
        private const val TAG = "TFLiteCvEngine"
        const val MODEL_FILE = "models/cv_model.tflite"
        const val INPUT_IMAGE_WIDTH = 640
        const val INPUT_IMAGE_HEIGHT = 640
        const val MODEL_VERSION = "v1.0-tflite-yolov8seg"
        const val CONFIDENCE_THRESHOLD = 0.85f

        // Calibration constants in pixels per millimeter (px/mm)
        const val DIAMETER_PIXELS_PER_MM = 1.9035f
        const val HEIGHT_PIXELS_PER_MM = 1.2816f

        /**
         * Converts pixel dimensions to centimeters using pixels-per-millimeter calibration factor.
         * Formula: dimensionCm = (dimensionPixels / pixelsPerMm) / 10.0f
         *
         * @return Dimension in centimeters, or null if inputs are non-finite or non-positive.
         */
        fun pixelsToCm(dimensionPixels: Float, pixelsPerMm: Float): Float? {
            if (!dimensionPixels.isFinite() || !pixelsPerMm.isFinite() || dimensionPixels <= 0f || pixelsPerMm <= 0f) {
                return null
            }
            return (dimensionPixels / pixelsPerMm) / 10.0f
        }
    }

    private var interpreter: Interpreter? = null
    private var isModelLoaded = false

    init {
        loadModel()
    }

    private fun loadModel() {
        try {
            val inputStream = context.assets.open(MODEL_FILE)
            val modelBytes = inputStream.readBytes()
            inputStream.close()
            val byteBuffer = ByteBuffer.allocateDirect(modelBytes.size)
            byteBuffer.order(ByteOrder.nativeOrder())
            byteBuffer.put(modelBytes)
            byteBuffer.rewind()

            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }
            interpreter = Interpreter(byteBuffer, options)
            isModelLoaded = true
            Log.i(TAG, "TFLite YOLOv8-seg model loaded successfully from $MODEL_FILE")
        } catch (e: IOException) {
            Log.w(TAG, "TFLite model file '$MODEL_FILE' not found in assets: ${e.message}", e)
            isModelLoaded = false
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing TFLite Interpreter: ${e.message}", e)
            isModelLoaded = false
        }
    }

    override suspend fun processImage(bitmap: Bitmap, mode: CvMode): CvResult = withContext(Dispatchers.Default) {
        if (!isModelLoaded || interpreter == null) {
            Log.e(TAG, "TFLite model not loaded. Asset path: assets/$MODEL_FILE")
            return@withContext CvResult(
                isSuccessful = false,
                errorMessage = "TFLite model asset ('$MODEL_FILE') is not available in assets/models/."
            )
        }

        try {
            val softwareBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bitmap.config == Bitmap.Config.HARDWARE) {
                bitmap.copy(Bitmap.Config.ARGB_8888, true) ?: bitmap
            } else {
                bitmap
            }

            val scaledBitmap = Bitmap.createScaledBitmap(softwareBitmap, INPUT_IMAGE_WIDTH, INPUT_IMAGE_HEIGHT, true)
            
            // Check model input shape layout (NCHW [1, 3, 640, 640] vs NHWC [1, 640, 640, 3])
            val inputShape = interpreter?.getInputTensor(0)?.shape() ?: intArrayOf(1, 3, 640, 640)
            Log.i(TAG, "TFLite Input Tensor 0 Shape: ${inputShape.joinToString()}")

            val isNchw = inputShape.size == 4 && inputShape[1] == 3

            val inputBuffer = ByteBuffer.allocateDirect(1 * 3 * INPUT_IMAGE_HEIGHT * INPUT_IMAGE_WIDTH * 4)
            inputBuffer.order(ByteOrder.nativeOrder())
            val floatBufInput = inputBuffer.asFloatBuffer()

            val intValues = IntArray(INPUT_IMAGE_WIDTH * INPUT_IMAGE_HEIGHT)
            scaledBitmap.getPixels(intValues, 0, INPUT_IMAGE_WIDTH, 0, 0, INPUT_IMAGE_WIDTH, INPUT_IMAGE_HEIGHT)

            if (isNchw) {
                // NCHW layout: [1, 3, 640, 640] -> All Red, then Green, then Blue
                val stride = INPUT_IMAGE_WIDTH * INPUT_IMAGE_HEIGHT
                for (i in intValues.indices) {
                    val pixel = intValues[i]
                    val r = ((pixel shr 16) and 0xFF) / 255.0f
                    val g = ((pixel shr 8) and 0xFF) / 255.0f
                    val b = (pixel and 0xFF) / 255.0f

                    floatBufInput.put(i, r)
                    floatBufInput.put(stride + i, g)
                    floatBufInput.put(2 * stride + i, b)
                }
            } else {
                // NHWC layout: [1, 640, 640, 3] -> Pixel interleaved RGB
                for (pixel in intValues) {
                    val r = ((pixel shr 16) and 0xFF) / 255.0f
                    val g = ((pixel shr 8) and 0xFF) / 255.0f
                    val b = (pixel and 0xFF) / 255.0f

                    floatBufInput.put(r)
                    floatBufInput.put(g)
                    floatBufInput.put(b)
                }
            }
            inputBuffer.rewind()

            // Determine output tensor shape dynamically from interpreter
            val outputTensorShape = interpreter?.getOutputTensor(0)?.shape() ?: intArrayOf(1, 37, 8400)
            Log.i(TAG, "TFLite Output Tensor 0 Shape: ${outputTensorShape.joinToString()}")

            val dim1 = outputTensorShape.getOrNull(1) ?: 37
            val dim2 = outputTensorShape.getOrNull(2) ?: 8400

            val channels: Int
            val numAnchors: Int
            val isTransposed: Boolean

            if (dim1 < dim2) {
                channels = dim1 // 37
                numAnchors = dim2 // 8400
                isTransposed = false
            } else {
                channels = dim2 // 37
                numAnchors = dim1 // 8400
                isTransposed = true
            }

            val outputBuffer0 = Array(1) { 
                Array(dim1) { 
                    FloatArray(dim2) 
                } 
            }

            val outputs = mutableMapOf<Int, Any>(0 to outputBuffer0)

            if ((interpreter?.outputTensorCount ?: 1) > 1) {
                val output1Shape = interpreter?.getOutputTensor(1)?.shape() ?: intArrayOf(1, 32, 160, 160)
                val outputBuffer1 = Array(output1Shape[0]) {
                    Array(output1Shape[1]) {
                        Array(output1Shape[2]) {
                            FloatArray(output1Shape[3])
                        }
                    }
                }
                outputs[1] = outputBuffer1
            }

            val startTime = System.currentTimeMillis()
            interpreter?.runForMultipleInputsOutputs(arrayOf(inputBuffer), outputs)
            Log.i(TAG, "Input bitmap size: ${bitmap.width}x${bitmap.height}")
            Log.i(TAG, "Scaled bitmap size: ${scaledBitmap.width}x${scaledBitmap.height}")

            for (i in 0 until (interpreter?.outputTensorCount ?: 0)) {
                val tensor = interpreter!!.getOutputTensor(i)
                Log.i(
                    TAG,
                    "OUTPUT $i: shape=${tensor.shape().contentToString()}, " +
                            "type=${tensor.dataType()}"
                )
            }
            var maxClassScore = Float.NEGATIVE_INFINITY
            var minClassScore = Float.POSITIVE_INFINITY

            fun getVal(c: Int, a: Int): Float {
                return if (!isTransposed) {
                    if (c in 0 until channels && a in 0 until numAnchors)
                        outputBuffer0[0][c][a] else 0f
                } else {
                    if (a in 0 until numAnchors && c in 0 until channels)
                        outputBuffer0[0][a][c] else 0f
                }
            }

            for (a in 0 until numAnchors) {
                val score = getVal(4, a)
                if (score > maxClassScore) maxClassScore = score
                if (score < minClassScore) minClassScore = score
            }

            Log.i(
                TAG,
                "YOLO CLASS SCORE RANGE: min=$minClassScore, max=$maxClassScore"
            )

            val inferenceTimeMs = System.currentTimeMillis() - startTime

            var bestConfidence = -1f
            var bestAnchorIndex = -1
            var bestWidthPx = 0f
            var bestHeightPx = 0f
            var bestCx = 0f
            var bestCy = 0f

            // Scan all 8,400 anchors to find the peak class detection from TFLite tensor.
            // For YOLOv8 single-class segmentation [1, 37, 8400]:
            // Channel 4 is the ONLY class score (lettuce score logit).
            // Channels 5..36 are mask prototype coefficients, NOT class scores.
            for (a in 0 until numAnchors) {
                val rawClassScore = getVal(4, a)
                val conf = if (rawClassScore > 1.0f || rawClassScore < -1.0f) {
                    1.0f / (1.0f + exp(-rawClassScore))
                } else if (rawClassScore >= 0f) {
                    rawClassScore
                } else {
                    1.0f / (1.0f + exp(-rawClassScore))
                }

                if (conf > bestConfidence) {
                    bestConfidence = conf
                    bestAnchorIndex = a
                    bestCx = getVal(0, a)
                    bestCy = getVal(1, a)
                    bestWidthPx = getVal(2, a)
                    bestHeightPx = getVal(3, a)
                }
            }

            val isAccepted = bestConfidence >= CONFIDENCE_THRESHOLD && bestWidthPx > 0f && bestHeightPx > 0f
            Log.i(
                TAG,
                "TFLite Inference complete (${inferenceTimeMs}ms) -> Peak Anchor: $bestAnchorIndex, Peak Confidence = $bestConfidence, Threshold = $CONFIDENCE_THRESHOLD, Accepted = $isAccepted"
            )

            // Scale normalized bounding box dimensions to 640x640 pixel coordinates if needed
            if (bestCx in 0.0001f..1.0f) bestCx *= INPUT_IMAGE_WIDTH
            if (bestCy in 0.0001f..1.0f) bestCy *= INPUT_IMAGE_HEIGHT
            if (bestWidthPx in 0.0001f..1.0f) bestWidthPx *= INPUT_IMAGE_WIDTH
            if (bestHeightPx in 0.0001f..1.0f) bestHeightPx *= INPUT_IMAGE_HEIGHT

            // Check if peak confidence passes detection threshold and box dimensions are valid
            if (!isAccepted) {
                val rejectReason = if (bestConfidence < CONFIDENCE_THRESHOLD) {
                    "Peak confidence ($bestConfidence) is below 85% threshold ($CONFIDENCE_THRESHOLD)"
                } else {
                    "Invalid bounding box dimensions (WidthPx=$bestWidthPx, HeightPx=$bestHeightPx)"
                }
                Log.w(
                    TAG,
                    "No valid lettuce detected in frame. REASON: $rejectReason"
                )
                return@withContext CvResult(
                    diameterCm = 0f,
                    heightCm = 0f,
                    confidence = bestConfidence.coerceAtLeast(0f),
                    modelVersion = MODEL_VERSION,
                    isSuccessful = false,
                    errorMessage = "Unable to detect lettuce clearly (Confidence: %.1f%%). Please position the plant centrally in good lighting and retake the photo.".format(bestConfidence.coerceAtLeast(0f) * 100),
                    bboxLeft = 0f,
                    bboxTop = 0f,
                    bboxRight = 0f,
                    bboxBottom = 0f
                )
            }

            // Calculate exact bounding box coordinates from raw model output
            val bboxLeft = (bestCx - bestWidthPx / 2f).coerceIn(0f, INPUT_IMAGE_WIDTH.toFloat())
            val bboxTop = (bestCy - bestHeightPx / 2f).coerceIn(0f, INPUT_IMAGE_HEIGHT.toFloat())
            val bboxRight = (bestCx + bestWidthPx / 2f).coerceIn(0f, INPUT_IMAGE_WIDTH.toFloat())
            val bboxBottom = (bestCy + bestHeightPx / 2f).coerceIn(0f, INPUT_IMAGE_HEIGHT.toFloat())

            // Extract mask coefficients for bestAnchorIndex from channels 5..36
            val maskCoeffs = FloatArray(32)
            for (k in 0 until 32) {
                maskCoeffs[k] = getVal(5 + k, bestAnchorIndex)
            }

            var feretDiameterPx = 0f
            var feretP1 = Pair(0f, 0f)
            var feretP2 = Pair(0f, 0f)
            var measurementSource = "BOUNDING_BOX"
            val contourPoints = mutableListOf<Pair<Float, Float>>()
            val foregroundPixels = mutableListOf<Pair<Float, Float>>()

            if (outputs.containsKey(1)) {
                val output1Shape = interpreter?.getOutputTensor(1)?.shape() ?: intArrayOf(1, 32, 160, 160)
                @Suppress("UNCHECKED_CAST")
                val outputBuffer1 = outputs[1] as? Array<Array<Array<FloatArray>>>

                if (outputBuffer1 != null) {
                    val maskBoxLeft = (bboxLeft / 4f).toInt().coerceIn(0, 159)
                    val maskBoxTop = (bboxTop / 4f).toInt().coerceIn(0, 159)
                    val maskBoxRight = (bboxRight / 4f).toInt().coerceIn(0, 159)
                    val maskBoxBottom = (bboxBottom / 4f).toInt().coerceIn(0, 159)

                    val mask160 = Array(160) { BooleanArray(160) }
                    for (my in maskBoxTop..maskBoxBottom) {
                        for (mx in maskBoxLeft..maskBoxRight) {
                            var logit = 0f
                            for (k in 0 until 32) {
                                val protoVal = if (output1Shape.size == 4 && output1Shape[1] == 32) {
                                    outputBuffer1[0][k][my][mx]
                                } else if (output1Shape.size == 4 && output1Shape[3] == 32) {
                                    outputBuffer1[0][my][mx][k]
                                } else {
                                    outputBuffer1[0][k][my][mx]
                                }
                                logit += maskCoeffs[k] * protoVal
                            }
                            val maskVal = 1.0f / (1.0f + exp(-logit))
                            if (maskVal >= 0.5f) {
                                mask160[my][mx] = true
                                val px = mx * 4.0f + 2.0f
                                val py = my * 4.0f + 2.0f
                                foregroundPixels.add(Pair(px, py))
                            }
                        }
                    }

                    val rawBoundary = mutableListOf<Pair<Float, Float>>()
                    for (my in maskBoxTop..maskBoxBottom) {
                        for (mx in maskBoxLeft..maskBoxRight) {
                            if (mask160[my][mx]) {
                                val isBoundary = my == maskBoxTop || my == maskBoxBottom || mx == maskBoxLeft || mx == maskBoxRight ||
                                        !mask160[my - 1][mx] || !mask160[my + 1][mx] || !mask160[my][mx - 1] || !mask160[my][mx + 1]
                                if (isBoundary) {
                                    val px = mx * 4.0f + 2.0f
                                    val py = my * 4.0f + 2.0f
                                    rawBoundary.add(Pair(px, py))
                                }
                            }
                        }
                    }

                    // Downsample contour for UI rendering efficiency if needed
                    val step = if (rawBoundary.size > 250) rawBoundary.size / 250 else 1
                    for (i in rawBoundary.indices step step) {
                        contourPoints.add(rawBoundary[i])
                    }

                    // Calculate Maximum Feret Diameter
                    if (rawBoundary.size >= 2) {
                        var maxDistSq = -1f
                        val n = rawBoundary.size
                        for (i in 0 until n) {
                            val p1 = rawBoundary[i]
                            for (j in i + 1 until n) {
                                val p2 = rawBoundary[j]
                                val dx = p1.first - p2.first
                                val dy = p1.second - p2.second
                                val distSq = dx * dx + dy * dy
                                if (distSq > maxDistSq) {
                                    maxDistSq = distSq
                                    feretP1 = p1
                                    feretP2 = p2
                                }
                            }
                        }
                        if (maxDistSq > 0f) {
                            feretDiameterPx = kotlin.math.sqrt(maxDistSq)
                            measurementSource = "FERET_DIAMETER"
                        }
                    }
                }
            }

            val finalDiameterPx = if (measurementSource == "FERET_DIAMETER" && feretDiameterPx > 0f) {
                feretDiameterPx
            } else {
                measurementSource = "BOUNDING_BOX"
                bestWidthPx
            }

            // Calculate Vertical Height from mask foreground pixels (Top leaf to bottom of lettuce plant)
            var heightPx = 0f
            var heightTopX = (bboxLeft + bboxRight) / 2f
            var heightTopY = bboxTop
            var heightBottomX = (bboxLeft + bboxRight) / 2f
            var heightBottomY = bboxBottom
            var heightMeasurementSource = "BOUNDING_BOX"

            if (foregroundPixels.isNotEmpty()) {
                var minY = Float.POSITIVE_INFINITY
                var maxY = Float.NEGATIVE_INFINITY
                val minXBucket = mutableListOf<Float>()
                val maxXBucket = mutableListOf<Float>()

                for (pt in foregroundPixels) {
                    if (pt.second < minY) {
                        minY = pt.second
                        minXBucket.clear()
                        minXBucket.add(pt.first)
                    } else if (kotlin.math.abs(pt.second - minY) < 2.0f) {
                        minXBucket.add(pt.first)
                    }

                    if (pt.second > maxY) {
                        maxY = pt.second
                        maxXBucket.clear()
                        maxXBucket.add(pt.first)
                    } else if (kotlin.math.abs(pt.second - maxY) < 2.0f) {
                        maxXBucket.add(pt.first)
                    }
                }

                val topX = if (minXBucket.isNotEmpty()) minXBucket.average().toFloat() else (bboxLeft + bboxRight) / 2f
                val bottomX = if (maxXBucket.isNotEmpty()) maxXBucket.average().toFloat() else (bboxLeft + bboxRight) / 2f

                heightPx = kotlin.math.abs(maxY - minY)
                heightTopX = topX
                heightTopY = minY
                heightBottomX = bottomX
                heightBottomY = maxY
                heightMeasurementSource = "VERTICAL_MASK_HEIGHT"
            }

            val finalHeightPx = if (heightMeasurementSource == "VERTICAL_MASK_HEIGHT" && heightPx > 0f) {
                heightPx
            } else {
                heightMeasurementSource = "BOUNDING_BOX"
                bestHeightPx
            }

            // Convert bounding box and mask dimensions from pixels to millimeters and centimeters
            val diameterCm = pixelsToCm(finalDiameterPx, DIAMETER_PIXELS_PER_MM) ?: 0f
            val heightCm = pixelsToCm(finalHeightPx, HEIGHT_PIXELS_PER_MM) ?: 0f

            val diameterMm = finalDiameterPx / DIAMETER_PIXELS_PER_MM
            val heightMm = finalHeightPx / HEIGHT_PIXELS_PER_MM

            Log.i(
                TAG,
                "CV_MEASUREMENT: mode=DIAMETER, source=$measurementSource, feretPx=$feretDiameterPx, widthPx=$bestWidthPx, diameterMm=$diameterMm, diameterCm=$diameterCm"
            )
            Log.i(
                TAG,
                "CV_MEASUREMENT: mode=HEIGHT, source=$heightMeasurementSource, heightPx=$finalHeightPx, topX=$heightTopX, topY=$heightTopY, bottomX=$heightBottomX, bottomY=$heightBottomY, heightCalibPxPerMm=$HEIGHT_PIXELS_PER_MM, heightMm=$heightMm, heightCm=$heightCm"
            )

            Log.i(
                TAG,
                "Lettuce detected successfully! Confidence = $bestConfidence, Diameter = $diameterCm cm, Height = $heightCm cm"
            )

            CvResult(
                diameterCm = diameterCm,
                heightCm = heightCm,
                confidence = bestConfidence,
                modelVersion = MODEL_VERSION,
                isSuccessful = true,
                errorMessage = null,
                bboxLeft = bboxLeft,
                bboxTop = bboxTop,
                bboxRight = bboxRight,
                bboxBottom = bboxBottom,
                feretDiameterPx = feretDiameterPx,
                feretPoint1X = feretP1.first,
                feretPoint1Y = feretP1.second,
                feretPoint2X = feretP2.first,
                feretPoint2Y = feretP2.second,
                measurementSource = measurementSource,
                contourPoints = contourPoints,
                heightPx = finalHeightPx,
                heightTopX = heightTopX,
                heightTopY = heightTopY,
                heightBottomX = heightBottomX,
                heightBottomY = heightBottomY,
                heightMeasurementSource = heightMeasurementSource
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error during TFLite inference execution: ${e.message}", e)
            CvResult(
                isSuccessful = false,
                errorMessage = "Image analysis encountered an unexpected issue. Please retake the photo."
            )
        }
    }

    fun close() {
        interpreter?.close()
        interpreter = null
        isModelLoaded = false
    }
}
