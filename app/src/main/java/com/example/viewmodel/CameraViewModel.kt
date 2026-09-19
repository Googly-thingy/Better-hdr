package com.example.viewmodel

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.camera.CameraManager
import com.example.model.BacklightSeverity
import com.example.model.BacklightState
import com.example.model.CapturedPhoto
import com.example.model.HdrMode
import com.example.model.HdrSettings
import com.example.model.HistogramChannel
import com.example.model.HistogramData
import com.example.model.TestScene
import com.example.processing.BacklightHdrProcessor
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CameraUiState(
  val selectedScene: TestScene = TestScene.LIVE_CAMERA,
  val hasCameraPermission: Boolean = false,
  val manualEvCompensation: Float = 0.0f,
  val minEv: Float = -3.0f,
  val maxEv: Float = 3.0f,
  val evStep: Float = 0.166667f,
  val histogramData: HistogramData = HistogramData(),
  val backlightState: BacklightState = BacklightState(),
  val hdrSettings: HdrSettings = HdrSettings(),
  val histogramChannel: HistogramChannel = HistogramChannel.RGB,
  val isHistogramVisible: Boolean = true,
  val isFlashOn: Boolean = false,
  val isAeLocked: Boolean = false,
  val isCapturing: Boolean = false,
  val capturedPhoto: CapturedPhoto? = null,
  val recentPhotos: List<CapturedPhoto> = emptyList(),
  val tapFocusPoint: Offset? = null,
  val isLiveHdrSimulationActive: Boolean = false,
  val simulatedTestBitmap: Bitmap? = null,
  val statusToast: String? = null
)

class CameraViewModel : ViewModel() {

  private val _uiState = MutableStateFlow(CameraUiState())
  val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

  private var cameraManager: CameraManager? = null

  fun setCameraManager(manager: CameraManager) {
    this.cameraManager = manager
    manager.onHistogramUpdate = { histData, backlightState ->
      if (_uiState.value.selectedScene == TestScene.LIVE_CAMERA) {
        _uiState.update { it.copy(histogramData = histData, backlightState = backlightState) }
      }
    }
    manager.onExposureRangeChanged = { minEv, maxEv, step ->
      _uiState.update { it.copy(minEv = minEv, maxEv = maxEv, evStep = step) }
    }
  }

  fun setCameraPermissionGranted(granted: Boolean, context: Context) {
    _uiState.update { it.copy(hasCameraPermission = granted) }
    if (!granted && _uiState.value.selectedScene == TestScene.LIVE_CAMERA) {
      // If camera permission not available, automatically switch to high-contrast test scene
      selectScene(TestScene.SUNSET_PORTRAIT, context)
    }
  }

  fun selectScene(scene: TestScene, context: Context) {
    _uiState.update { it.copy(selectedScene = scene) }
    if (scene != TestScene.LIVE_CAMERA && scene.drawableRes != null) {
      viewModelScope.launch(Dispatchers.Default) {
        val bitmap = BitmapFactory.decodeResource(context.resources, scene.drawableRes)
        val (hist, backlight) = BacklightHdrProcessor.analyzeBitmap(bitmap)
        _uiState.update {
          it.copy(
            simulatedTestBitmap = bitmap,
            histogramData = hist,
            backlightState = backlight,
            manualEvCompensation = 0f
          )
        }
      }
    }
  }

  fun setEvCompensation(ev: Float) {
    val clamped = ev.coerceIn(_uiState.value.minEv, _uiState.value.maxEv)
    val rounded = Math.round(clamped * 10f) / 10f
    _uiState.update { it.copy(manualEvCompensation = rounded) }

    if (_uiState.value.selectedScene == TestScene.LIVE_CAMERA) {
      cameraManager?.setExposureCompensation(rounded)
    } else {
      // Recompute histogram on simulated scene with EV bias
      _uiState.value.simulatedTestBitmap?.let { bmp ->
        viewModelScope.launch(Dispatchers.Default) {
          val biased = BacklightHdrProcessor.processBacklightHdr(
            source = bmp,
            evBias = rounded,
            shadowLift = 0f,
            highlightRecovery = 0f,
            fillLightEmulation = false
          )
          val (hist, backlight) = BacklightHdrProcessor.analyzeBitmap(biased)
          _uiState.update { it.copy(histogramData = hist, backlightState = backlight) }
        }
      }
    }
  }

  fun applyRecommendedEv() {
    val rec = _uiState.value.backlightState.recommendedEvCompensation
    setEvCompensation(rec)
    showToast("Applied Recommended EV: +${rec} EV")
  }

  fun resetEvCompensation() {
    setEvCompensation(0.0f)
  }

  fun setHdrMode(mode: HdrMode) {
    _uiState.update { it.copy(hdrSettings = it.hdrSettings.copy(mode = mode)) }
  }

  fun updateHdrSettings(
    shadowLift: Float? = null,
    highlightRecovery: Float? = null,
    fillLight: Boolean? = null,
    showZebra: Boolean? = null
  ) {
    _uiState.update { current ->
      current.copy(
        hdrSettings = current.hdrSettings.copy(
          shadowLift = shadowLift ?: current.hdrSettings.shadowLift,
          highlightRecovery = highlightRecovery ?: current.hdrSettings.highlightRecovery,
          fillLightEmulation = fillLight ?: current.hdrSettings.fillLightEmulation,
          showZebraAlerts = showZebra ?: current.hdrSettings.showZebraAlerts
        )
      )
    }
  }

  fun setHistogramChannel(channel: HistogramChannel) {
    _uiState.update { it.copy(histogramChannel = channel) }
  }

  fun toggleHistogramVisibility() {
    _uiState.update { it.copy(isHistogramVisible = !it.isHistogramVisible) }
  }

  fun toggleFlash() {
    val next = !_uiState.value.isFlashOn
    _uiState.update { it.copy(isFlashOn = next) }
    cameraManager?.setFlashMode(next)
  }

  fun toggleLiveHdrSimulation() {
    _uiState.update { it.copy(isLiveHdrSimulationActive = !it.isLiveHdrSimulationActive) }
  }

  fun onFocusTap(normX: Float, normY: Float, viewWidth: Float, viewHeight: Float) {
    _uiState.update { it.copy(tapFocusPoint = Offset(normX * viewWidth, normY * viewHeight)) }
    if (_uiState.value.selectedScene == TestScene.LIVE_CAMERA) {
      cameraManager?.triggerSpotMetering(normX, normY, viewWidth, viewHeight)
    }
    viewModelScope.launch {
      kotlinx.coroutines.delay(3000)
      _uiState.update { if (it.tapFocusPoint == Offset(normX * viewWidth, normY * viewHeight)) it.copy(tapFocusPoint = null) else it }
    }
  }

  fun capturePhoto(context: Context) {
    val state = _uiState.value
    if (state.isCapturing) return

    _uiState.update { it.copy(isCapturing = true) }

    viewModelScope.launch(Dispatchers.Default) {
      try {
        val rawBitmap: Bitmap? = if (state.selectedScene == TestScene.LIVE_CAMERA) {
          cameraManager?.capturePhoto()
        } else {
          state.simulatedTestBitmap
        }

        if (rawBitmap == null) {
          withContext(Dispatchers.Main) {
            _uiState.update { it.copy(isCapturing = false) }
            showToast("Failed to acquire photo frame")
          }
          return@launch
        }

        // Apply EV bias to raw frame if simulating or for unboosted comparison
        val original = if (state.selectedScene != TestScene.LIVE_CAMERA && state.manualEvCompensation != 0f) {
          BacklightHdrProcessor.processBacklightHdr(
            source = rawBitmap,
            evBias = state.manualEvCompensation,
            shadowLift = 0f,
            highlightRecovery = 0f,
            fillLightEmulation = false
          )
        } else {
          rawBitmap
        }

        val isMultiFrame = state.hdrSettings.mode == HdrMode.MULTI_FRAME_HDR
        val shadowLift = if (state.hdrSettings.mode == HdrMode.OFF) 0f else state.hdrSettings.shadowLift
        val highlightRecovery = if (state.hdrSettings.mode == HdrMode.OFF) 0f else state.hdrSettings.highlightRecovery
        val fillLight = if (state.hdrSettings.mode == HdrMode.OFF) false else state.hdrSettings.fillLightEmulation

        val hdrBitmap = BacklightHdrProcessor.processBacklightHdr(
          source = original,
          evBias = state.manualEvCompensation,
          shadowLift = shadowLift,
          highlightRecovery = highlightRecovery,
          fillLightEmulation = fillLight,
          isMultiFrameMode = isMultiFrame
        )

        val (histBefore, _) = BacklightHdrProcessor.analyzeBitmap(original)
        val (histAfter, _) = BacklightHdrProcessor.analyzeBitmap(hdrBitmap)

        val captured = CapturedPhoto(
          originalBitmap = original,
          hdrBitmap = hdrBitmap,
          evBias = state.manualEvCompensation,
          hdrModeUsed = state.hdrSettings.mode,
          contrastRatio = state.backlightState.contrastRatio,
          histogramBefore = histBefore,
          histogramAfter = histAfter
        )

        withContext(Dispatchers.Main) {
          _uiState.update { current ->
            current.copy(
              isCapturing = false,
              capturedPhoto = captured,
              recentPhotos = listOf(captured) + current.recentPhotos.take(5)
            )
          }
        }
      } catch (e: Exception) {
        e.printStackTrace()
        withContext(Dispatchers.Main) {
          _uiState.update { it.copy(isCapturing = false) }
          showToast("Capture error: ${e.message}")
        }
      }
    }
  }

  fun savePhotoToGallery(context: Context, photo: CapturedPhoto) {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
          val filename = "BACKLIGHT_HDR_${System.currentTimeMillis()}.jpg"
          put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
          put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/BacklightHDR")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
          }
        }

        val uri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
          resolver.openOutputStream(uri)?.use { stream: OutputStream ->
            photo.hdrBitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
          }

          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
          }

          withContext(Dispatchers.Main) {
            showToast("Saved Backlight HDR photo to Gallery!")
          }
        }
      } catch (e: Exception) {
        e.printStackTrace()
        withContext(Dispatchers.Main) {
          showToast("Failed to save photo: ${e.message}")
        }
      }
    }
  }

  fun dismissCapturedPhoto() {
    _uiState.update { it.copy(capturedPhoto = null) }
  }

  fun showToast(msg: String) {
    _uiState.update { it.copy(statusToast = msg) }
  }

  fun clearToast() {
    _uiState.update { it.copy(statusToast = null) }
  }
}
