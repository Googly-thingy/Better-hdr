package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HdrAmber
import com.example.ui.theme.HdrAmberGlow
import com.example.ui.theme.OptimalExposureGreen
import com.example.ui.theme.TitaniumDark
import com.example.ui.theme.TitaniumSurfaceElevated
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun ExposureControlDial(
  currentEv: Float,
  minEv: Float,
  maxEv: Float,
  recommendedEv: Float,
  onEvChanged: (Float) -> Unit,
  onResetEv: () -> Unit,
  onApplyRecommended: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(16.dp))
      .background(TitaniumDark.copy(alpha = 0.85f))
      .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
      .padding(horizontal = 12.dp, vertical = 8.dp)
      .testTag("exposure_control_dial")
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      // Header: EV Label + Value + Quick Actions
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = "EXPOSURE (EV)",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.SemiBold,
              color = Color.White.copy(alpha = 0.7f),
              fontSize = 11.sp
            )
          )

          if (recommendedEv > 0.3f && abs(currentEv - recommendedEv) > 0.2f) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(HdrAmber.copy(alpha = 0.25f))
                .clickable { onApplyRecommended() }
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = String.format(Locale.US, "Rec: +%.1f EV", recommendedEv),
                style = MaterialTheme.typography.labelSmall.copy(
                  color = HdrAmber,
                  fontWeight = FontWeight.Bold,
                  fontSize = 10.sp
                )
              )
            }
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Current EV Numeric Readout
          val sign = if (currentEv > 0f) "+" else ""
          val evColor = when {
            abs(currentEv) < 0.1f -> OptimalExposureGreen
            currentEv > 0f -> HdrAmber
            else -> Color(0xFF64B5F6)
          }

          Text(
            text = String.format(Locale.US, "%s%.1f EV", sign, currentEv),
            style = MaterialTheme.typography.titleMedium.copy(
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              color = evColor
            ),
            modifier = Modifier.testTag("current_ev_display")
          )

          // Reset to 0 EV button
          if (abs(currentEv) > 0.05f) {
            Box(
              modifier = Modifier
                .clip(CircleShape)
                .background(TitaniumSurfaceElevated)
                .clickable { onResetEv() }
                .padding(4.dp)
                .testTag("reset_ev_button")
            ) {
              Icon(
                imageVector = Icons.Default.RestartAlt,
                contentDescription = "Reset EV to 0.0",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Custom Ruler Ticks & Scrub Bar
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(38.dp)
      ) {
        // Ruler ticks canvas
        Canvas(modifier = Modifier.matchParentSize()) {
          val width = size.width
          val height = size.height
          val totalRange = maxEv - minEv
          if (totalRange <= 0f) return@Canvas

          val zeroX = ((0f - minEv) / totalRange) * width

          // Zero line indicator
          drawLine(
            color = OptimalExposureGreen.copy(alpha = 0.8f),
            start = Offset(zeroX, 2f),
            end = Offset(zeroX, height - 4f),
            strokeWidth = 2.5f
          )

          // Recommended EV pip
          if (recommendedEv in minEv..maxEv) {
            val recX = ((recommendedEv - minEv) / totalRange) * width
            drawCircle(
              color = HdrAmber,
              radius = 5f,
              center = Offset(recX, 6f)
            )
          }

          // Ruler tick marks every 0.5 EV
          var stepEv = (minEv * 2).toInt() / 2f
          while (stepEv <= maxEv) {
            val x = ((stepEv - minEv) / totalRange) * width
            val isMajor = abs(stepEv - stepEv.roundToInt()) < 0.01f
            val tickHeight = if (isMajor) 14f else 8f
            val tickColor = if (isMajor) Color.White.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.2f)

            drawLine(
              color = tickColor,
              start = Offset(x, height - tickHeight - 2f),
              end = Offset(x, height - 2f),
              strokeWidth = if (isMajor) 1.5f else 1f
            )
            stepEv += 0.5f
          }
        }

        // Material Slider overlaying ruler
        Slider(
          value = currentEv,
          onValueChange = { onEvChanged(it) },
          valueRange = minEv..maxEv,
          colors = SliderDefaults.colors(
            thumbColor = HdrAmber,
            activeTrackColor = HdrAmber.copy(alpha = 0.6f),
            inactiveTrackColor = Color.White.copy(alpha = 0.15f)
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .testTag("exposure_slider")
        )
      }

      // Range Labels (-3.0 EV ... 0.0 ... +3.0 EV)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = String.format(Locale.US, "%.1f EV", minEv),
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f))
        )
        Text(
          text = "0.0 EV (Standard)",
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = OptimalExposureGreen.copy(alpha = 0.7f))
        )
        Text(
          text = String.format(Locale.US, "+%.1f EV", maxEv),
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f))
        )
      }
    }
  }
}
