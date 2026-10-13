package com.theis.agrinovis.ml.cv

/**
 * Data contract representing the output produced by the Computer Vision model.
 *
 * @property diameterCm Extracted lettuce diameter in centimeters.
 * @property heightCm Extracted lettuce height in centimeters.
 * @property confidence Model detection/segmentation confidence score (0.0 to 1.0).
 * @property modelVersion Deployed CV model version identifier.
 * @property isSuccessful True if model successfully extracted measurements.
 * @property errorMessage Error description if image processing or model execution failed.
 * @property bboxLeft Bounding box left coordinate in 640x640 model pixel space.
 * @property bboxTop Bounding box top coordinate in 640x640 model pixel space.
 * @property bboxRight Bounding box right coordinate in 640x640 model pixel space.
 * @property bboxBottom Bounding box bottom coordinate in 640x640 model pixel space.
 * @property feretDiameterPx Maximum Feret diameter distance in model pixels.
 * @property feretPoint1X Feret endpoint 1 X coordinate in 640x640 model pixel space.
 * @property feretPoint1Y Feret endpoint 1 Y coordinate in 640x640 model pixel space.
 * @property feretPoint2X Feret endpoint 2 X coordinate in 640x640 model pixel space.
 * @property feretPoint2Y Feret endpoint 2 Y coordinate in 640x640 model pixel space.
 * @property measurementSource Measurement calculation source ("FERET_DIAMETER" or "BOUNDING_BOX").
 * @property contourPoints Contour boundary points in 640x640 model pixel space for drawing mask outline.
 * @property heightPx Vertical height in model pixels.
 * @property heightTopX Height top endpoint X coordinate in 640x640 model pixel space.
 * @property heightTopY Height top endpoint Y coordinate in 640x640 model pixel space.
 * @property heightBottomX Height bottom endpoint X coordinate in 640x640 model pixel space.
 * @property heightBottomY Height bottom endpoint Y coordinate in 640x640 model pixel space.
 * @property heightMeasurementSource Height calculation source ("VERTICAL_MASK_HEIGHT" or "BOUNDING_BOX").
 */
data class CvResult(
    val diameterCm: Float = 0f,
    val heightCm: Float = 0f,
    val confidence: Float = 0f,
    val modelVersion: String = "v1.0-cv",
    val isSuccessful: Boolean = false,
    val errorMessage: String? = null,
    val bboxLeft: Float = 0f,
    val bboxTop: Float = 0f,
    val bboxRight: Float = 0f,
    val bboxBottom: Float = 0f,
    val feretDiameterPx: Float = 0f,
    val feretPoint1X: Float = 0f,
    val feretPoint1Y: Float = 0f,
    val feretPoint2X: Float = 0f,
    val feretPoint2Y: Float = 0f,
    val measurementSource: String = "BOUNDING_BOX",
    val contourPoints: List<Pair<Float, Float>> = emptyList(),
    val heightPx: Float = 0f,
    val heightTopX: Float = 0f,
    val heightTopY: Float = 0f,
    val heightBottomX: Float = 0f,
    val heightBottomY: Float = 0f,
    val heightMeasurementSource: String = "BOUNDING_BOX"
)
