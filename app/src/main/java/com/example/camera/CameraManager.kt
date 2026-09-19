package com.example.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Rational
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.model.BacklightState
import com.example.model.HistogramData
import com.example.processing.BacklightHdrProcessor
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CameraManager(private val context: Context) {

  private var cameraProvider: ProcessCameraProvider? = null
  private var camera: Camera? = null
  private var preview: Preview? = null
  private var imageCapture: ImageCapture? = null
  private var imageAnalysis: ImageAnalysis? = null
  private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

  var onHistogramUpdate: ((HistogramData, BacklightState) -> Unit)? = null
  var onExposureRangeChanged: ((minEv: Float, maxEv: Float, step: Float) -> Unit)? = null

  private var currentStep: Float = 0.166667f
  private var currentMinIndex: Int = -12
  private var currentMaxIndex: Int = 12

  suspend fun initializeCamera(
    lifecycleOwner: LifecycleOwner,
    surfaceProvider: Preview.SurfaceProvider
  ): Boolean = withContext(Dispatchers.Main) {
    try {
      val provider = getCameraProvider()
      cameraProvider = provider

      provider.unbindAll()

      val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

      preview = Preview.Builder().build().also {
        it.surfaceProvider = surfaceProvider
      }

      imageCapture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
        .build()

      imageAnalysis = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
        .build().also { analysis ->
          analysis.setAnalyzer(cameraExecutor) { imageProxy ->
            try {
              val (histData, backlightState) = BacklightHdrProcessor.analyzeImageProxy(imageProxy)
              onHistogramUpdate?.invoke(histData, backlightState)
            } catch (e: Exception) {
              // ignore analyzer frame errors
            } finally {
              imageProxy.close()
            }
          }
        }

      camera = provider.bindToLifecycle(
        lifecycleOwner,
        cameraSelector,
        preview,
        imageCapture,
        imageAnalysis
      )

      val exposureState = camera?.cameraInfo?.exposureState
      if (exposureState != null && exposureState.isExposureCompensationSupported) {
        val range = exposureState.exposureCompensationRange
        val stepRational = exposureState.exposureCompensationStep
        currentStep = stepRational.toFloat()
        currentMinIndex = range.lower
        currentMaxIndex = range.upper
        val minEv = currentMinIndex * currentStep
        val maxEv = currentMaxIndex * currentStep
        onExposureRangeChanged?.invoke(minEv, maxEv, currentStep)
      } else {
        // Fallback standard range
        currentStep = 0.333f
        currentMinIndex = -6
        currentMaxIndex = 6
        onExposureRangeChanged?.invoke(-2.0f, 2.0f, 0.333f)
      }

      true
    } catch (e: Exception) {
      e.printStackTrace()
      false
    }
  }

  fun setExposureCompensation(ev: Float) {
    val cam = camera ?: return
    val index = (ev / currentStep).toInt().coerceIn(currentMinIndex, currentMaxIndex)
    cam.cameraControl.setExposureCompensationIndex(index)
  }

  fun setFlashMode(torchOn: Boolean) {
    camera?.cameraControl?.enableTorch(torchOn)
  }

  fun triggerSpotMetering(normX: Float, normY: Float, viewWidth: Float, viewHeight: Float) {
    val cam = camera ?: return
    val factory = SurfaceOrientedMeteringPointFactory(viewWidth, viewHeight)
    val point = factory.createPoint(normX * viewWidth, normY * viewHeight)
    val action = FocusMeteringAction.Builder(
      point,
      FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AF
    ).setAutoCancelDuration(4000, java.util.concurrent.TimeUnit.MILLISECONDS)
      .build()
    cam.cameraControl.startFocusAndMetering(action)
  }

  suspend fun capturePhoto(): Bitmap? = suspendCoroutine { continuation ->
    val capture = imageCapture
    if (capture == null) {
      continuation.resume(null)
      return@suspendCoroutine
    }

    capture.takePicture(
      cameraExecutor,
      object : ImageCapture.OnImageCapturedCallback() {
        override fun onCaptureSuccess(image: ImageProxy) {
          try {
            val bitmap = imageProxyToBitmap(image)
            continuation.resume(bitmap)
          } catch (e: Exception) {
            continuation.resumeWithException(e)
          } finally {
            image.close()
          }
        }

        override fun onError(exception: ImageCaptureException) {
          continuation.resumeWithException(exception)
        }
      }
    )
  }

  private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    val rotation = image.imageInfo.rotationDegrees
    return if (rotation != 0) {
      val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
      Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
    } else {
      original
    }
  }

  private suspend fun getCameraProvider(): ProcessCameraProvider =
    suspendCoroutine { continuation ->
      val future = ProcessCameraProvider.getInstance(context)
      future.addListener(
        {
          try {
            continuation.resume(future.get())
          } catch (e: Exception) {
            continuation.resumeWithException(e)
          }
        },
        ContextCompat.getMainExecutor(context)
      )
    }

  fun release() {
    cameraProvider?.unbindAll()
    cameraExecutor.shutdown()
  }
}
