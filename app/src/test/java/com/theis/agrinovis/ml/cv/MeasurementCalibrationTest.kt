package com.theis.agrinovis.ml.cv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MeasurementCalibrationTest {

    @Test
    fun pixelsToCm_returnsCorrectCentimeters_forStandardInputs() {
        // 1000 pixels / 10 pixelsPerMm = 100 mm = 10.0 cm
        val result = TFLiteCvEngine.pixelsToCm(1000f, 10f)
        assertEquals(10.0f, result!!, 0.001f)
    }

    @Test
    fun pixelsToCm_returnsCorrectCentimeters_forDiameterCalibration() {
        // testPixels with calibration DIAMETER_PIXELS_PER_MM = 10 mm = 1.0 cm
        val testPixels = TFLiteCvEngine.DIAMETER_PIXELS_PER_MM * 10f
        val result = TFLiteCvEngine.pixelsToCm(testPixels, TFLiteCvEngine.DIAMETER_PIXELS_PER_MM)
        assertEquals(1.0f, result!!, 0.001f)
    }

    @Test
    fun pixelsToCm_returnsCorrectCentimeters_forHeightCalibration() {
        // testPixels with calibration HEIGHT_PIXELS_PER_MM = 10 mm = 1.0 cm
        val testPixels = TFLiteCvEngine.HEIGHT_PIXELS_PER_MM * 10f
        val result = TFLiteCvEngine.pixelsToCm(testPixels, TFLiteCvEngine.HEIGHT_PIXELS_PER_MM)
        assertEquals(1.0f, result!!, 0.001f)
    }

    @Test
    fun pixelsToCm_returnsNull_forZeroPixels() {
        assertNull(TFLiteCvEngine.pixelsToCm(0f, 10f))
    }

    @Test
    fun pixelsToCm_returnsNull_forNegativePixels() {
        assertNull(TFLiteCvEngine.pixelsToCm(-100f, 10f))
    }

    @Test
    fun pixelsToCm_returnsNull_forZeroOrNegativeCalibration() {
        assertNull(TFLiteCvEngine.pixelsToCm(100f, 0f))
        assertNull(TFLiteCvEngine.pixelsToCm(100f, -5f))
    }

    @Test
    fun pixelsToCm_returnsNull_forNonFiniteInputs() {
        assertNull(TFLiteCvEngine.pixelsToCm(Float.NaN, 10f))
        assertNull(TFLiteCvEngine.pixelsToCm(100f, Float.POSITIVE_INFINITY))
    }
}
