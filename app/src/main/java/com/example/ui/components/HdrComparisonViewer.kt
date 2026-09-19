package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.model.CapturedPhoto
import com.example.model.HistogramChannel
import com.example.ui.theme.HdrAmber
import com.example.ui.theme.OptimalExposureGreen
import com.example.ui.theme.TitaniumDark
import com.example.ui.theme.TitaniumSurface
import com.example.ui.theme.TitaniumSurfaceElevated
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HdrComparisonViewer(
  photo: CapturedPhoto,
  onSaveToGallery: (CapturedPhoto) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var splitPosition by remember { mutableFloatStateOf(0.5f) }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(TitaniumDark)
      .statusBarsPadding()
      .navigationBarsPadding()
      .testTag("hdr_comparison_viewer")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .verticalScroll(rememberScrollState())
    ) {
      // Top Bar: Title & Close
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = HdrAmber,
            modifier = Modifier.size(22.dp)
          )
          Column {
            Text(
              text = "Backlight HDR Capture",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            )
            Text(
              text = "Interactive Before / After Comparison",
              style = MaterialTheme.typography.bodySmall.copy(
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp
              )
            )
          }
        }

        IconButton(
          onClick = onDismiss,
          modifier = Modifier
            .clip(CircleShape)
            .background(TitaniumSurfaceElevated)
            .size(36.dp)
            .testTag("close_comparison_button")
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Interactive Split Slider Viewport
      BoxWithConstraints(
        modifier = Modifier
          .fillMaxWidth()
          .height(340.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(Color.Black)
          .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
          .testTag("split_slider_container")
      ) {
        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()

        val origImageBitmap = remember(photo.originalBitmap) { photo.originalBitmap.asImageBitmap() }
        val hdrImageBitmap = remember(photo.hdrBitmap) { photo.hdrBitmap.asImageBitmap() }

        // Custom Split Canvas
        Canvas(
          modifier = Modifier
            .matchParentSize()
            .pointerInput(Unit) {
              detectDragGestures { change, _ ->
                change.consume()
                val newPos = (change.position.x / containerWidth).coerceIn(0.05f, 0.95f)
                splitPosition = newPos
              }
            }
        ) {
          val splitX = splitPosition * size.width

          // 1. Draw HDR layer on the entire background
          drawImage(
            image = hdrImageBitmap,
            dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt())
          )

          // 2. Draw Original layer clipped to the left of the split line
          val clipPath = Path().apply {
            addRect(Rect(0f, 0f, splitX, size.height))
          }
          clipPath(clipPath) {
            drawImage(
              image = origImageBitmap,
              dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt())
            )
          }

          // 3. Draw Split Dividing Line
          drawLine(
            color = Color.White,
            start = Offset(splitX, 0f),
            end = Offset(splitX, size.height),
            strokeWidth = 2.5.dp.toPx()
          )

          // 4. Draw Center Thumb Pill on Divider Line
          val thumbY = size.height * 0.5f
          drawCircle(
            color = Color.White,
            radius = 16.dp.toPx(),
            center = Offset(splitX, thumbY)
          )
          drawCircle(
            color = HdrAmber,
            radius = 13.dp.toPx(),
            center = Offset(splitX, thumbY)
          )
        }

        // Overlay Pill Labels on Left & Right
        Box(
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(10.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = "◀ Standard (Silhouette)",
            style = MaterialTheme.typography.labelSmall.copy(
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 10.sp
            )
          )
        }

        Box(
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(10.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(HdrAmber.copy(alpha = 0.85f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = "Backlight HDR ▶",
            style = MaterialTheme.typography.labelSmall.copy(
              color = Color.Black,
              fontWeight = FontWeight.Bold,
              fontSize = 10.sp
            )
          )
        }

        // Drag instruction hint
        Box(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.65f))
            .padding(horizontal = 10.dp, vertical = 3.dp)
        ) {
          Text(
            text = "↔ Drag slider to inspect backlight recovery",
            style = MaterialTheme.typography.labelSmall.copy(
              color = Color.White.copy(alpha = 0.8f),
              fontSize = 9.sp
            )
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Shot Analysis Cards
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        MetricCard(
          title = "EV COMPENSATION",
          value = String.format(Locale.US, "%s%.1f EV", if (photo.evBias > 0) "+" else "", photo.evBias),
          subtitle = "Shadow Bias Applied",
          modifier = Modifier.weight(1f)
        )

        MetricCard(
          title = "SHADOW LIFT",
          value = String.format(Locale.US, "-%.0f%%", (photo.histogramBefore.shadowClippingPercent - photo.histogramAfter.shadowClippingPercent).coerceAtLeast(0f)),
          subtitle = "Silhouette Reduced",
          valueColor = OptimalExposureGreen,
          modifier = Modifier.weight(1f)
        )

        MetricCard(
          title = "DYNAMIC RANGE",
          value = String.format(Locale.US, "%.1f EV", photo.histogramAfter.dynamicRangeEv),
          subtitle = "Tone Balanced",
          valueColor = HdrAmber,
          modifier = Modifier.weight(1f)
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Histogram Comparison Preview
      Card(
        colors = CardDefaults.cardColors(containerColor = TitaniumSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Text(
            text = "Real-Time Histogram Redistribution",
            style = MaterialTheme.typography.titleSmall.copy(
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          )
          Text(
            text = "Crushed shadows in standard capture (left spike) were dynamically expanded across midtones, recovering subject features while retaining highlight sky details.",
            style = MaterialTheme.typography.bodySmall.copy(
              color = Color.White.copy(alpha = 0.7f),
              fontSize = 11.sp
            ),
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
          )

          HistogramView(
            histogramData = photo.histogramAfter,
            selectedChannel = HistogramChannel.RGB,
            onChannelSelected = {},
            showClippingAlerts = false
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Bottom Action Buttons: Save & Share
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Button(
          onClick = { onSaveToGallery(photo) },
          colors = ButtonDefaults.buttonColors(containerColor = HdrAmber, contentColor = Color.Black),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .weight(1.3f)
            .height(48.dp)
            .testTag("save_to_gallery_button")
        ) {
          Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Save HDR Photo", fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
          onClick = { sharePhoto(context, photo.hdrBitmap) },
          shape = RoundedCornerShape(12.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .testTag("share_photo_button")
        ) {
          Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Share")
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@Composable
private fun MetricCard(
  title: String,
  value: String,
  subtitle: String,
  modifier: Modifier = Modifier,
  valueColor: Color = Color.White
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = TitaniumSurfaceElevated),
    shape = RoundedCornerShape(10.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(8.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
          color = Color.White.copy(alpha = 0.5f),
          fontSize = 8.sp,
          fontWeight = FontWeight.Bold
        )
      )
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium.copy(
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold,
          color = valueColor
        ),
        modifier = Modifier.padding(vertical = 2.dp)
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.labelSmall.copy(
          color = Color.White.copy(alpha = 0.5f),
          fontSize = 9.sp
        ),
        maxLines = 1
      )
    }
  }
}

private fun sharePhoto(context: Context, bitmap: Bitmap) {
  try {
    val cachePath = File(context.cacheDir, "images")
    cachePath.mkdirs()
    val file = File(cachePath, "backlight_hdr_shared.jpg")
    val stream = FileOutputStream(file)
    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
    stream.close()

    val contentUri: Uri = FileProvider.getUriForFile(
      context,
      "${context.packageName}.fileprovider",
      file
    )

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
      type = "image/jpeg"
      putExtra(Intent.EXTRA_STREAM, contentUri)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Backlight HDR Photo"))
  } catch (e: Exception) {
    e.printStackTrace()
  }
}
