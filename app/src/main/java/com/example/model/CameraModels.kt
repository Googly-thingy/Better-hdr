package com.example.model

import android.graphics.Bitmap

enum class HistogramChannel {
  LUMINANCE,
  RGB,
  RED,
  GREEN,
  BLUE
}

data class HistogramData(
  val lumaBins: FloatArray = FloatArray(64) { 0f },
  val redBins: FloatArray = FloatArray(64) { 0f },
  val greenBins: FloatArray = FloatArray(64) { 0f },
  val blueBins: FloatArray = FloatArray(64) { 0f },
  val shadowClippingPercent: Float = 0f,
  val highlightClippingPercent: Float = 0f,
  val dynamicRangeEv: Float = 0f,
  val meanLuma: Float = 128f
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as HistogramData
    return lumaBins.contentEquals(other.lumaBins) &&
      redBins.contentEquals(other.redBins) &&
      greenBins.contentEquals(other.greenBins) &&
      blueBins.contentEquals(other.blueBins) &&
      shadowClippingPercent == other.shadowClippingPercent &&
      highlightClippingPercent == other.highlightClippingPercent &&
      dynamicRangeEv == other.dynamicRangeEv &&
      meanLuma == other.meanLuma
  }

  override fun hashCode(): Int {
    var result = lumaBins.contentHashCode()
    result = 31 * result + redBins.contentHashCode()
    result = 31 * result + greenBins.contentHashCode()
    result = 31 * result + blueBins.contentHashCode()
    result = 31 * result + shadowClippingPercent.hashCode()
    result = 31 * result + highlightClippingPercent.hashCode()
    result = 31 * result + dynamicRangeEv.hashCode()
    result = 31 * result + meanLuma.hashCode()
    return result
  }
}

enum class BacklightSeverity {
  NONE,
  MILD,
  HIGH,
  EXTREME
}

data class BacklightState(
  val severity: BacklightSeverity = BacklightSeverity.NONE,
  val contrastRatio: Float = 1.0f,
  val recommendedEvCompensation: Float = 0.0f,
  val centerLuma: Float = 128f,
  val perimeterLuma: Float = 128f,
  val message: String = "Normal balanced scene"
)

enum class HdrMode(val label: String, val description: String) {
  OFF("HDR Off", "Standard sensor exposure without dynamic tone adjustment"),
  AUTO("Auto HDR", "Engages HDR when strong backlight contrast is detected"),
  BACKLIGHT_BOOST("Backlight Boost", "Adaptive shadow lift & highlight recovery for backlit subjects"),
  MULTI_FRAME_HDR("Multi-Frame HDR", "High-contrast multi-exposure bracketing fusion")
}

data class HdrSettings(
  val mode: HdrMode = HdrMode.BACKLIGHT_BOOST,
  val shadowLift: Float = 0.70f,
  val highlightRecovery: Float = 0.75f,
  val localContrastBoost: Float = 0.50f,
  val fillLightEmulation: Boolean = true,
  val showZebraAlerts: Boolean = true
)

enum class TestScene(val label: String, val drawableRes: Int?) {
  LIVE_CAMERA("Live Camera", null),
  SUNSET_PORTRAIT("Sunset Portrait (Extreme Backlight)", com.example.R.drawable.test_scene_sunset),
  WINDOW_INTERIOR("Bright Window Interior", com.example.R.drawable.test_scene_window)
}

data class CapturedPhoto(
  val id: String = System.currentTimeMillis().toString(),
  val timestamp: Long = System.currentTimeMillis(),
  val originalBitmap: Bitmap,
  val hdrBitmap: Bitmap,
  val evBias: Float = 0f,
  val hdrModeUsed: HdrMode = HdrMode.BACKLIGHT_BOOST,
  val contrastRatio: Float = 1f,
  val histogramBefore: HistogramData = HistogramData(),
  val histogramAfter: HistogramData = HistogramData()
)
