package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.camera.CameraManager
import com.example.model.BacklightSeverity
import com.example.model.HdrMode
import com.example.model.TestScene
import com.example.processing.BacklightHdrProcessor
import com.example.ui.components.BacklightAlertBanner
import com.example.ui.components.ExposureControlDial
import com.example.ui.components.FocusRing
import com.example.ui.components.HdrComparisonViewer
import com.example.ui.components.HdrSettingsSheet
import com.example.ui.components.HistogramView
import com.example.ui.theme.HdrAmber
import com.example.ui.theme.HdrAmberGlow
import com.example.ui.theme.OptimalExposureGreen
import com.example.ui.theme.TitaniumDark
import com.example.ui.theme.TitaniumSurface
import com.example.ui.theme.TitaniumSurfaceElevated
import com.example.viewmodel.CameraViewModel
import kotlin.math.pow
import kotlinx.coroutines.launch

@Composable
fun BacklightCameraScreen(
  viewModel: CameraViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val uiState by viewModel.uiState.collectAsState()
  val scope = rememberCoroutineScope()

  var showSettingsSheet by remember { mutableStateOf(false) }
  var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
  val cameraManager = remember { CameraManager(context) }

  // Check and request Camera permission
  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    viewModel.setCameraPermissionGranted(isGranted, context)
  }

  LaunchedEffect(Unit) {
    viewModel.setCameraManager(cameraManager)
    val hasPermission = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
    viewModel.setCameraPermissionGranted(hasPermission, context)
    if (!hasPermission) {
      permissionLauncher.launch(Manifest.permission.CAMERA)
    }
  }

  DisposableEffect(lifecycleOwner) {
    onDispose {
      cameraManager.release()
    }
  }

  // Bind live camera when surface is ready and live mode is active
  LaunchedEffect(uiState.selectedScene, uiState.hasCameraPermission, previewViewRef) {
    val pv = previewViewRef
    if (uiState.selectedScene == TestScene.LIVE_CAMERA && uiState.hasCameraPermission && pv != null) {
      cameraManager.initializeCamera(lifecycleOwner, pv.surfaceProvider)
    }
  }

  // Toast notifications
  LaunchedEffect(uiState.statusToast) {
    uiState.statusToast?.let { msg ->
      Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
      viewModel.clearToast()
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(TitaniumDark)
      .testTag("backlight_camera_screen")
  ) {
    // 1. Viewfinder (Live CameraX Feed OR Interactive Test Scene)
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .pointerInput(uiState.selectedScene) {
          detectTapGestures { offset ->
            val normX = (offset.x / size.width).coerceIn(0f, 1f)
            val normY = (offset.y / size.height).coerceIn(0f, 1f)
            viewModel.onFocusTap(normX, normY, size.width.toFloat(), size.height.toFloat())
          }
        }
        .testTag("camera_viewfinder")
    ) {
      val viewWidth = maxWidth
      val viewHeight = maxHeight

      if (uiState.selectedScene == TestScene.LIVE_CAMERA && uiState.hasCameraPermission) {
        AndroidView(
          factory = { ctx ->
            PreviewView(ctx).apply {
              layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
              )
              scaleType = PreviewView.ScaleType.FILL_CENTER
              implementationMode = PreviewView.ImplementationMode.PERFORMANCE
              previewViewRef = this
            }
          },
          modifier = Modifier.fillMaxSize()
        )
      } else {
        // High-Contrast Backlit Scene Simulator
        val testDrawable = uiState.selectedScene.drawableRes ?: com.example.R.drawable.test_scene_sunset
        val testBitmap = uiState.simulatedTestBitmap

        if (testBitmap != null) {
          // Render simulated scene with live EV bias and simulated HDR boost if active
          val evFactor = 2.0.pow(uiState.manualEvCompensation.toDouble()).toFloat()
          val isHdrSim = uiState.isLiveHdrSimulationActive

          Canvas(modifier = Modifier.fillMaxSize()) {
            val imgBmp = testBitmap
            val canvasW = size.width
            val canvasH = size.height

            val paint = Paint().apply {
              isFilterBitmap = true
              if (evFactor != 1.0f || isHdrSim) {
                // Exposure matrix
                val rScale = if (isHdrSim) evFactor * 1.25f else evFactor
                val gScale = if (isHdrSim) evFactor * 1.15f else evFactor
                val bScale = if (isHdrSim) evFactor * 1.05f else evFactor
                val offset = if (isHdrSim) 25f else 0f
                val cm = ColorMatrix(
                  floatArrayOf(
                    rScale, 0f, 0f, 0f, offset,
                    0f, gScale, 0f, 0f, offset,
                    0f, 0f, bScale, 0f, offset,
                    0f, 0f, 0f, 1f, 0f
                  )
                )
                colorFilter = ColorMatrixColorFilter(cm)
              }
            }

            drawIntoCanvas { canvas ->
              val srcRect = android.graphics.Rect(0, 0, imgBmp.width, imgBmp.height)
              val dstRect = android.graphics.Rect(0, 0, canvasW.toInt(), canvasH.toInt())
              canvas.nativeCanvas.drawBitmap(imgBmp, srcRect, dstRect, paint)
            }
          }
        } else {
          Image(
            painter = painterResource(id = testDrawable),
            contentDescription = "Test Backlit Scene",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        }
      }

      // Tap-to-Focus Reticle
      uiState.tapFocusPoint?.let { point ->
        FocusRing(tapPoint = point)
      }

      // Live HDR Simulation Watermark
      if (uiState.isLiveHdrSimulationActive) {
        Box(
          modifier = Modifier
            .align(Alignment.Center)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
          Text(
            text = "LIVE HDR TONE MAPPING PREVIEW",
            style = MaterialTheme.typography.labelSmall.copy(
              color = HdrAmber,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp,
              fontSize = 10.sp
            )
          )
        }
      }
    }

    // 2. Top HUD Controls & Scene Selector
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // HDR Mode Chip
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(TitaniumDark.copy(alpha = 0.85f))
            .border(1.dp, HdrAmber.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .clickable { showSettingsSheet = true }
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("hdr_mode_chip")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(
              imageVector = Icons.Default.AutoAwesome,
              contentDescription = null,
              tint = HdrAmber,
              modifier = Modifier.size(15.dp)
            )
            Text(
              text = uiState.hdrSettings.mode.label.uppercase(),
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 11.sp
              )
            )
          }
        }

        // Action Icons: Live HDR Sim, Histogram Toggle, Flash, Settings
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          // Live HDR Sim toggle
          IconButton(
            onClick = { viewModel.toggleLiveHdrSimulation() },
            modifier = Modifier
              .clip(CircleShape)
              .background(if (uiState.isLiveHdrSimulationActive) HdrAmber else TitaniumDark.copy(alpha = 0.8f))
              .size(36.dp)
              .testTag("toggle_hdr_sim_button")
          ) {
            Icon(
              imageVector = if (uiState.isLiveHdrSimulationActive) Icons.Default.Visibility else Icons.Default.VisibilityOff,
              contentDescription = "Toggle Live HDR Simulation",
              tint = if (uiState.isLiveHdrSimulationActive) Color.Black else Color.White,
              modifier = Modifier.size(18.dp)
            )
          }

          // Live Histogram Toggle
          IconButton(
            onClick = { viewModel.toggleHistogramVisibility() },
            modifier = Modifier
              .clip(CircleShape)
              .background(if (uiState.isHistogramVisible) HdrAmber else TitaniumDark.copy(alpha = 0.8f))
              .size(36.dp)
              .testTag("toggle_histogram_button")
          ) {
            Icon(
              imageVector = Icons.Default.BarChart,
              contentDescription = "Toggle Histogram",
              tint = if (uiState.isHistogramVisible) Color.Black else Color.White,
              modifier = Modifier.size(18.dp)
            )
          }

          // Flash
          if (uiState.selectedScene == TestScene.LIVE_CAMERA) {
            IconButton(
              onClick = { viewModel.toggleFlash() },
              modifier = Modifier
                .clip(CircleShape)
                .background(if (uiState.isFlashOn) HdrAmber else TitaniumDark.copy(alpha = 0.8f))
                .size(36.dp)
                .testTag("toggle_flash_button")
            ) {
              Icon(
                imageVector = if (uiState.isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                contentDescription = "Toggle Torch",
                tint = if (uiState.isFlashOn) Color.Black else Color.White,
                modifier = Modifier.size(18.dp)
              )
            }
          }

          // Settings
          IconButton(
            onClick = { showSettingsSheet = true },
            modifier = Modifier
              .clip(CircleShape)
              .background(TitaniumDark.copy(alpha = 0.8f))
              .size(36.dp)
              .testTag("open_settings_button")
          ) {
            Icon(
              imageVector = Icons.Default.Tune,
              contentDescription = "HDR Engine Settings",
              tint = Color.White,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Scene Selector Pills (Live Camera vs High-Contrast Backlit Test Scenes)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        SceneTabChip(
          label = "📷 Live Camera",
          isSelected = uiState.selectedScene == TestScene.LIVE_CAMERA,
          onClick = {
            if (!uiState.hasCameraPermission) {
              permissionLauncher.launch(Manifest.permission.CAMERA)
            }
            viewModel.selectScene(TestScene.LIVE_CAMERA, context)
          }
        )

        SceneTabChip(
          label = "🌅 Sunset Studio",
          isSelected = uiState.selectedScene == TestScene.SUNSET_PORTRAIT,
          onClick = { viewModel.selectScene(TestScene.SUNSET_PORTRAIT, context) }
        )

        SceneTabChip(
          label = "🪟 Window Studio",
          isSelected = uiState.selectedScene == TestScene.WINDOW_INTERIOR,
          onClick = { viewModel.selectScene(TestScene.WINDOW_INTERIOR, context) }
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Smart Backlight Alert HUD
      BacklightAlertBanner(
        backlightState = uiState.backlightState,
        currentHdrMode = uiState.hdrSettings.mode,
        currentEv = uiState.manualEvCompensation,
        onApplyRecommendedEv = { viewModel.applyRecommendedEv() },
        onEnableBacklightHdr = { viewModel.setHdrMode(HdrMode.BACKLIGHT_BOOST) }
      )

      // Real-Time Live Histogram Floating Overlay
      AnimatedVisibility(
        visible = uiState.isHistogramVisible,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically()
      ) {
        Column(modifier = Modifier.padding(top = 8.dp)) {
          HistogramView(
            histogramData = uiState.histogramData,
            selectedChannel = uiState.histogramChannel,
            onChannelSelected = { viewModel.setHistogramChannel(it) },
            showClippingAlerts = uiState.hdrSettings.showZebraAlerts
          )
        }
      }
    }

    // 3. Bottom Controls Area (Exposure Slider + Shutter Bar)
    Column(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .background(
          Brush.verticalGradient(
            listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f), Color.Black)
          )
        )
        .navigationBarsPadding()
        .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
      // Manual Exposure Compensation Dial
      ExposureControlDial(
        currentEv = uiState.manualEvCompensation,
        minEv = uiState.minEv,
        maxEv = uiState.maxEv,
        recommendedEv = uiState.backlightState.recommendedEvCompensation,
        onEvChanged = { viewModel.setEvCompensation(it) },
        onResetEv = { viewModel.resetEvCompensation() },
        onApplyRecommended = { viewModel.applyRecommendedEv() }
      )

      Spacer(modifier = Modifier.height(14.dp))

      // Bottom Shutter Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Left: Gallery Thumbnail (Recent Capture)
        Box(
          modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(TitaniumSurfaceElevated)
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .clickable {
              uiState.recentPhotos.firstOrNull()?.let { photo ->
                viewModel.capturePhoto(context)
              }
            }
            .testTag("recent_photos_thumbnail"),
          contentAlignment = Alignment.Center
        ) {
          val recent = uiState.recentPhotos.firstOrNull()
          if (recent != null) {
            Image(
              bitmap = recent.hdrBitmap.asImageBitmap(),
              contentDescription = "Recent Capture",
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize()
            )
          } else {
            Icon(
              imageVector = Icons.Default.Photo,
              contentDescription = "Gallery",
              tint = Color.White.copy(alpha = 0.6f),
              modifier = Modifier.size(24.dp)
            )
          }
        }

        // Center: Backlight HDR Shutter Button
        Box(
          modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .border(3.dp, HdrAmber, CircleShape)
            .padding(5.dp)
            .clip(CircleShape)
            .background(if (uiState.isCapturing) HdrAmberGlow else Color.White)
            .clickable(enabled = !uiState.isCapturing) {
              viewModel.capturePhoto(context)
            }
            .testTag("shutter_button"),
          contentAlignment = Alignment.Center
        ) {
          if (uiState.isCapturing) {
            CircularProgressIndicator(
              color = Color.Black,
              strokeWidth = 3.dp,
              modifier = Modifier.size(36.dp)
            )
          } else {
            Box(
              modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(HdrAmber)
                .border(2.dp, Color.Black.copy(alpha = 0.2f), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Capture Backlight HDR Photo",
                tint = Color.Black,
                modifier = Modifier.size(28.dp)
              )
            }
          }
        }

        // Right: Fine-Tune / Preset Mode Button
        Box(
          modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(TitaniumSurfaceElevated)
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .clickable { showSettingsSheet = true }
            .testTag("fine_tune_button"),
          contentAlignment = Alignment.Center
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            Icon(
              imageVector = Icons.Default.Settings,
              contentDescription = "Settings",
              tint = HdrAmber,
              modifier = Modifier.size(22.dp)
            )
            Text(
              text = "HDR",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            )
          }
        }
      }
    }

    // 4. HDR Settings Sheet
    if (showSettingsSheet) {
      HdrSettingsSheet(
        settings = uiState.hdrSettings,
        onSettingsChanged = { viewModel.updateHdrSettings(it.shadowLift, it.highlightRecovery, it.fillLightEmulation, it.showZebraAlerts); viewModel.setHdrMode(it.mode) },
        onDismiss = { showSettingsSheet = false }
      )
    }

    // 5. Captured Photo Before / After Comparison Inspector Modal
    uiState.capturedPhoto?.let { photo ->
      HdrComparisonViewer(
        photo = photo,
        onSaveToGallery = { viewModel.savePhotoToGallery(context, it) },
        onDismiss = { viewModel.dismissCapturedPhoto() }
      )
    }
  }
}

@Composable
private fun SceneTabChip(
  label: String,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(14.dp))
      .background(if (isSelected) HdrAmber else TitaniumDark.copy(alpha = 0.75f))
      .border(
        width = 1.dp,
        color = if (isSelected) HdrAmber else Color.White.copy(alpha = 0.15f),
        shape = RoundedCornerShape(14.dp)
      )
      .clickable { onClick() }
      .padding(horizontal = 8.dp, vertical = 4.dp)
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.85f),
        fontSize = 11.sp
      )
    )
  }
}
