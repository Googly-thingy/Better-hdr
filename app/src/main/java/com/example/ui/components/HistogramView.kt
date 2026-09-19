package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.HistogramChannel
import com.example.model.HistogramData
import com.example.ui.theme.ClippingWarning
import com.example.ui.theme.HdrAmber
import com.example.ui.theme.HighlightCyan
import com.example.ui.theme.ShadowIndigo
import com.example.ui.theme.TitaniumDark
import com.example.ui.theme.TitaniumSurfaceElevated
import java.util.Locale

@Composable
fun HistogramView(
  histogramData: HistogramData,
  selectedChannel: HistogramChannel,
  onChannelSelected: (HistogramChannel) -> Unit,
  modifier: Modifier = Modifier,
  showClippingAlerts: Boolean = true
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .background(TitaniumDark.copy(alpha = 0.85f))
      .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
      .padding(8.dp)
      .testTag("histogram_container")
  ) {
    Column {
      // Header: Channel Chips + Dynamic Range Info
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          HistogramChannelChip(
            label = "RGB",
            isSelected = selectedChannel == HistogramChannel.RGB,
            color = Color.White
          ) { onChannelSelected(HistogramChannel.RGB) }

          HistogramChannelChip(
            label = "Luma",
            isSelected = selectedChannel == HistogramChannel.LUMINANCE,
            color = HdrAmber
          ) { onChannelSelected(HistogramChannel.LUMINANCE) }

          HistogramChannelChip(
            label = "R",
            isSelected = selectedChannel == HistogramChannel.RED,
            color = Color(0xFFFF453A)
          ) { onChannelSelected(HistogramChannel.RED) }

          HistogramChannelChip(
            label = "G",
            isSelected = selectedChannel == HistogramChannel.GREEN,
            color = Color(0xFF30D158)
          ) { onChannelSelected(HistogramChannel.GREEN) }

          HistogramChannelChip(
            label = "B",
            isSelected = selectedChannel == HistogramChannel.BLUE,
            color = Color(0xFF0A84FF)
          ) { onChannelSelected(HistogramChannel.BLUE) }
        }

        Text(
          text = String.format(Locale.US, "%.1f EV DR", histogramData.dynamicRangeEv),
          style = MaterialTheme.typography.labelSmall.copy(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = HdrAmber,
            fontSize = 10.sp
          )
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Main Histogram Graph Canvas
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(72.dp)
          .clip(RoundedCornerShape(6.dp))
          .background(Color(0xFF0A0C0E))
      ) {
        Canvas(modifier = Modifier.matchParentSize()) {
          val canvasWidth = size.width
          val canvasHeight = size.height

          // Draw grid division lines (Zone system / EV stops)
          val evStep = canvasWidth / 5f
          for (i in 1..4) {
            drawLine(
              color = Color.White.copy(alpha = 0.08f),
              start = Offset(evStep * i, 0f),
              end = Offset(evStep * i, canvasHeight),
              strokeWidth = 1f
            )
          }

          // Draw histogram channels
          when (selectedChannel) {
            HistogramChannel.LUMINANCE -> {
              drawChannelCurve(histogramData.lumaBins, canvasWidth, canvasHeight, HdrAmber, fillAlpha = 0.35f)
            }
            HistogramChannel.RGB -> {
              drawChannelCurve(histogramData.redBins, canvasWidth, canvasHeight, Color(0xFFFF453A), fillAlpha = 0.20f)
              drawChannelCurve(histogramData.greenBins, canvasWidth, canvasHeight, Color(0xFF30D158), fillAlpha = 0.20f)
              drawChannelCurve(histogramData.blueBins, canvasWidth, canvasHeight, Color(0xFF0A84FF), fillAlpha = 0.20f)
              drawChannelCurve(histogramData.lumaBins, canvasWidth, canvasHeight, Color.White, fillAlpha = 0.15f)
            }
            HistogramChannel.RED -> {
              drawChannelCurve(histogramData.redBins, canvasWidth, canvasHeight, Color(0xFFFF453A), fillAlpha = 0.40f)
            }
            HistogramChannel.GREEN -> {
              drawChannelCurve(histogramData.greenBins, canvasWidth, canvasHeight, Color(0xFF30D158), fillAlpha = 0.40f)
            }
            HistogramChannel.BLUE -> {
              drawChannelCurve(histogramData.blueBins, canvasWidth, canvasHeight, Color(0xFF0A84FF), fillAlpha = 0.40f)
            }
          }

          // Highlight shadow crush indicator (left 5%)
          if (histogramData.shadowClippingPercent > 3.0f) {
            drawRect(
              color = ShadowIndigo.copy(alpha = 0.35f),
              topLeft = Offset(0f, 0f),
              size = Size(canvasWidth * 0.06f, canvasHeight)
            )
          }

          // Highlight blown highlights indicator (right 5%)
          if (histogramData.highlightClippingPercent > 3.0f) {
            drawRect(
              color = ClippingWarning.copy(alpha = 0.40f),
              topLeft = Offset(canvasWidth * 0.94f, 0f),
              size = Size(canvasWidth * 0.06f, canvasHeight)
            )
          }
        }

        // Left alert: Shadow crush warning
        if (showClippingAlerts && histogramData.shadowClippingPercent > 4.0f) {
          Box(
            modifier = Modifier
              .align(Alignment.BottomStart)
              .padding(4.dp)
              .clip(RoundedCornerShape(4.dp))
              .background(ShadowIndigo.copy(alpha = 0.9f))
              .padding(horizontal = 4.dp, vertical = 2.dp)
          ) {
            Text(
              text = String.format(Locale.US, "▲ %.0f%% CRUSH", histogramData.shadowClippingPercent),
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            )
          }
        }

        // Right alert: Highlight blown warning
        if (showClippingAlerts && histogramData.highlightClippingPercent > 4.0f) {
          Box(
            modifier = Modifier
              .align(Alignment.BottomEnd)
              .padding(4.dp)
              .clip(RoundedCornerShape(4.dp))
              .background(ClippingWarning.copy(alpha = 0.9f))
              .padding(horizontal = 4.dp, vertical = 2.dp)
          ) {
            Text(
              text = String.format(Locale.US, "CLIPPED %.0f%% ▲", histogramData.highlightClippingPercent),
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            )
          }
        }
      }

      // Bottom Legend: Deep Shadows -> Midtones -> Specular Highlights
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 4.dp, start = 2.dp, end = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text("Shadows", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f)))
        Text("Midtones", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f)))
        Text("Highlights", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f)))
      }
    }
  }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawChannelCurve(
  bins: FloatArray,
  width: Float,
  height: Float,
  color: Color,
  fillAlpha: Float = 0.25f
) {
  if (bins.isEmpty()) return
  val path = Path()
  val fillPath = Path()

  val binCount = bins.size
  val stepX = width / (binCount - 1).toFloat()

  fillPath.moveTo(0f, height)
  for (i in 0 until binCount) {
    val x = i * stepX
    val normY = (1f - bins[i].coerceIn(0f, 1f)) * (height - 2f)
    if (i == 0) {
      path.moveTo(x, normY)
      fillPath.lineTo(x, normY)
    } else {
      path.lineTo(x, normY)
      fillPath.lineTo(x, normY)
    }
  }
  fillPath.lineTo(width, height)
  fillPath.close()

  drawPath(
    path = fillPath,
    brush = Brush.verticalGradient(
      colors = listOf(color.copy(alpha = fillAlpha), color.copy(alpha = 0.05f)),
      startY = 0f,
      endY = height
    ),
    style = Fill
  )

  drawPath(
    path = path,
    color = color,
    style = Stroke(width = 1.5f)
  )
}

@Composable
private fun HistogramChannelChip(
  label: String,
  isSelected: Boolean,
  color: Color,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(4.dp))
      .background(if (isSelected) color.copy(alpha = 0.25f) else Color.Transparent)
      .border(
        width = 1.dp,
        color = if (isSelected) color else Color.White.copy(alpha = 0.15f),
        shape = RoundedCornerShape(4.dp)
      )
      .clickable { onClick() }
      .padding(horizontal = 6.dp, vertical = 2.dp)
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(
        fontSize = 9.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        color = if (isSelected) color else Color.White.copy(alpha = 0.7f)
      )
    )
  }
}
