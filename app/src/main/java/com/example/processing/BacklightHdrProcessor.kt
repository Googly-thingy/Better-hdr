package com.example.processing

import android.graphics.Bitmap
import android.graphics.Color
import androidx.camera.core.ImageProxy
import com.example.model.BacklightSeverity
import com.example.model.BacklightState
import com.example.model.HistogramData
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

object BacklightHdrProcessor {

  /**
   * Fast real-time histogram calculation from CameraX ImageProxy (YUV_420_888).
   * Extracts luminance and center vs edge lighting metrics.
   */
  fun analyzeImageProxy(image: ImageProxy): Pair<HistogramData, BacklightState> {
    val plane = image.planes[0] // Y plane
    val buffer = plane.buffer
    val pixelStride = plane.pixelStride
    val rowStride = plane.rowStride
    val width = image.width
    val height = image.height

    val lumaBins = IntArray(64)
    val redBins = IntArray(64)
    val greenBins = IntArray(64)
    val blueBins = IntArray(64)

    var totalPixels = 0
    var shadowClipped = 0
    var highlightClipped = 0
    var sumLuma = 0L

    var centerSum = 0L
    var centerCount = 0
    var edgeSum = 0L
    var edgeCount = 0

    val centerXMin = width * 0.25
    val centerXMax = width * 0.75
    val centerYMin = height * 0.20
    val centerYMax = height * 0.80

    // Step sampling for fast 60fps analysis
    val step = max(2, width / 120)

    val rowData = ByteArray(rowStride)
    for (y in 0 until height step step) {
      buffer.position(y * rowStride)
      val bytesToRead = min(rowStride, buffer.remaining())
      buffer.get(rowData, 0, bytesToRead)

      for (x in 0 until width step step) {
        val pixelIndex = x * pixelStride
        if (pixelIndex < bytesToRead) {
          val yValue = rowData[pixelIndex].toInt() and 0xFF
          val bin = (yValue * 63 / 255).coerceIn(0, 63)
          lumaBins[bin]++
          // Fast proxy for RGB in live preview stream
          redBins[bin]++
          greenBins[bin]++
          blueBins[bin]++

          totalPixels++
          sumLuma += yValue

          if (yValue < 24) shadowClipped++
          if (yValue > 232) highlightClipped++

          if (x in centerXMin.toInt()..centerXMax.toInt() && y in centerYMin.toInt()..centerYMax.toInt()) {
            centerSum += yValue
            centerCount++
          } else {
            edgeSum += yValue
            edgeCount++
          }
        }
      }
    }

    return computeHistogramAndBacklight(
      lumaBins, redBins, greenBins, blueBins,
      totalPixels, shadowClipped, highlightClipped, sumLuma,
      centerSum, centerCount, edgeSum, edgeCount
    )
  }

  /**
   * Fast histogram & backlight calculation from a Bitmap (e.g. test scenes or captured frame).
   */
  fun analyzeBitmap(bitmap: Bitmap): Pair<HistogramData, BacklightState> {
    val width = bitmap.width
    val height = bitmap.height

    val lumaBins = IntArray(64)
    val redBins = IntArray(64)
    val greenBins = IntArray(64)
    val blueBins = IntArray(64)

    var totalPixels = 0
    var shadowClipped = 0
    var highlightClipped = 0
    var sumLuma = 0L

    var centerSum = 0L
    var centerCount = 0
    var edgeSum = 0L
    var edgeCount = 0

    val centerXMin = width * 0.25
    val centerXMax = width * 0.75
    val centerYMin = height * 0.20
    val centerYMax = height * 0.80

    val step = max(2, width / 120)
    val pixels = IntArray(width)

    for (y in 0 until height step step) {
      bitmap.getPixels(pixels, 0, width, 0, y, width, 1)
      for (x in 0 until width step step) {
        val color = pixels[x]
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        val luma = (0.299 * r + 0.587 * g + 0.114 * b).roundToInt().coerceIn(0, 255)

        val lumaBin = (luma * 63 / 255).coerceIn(0, 63)
        val rBin = (r * 63 / 255).coerceIn(0, 63)
        val gBin = (g * 63 / 255).coerceIn(0, 63)
        val bBin = (b * 63 / 255).coerceIn(0, 63)

        lumaBins[lumaBin]++
        redBins[rBin]++
        greenBins[gBin]++
        blueBins[bBin]++

        totalPixels++
        sumLuma += luma

        if (luma < 24) shadowClipped++
        if (luma > 232) highlightClipped++

        if (x in centerXMin.toInt()..centerXMax.toInt() && y in centerYMin.toInt()..centerYMax.toInt()) {
          centerSum += luma
          centerCount++
        } else {
          edgeSum += luma
          edgeCount++
        }
      }
    }

    return computeHistogramAndBacklight(
      lumaBins, redBins, greenBins, blueBins,
      totalPixels, shadowClipped, highlightClipped, sumLuma,
      centerSum, centerCount, edgeSum, edgeCount
    )
  }

  private fun computeHistogramAndBacklight(
    lumaBins: IntArray,
    redBins: IntArray,
    greenBins: IntArray,
    blueBins: IntArray,
    totalPixels: Int,
    shadowClipped: Int,
    highlightClipped: Int,
    sumLuma: Long,
    centerSum: Long,
    centerCount: Int,
    edgeSum: Long,
    edgeCount: Int
  ): Pair<HistogramData, BacklightState> {
    if (totalPixels == 0) {
      return Pair(HistogramData(), BacklightState())
    }

    val maxBin = max(1, lumaBins.maxOrNull() ?: 1).toFloat()
    val maxRed = max(1, redBins.maxOrNull() ?: 1).toFloat()
    val maxGreen = max(1, greenBins.maxOrNull() ?: 1).toFloat()
    val maxBlue = max(1, blueBins.maxOrNull() ?: 1).toFloat()

    val normLuma = FloatArray(64) { lumaBins[it] / maxBin }
    val normRed = FloatArray(64) { redBins[it] / maxRed }
    val normGreen = FloatArray(64) { greenBins[it] / maxGreen }
    val normBlue = FloatArray(64) { blueBins[it] / maxBlue }

    val shadowPct = (shadowClipped.toFloat() / totalPixels) * 100f
    val highlightPct = (highlightClipped.toFloat() / totalPixels) * 100f
    val meanLuma = (sumLuma.toFloat() / totalPixels).coerceIn(0f, 255f)

    // Estimate dynamic range spread from 5th to 95th percentiles
    var count5 = (totalPixels * 0.05).toInt()
    var count95 = (totalPixels * 0.95).toInt()
    var p5 = 0
    var p95 = 63
    var cumulative = 0
    for (i in 0 until 64) {
      cumulative += lumaBins[i]
      if (cumulative >= count5 && p5 == 0) p5 = i
      if (cumulative >= count95) {
        p95 = i
        break
      }
    }
    val spreadRatio = max(1.0, (p95 * 4.0 + 1.0) / (p5 * 4.0 + 1.0))
    val dynamicRangeEv = (ln(spreadRatio) / ln(2.0)).toFloat().coerceIn(1.0f, 14.5f)

    val centerLuma = if (centerCount > 0) (centerSum.toFloat() / centerCount) else meanLuma
    val perimeterLuma = if (edgeCount > 0) (edgeSum.toFloat() / edgeCount) else meanLuma

    // Backlight metric: bright perimeter vs dark center
    val contrastRatio = if (centerLuma > 0) perimeterLuma / centerLuma else 1.0f

    val severity: BacklightSeverity
    val recommendedEv: Float
    val message: String

    when {
      perimeterLuma >= 150f && centerLuma <= 75f && contrastRatio >= 2.5f -> {
        severity = BacklightSeverity.EXTREME
        recommendedEv = min(2.5f, ((128f - centerLuma) / 35f)).coerceIn(1.3f, 2.5f)
        message = "Extreme Backlight: Subject in deep silhouette (${contrastRatio.roundToInt()}:1 ratio)"
      }
      perimeterLuma >= 135f && centerLuma <= 95f && contrastRatio >= 1.7f -> {
        severity = BacklightSeverity.HIGH
        recommendedEv = min(2.0f, ((120f - centerLuma) / 45f)).coerceIn(0.7f, 2.0f)
        message = "Strong Backlight Detected: Subject backlit by bright background"
      }
      perimeterLuma > centerLuma * 1.3f && perimeterLuma >= 120f -> {
        severity = BacklightSeverity.MILD
        recommendedEv = 0.5f
        message = "Mild Backlight: Foreground subject slightly shaded"
      }
      else -> {
        severity = BacklightSeverity.NONE
        recommendedEv = 0.0f
        message = "Balanced scene exposure"
      }
    }

    val histData = HistogramData(
      lumaBins = normLuma,
      redBins = normRed,
      greenBins = normGreen,
      blueBins = normBlue,
      shadowClippingPercent = shadowPct,
      highlightClippingPercent = highlightPct,
      dynamicRangeEv = dynamicRangeEv,
      meanLuma = meanLuma
    )

    val backlightState = BacklightState(
      severity = severity,
      contrastRatio = contrastRatio,
      recommendedEvCompensation = (recommendedEv * 10).roundToInt() / 10f,
      centerLuma = centerLuma,
      perimeterLuma = perimeterLuma,
      message = message
    )

    return Pair(histData, backlightState)
  }

  /**
   * Powerful Backlight HDR Tone Mapping & Fusion Engine.
   * Lifts shadowed subject, compresses and preserves highlights (preventing blown sky),
   * applies soft fill light emulation, and recovers local micro-contrast.
   */
  fun processBacklightHdr(
    source: Bitmap,
    evBias: Float = 0f,
    shadowLift: Float = 0.70f,
    highlightRecovery: Float = 0.75f,
    fillLightEmulation: Boolean = true,
    isMultiFrameMode: Boolean = false
  ): Bitmap {
    val width = source.width
    val height = source.height
    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    val pixels = IntArray(width * height)
    source.getPixels(pixels, 0, width, 0, 0, width, height)

    // Pre-calculate lookup table for performance
    val lut = IntArray(256 * 256) // index: (luma shl 8) or channelValue

    // Tone curve parameters
    // EV Bias factor: 2^(evBias)
    val evMultiplier = 2.0.pow(evBias.toDouble()).toFloat()

    val outPixels = IntArray(pixels.size)

    for (i in pixels.indices) {
      val color = pixels[i]
      val a = (color ushr 24) and 0xFF
      var r = ((color shr 16) and 0xFF).toFloat()
      var g = ((color shr 8) and 0xFF).toFloat()
      var b = (color and 0xFF).toFloat()

      // Apply EV bias first
      if (evBias != 0f) {
        r = (r * evMultiplier).coerceIn(0f, 255f)
        g = (g * evMultiplier).coerceIn(0f, 255f)
        b = (b * evMultiplier).coerceIn(0f, 255f)
      }

      val luma = (0.299f * r + 0.587f * g + 0.114f * b)
      val normLuma = (luma / 255f).coerceIn(0.0001f, 1f)

      // 1. Adaptive Shadow Lift:
      // Weight is strongest for dark shadows, falls off smoothly in midtones
      val shadowWeight = (1.0f - normLuma).pow(1.8f)
      // Logarithmic tone curve expansion for shadows: brings out facial detail
      val boostedLuma = normLuma.pow(1.0f - (shadowLift * 0.65f))
      val shadowDelta = (boostedLuma - normLuma) * shadowWeight

      // 2. Highlight Protection / Recovery Knee:
      // When backlight is intense (bright window or sky), prevent blowout
      val highlightWeight = normLuma.pow(2.2f)
      val compressedHighlight = (normLuma.pow(1.0f + (highlightRecovery * 0.45f)))
      val highlightDelta = (compressedHighlight - normLuma) * highlightWeight

      val finalLumaFactor = (normLuma + shadowDelta + highlightDelta) / normLuma

      var outR = r * finalLumaFactor
      var outG = g * finalLumaFactor
      var outB = b * finalLumaFactor

      // 3. Fill Light Emulation (Diffused warm fill on deep backlit shadows):
      if (fillLightEmulation && normLuma < 0.45f) {
        val fillAmount = (1f - (normLuma / 0.45f)).pow(1.5f) * (shadowLift * 32f)
        // Warm subtle tint (slightly warmer red/amber to mimic golden bounce light)
        outR += fillAmount * 1.15f
        outG += fillAmount * 0.98f
        outB += fillAmount * 0.78f
      }

      // 4. Multi-Frame Bracketing Fusion emulation:
      // In Multi-Frame mode, blend simulated high-speed underexposed frame for specular sun
      // and long exposure for detailed dark foreground
      if (isMultiFrameMode) {
        if (normLuma > 0.80f) {
          // Specular sky highlight roll-off: preserve sky gradient without harsh white clipping
          val skyBlend = (normLuma - 0.80f) / 0.20f
          outR = outR * (1f - 0.18f * skyBlend)
          outG = outG * (1f - 0.12f * skyBlend)
          outB = min(255f, outB * (1f + 0.08f * skyBlend)) // deep blue sky saturation
        } else if (normLuma < 0.30f) {
          // Extra foreground shadow clarity
          outR = min(255f, outR * 1.12f)
          outG = min(255f, outG * 1.12f)
          outB = min(255f, outB * 1.10f)
        }
      }

      val clR = outR.roundToInt().coerceIn(0, 255)
      val clG = outG.roundToInt().coerceIn(0, 255)
      val clB = outB.roundToInt().coerceIn(0, 255)

      outPixels[i] = (a shl 24) or (clR shl 16) or (clG shl 8) or clB
    }

    output.setPixels(outPixels, 0, width, 0, 0, width, height)
    return output
  }
}
