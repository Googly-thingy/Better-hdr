package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BacklightSeverity
import com.example.model.BacklightState
import com.example.model.HdrMode
import com.example.ui.theme.ClippingWarning
import com.example.ui.theme.HdrAmber
import com.example.ui.theme.HdrAmberDark
import com.example.ui.theme.OptimalExposureGreen
import com.example.ui.theme.TitaniumDark
import java.util.Locale

@Composable
fun BacklightAlertBanner(
  backlightState: BacklightState,
  currentHdrMode: HdrMode,
  currentEv: Float,
  onApplyRecommendedEv: () -> Unit,
  onEnableBacklightHdr: () -> Unit,
  modifier: Modifier = Modifier
) {
  val isBacklit = backlightState.severity != BacklightSeverity.NONE

  val bannerBg = when (backlightState.severity) {
    BacklightSeverity.EXTREME -> Brush.horizontalGradient(
      listOf(Color(0xFF4A1A05), Color(0xFF2C1004))
    )
    BacklightSeverity.HIGH -> Brush.horizontalGradient(
      listOf(Color(0xFF382305), Color(0xFF211504))
    )
    BacklightSeverity.MILD -> Brush.horizontalGradient(
      listOf(Color(0xFF241D09), Color(0xFF14130A))
    )
    BacklightSeverity.NONE -> Brush.horizontalGradient(
      listOf(TitaniumDark.copy(alpha = 0.75f), TitaniumDark.copy(alpha = 0.75f))
    )
  }

  val accentColor = when (backlightState.severity) {
    BacklightSeverity.EXTREME -> ClippingWarning
    BacklightSeverity.HIGH -> HdrAmber
    BacklightSeverity.MILD -> Color(0xFFFFD54F)
    BacklightSeverity.NONE -> OptimalExposureGreen
  }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(bannerBg)
      .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
      .padding(horizontal = 10.dp, vertical = 7.dp)
      .testTag("backlight_alert_banner")
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.weight(1f)
      ) {
        Box(
          modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(accentColor.copy(alpha = 0.2f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = if (isBacklit) Icons.Default.WbSunny else Icons.Default.CheckCircle,
            contentDescription = "Backlight Status",
            tint = accentColor,
            modifier = Modifier.size(16.dp)
          )
        }

        Column {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              text = when (backlightState.severity) {
                BacklightSeverity.EXTREME -> "EXTREME BACKLIGHT"
                BacklightSeverity.HIGH -> "HIGH CONTRAST BACKLIGHT"
                BacklightSeverity.MILD -> "MILD BACKLIGHT"
                BacklightSeverity.NONE -> "BALANCED LIGHTING"
              },
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = accentColor,
                fontSize = 11.sp
              )
            )

            Text(
              text = String.format(Locale.US, "%.1f:1 Ratio", backlightState.contrastRatio),
              style = MaterialTheme.typography.labelSmall.copy(
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 10.sp
              )
            )
          }

          Text(
            text = if (isBacklit) {
              "Bright background casting subject into shadow"
            } else {
              "Front/even illumination detected"
            },
            style = MaterialTheme.typography.bodySmall.copy(
              color = Color.White.copy(alpha = 0.8f),
              fontSize = 10.sp
            ),
            maxLines = 1
          )
        }
      }

      // Quick One-Tap Correction Button
      if (isBacklit) {
        if (currentHdrMode == HdrMode.OFF) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(HdrAmber)
              .clickable { onEnableBacklightHdr() }
              .padding(horizontal = 8.dp, vertical = 6.dp)
              .testTag("enable_hdr_quick_action")
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Engage HDR", color = Color.Black, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp))
            }
          }
        } else if (backlightState.recommendedEvCompensation > 0.4f && currentEv < 0.3f) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(HdrAmber)
              .clickable { onApplyRecommendedEv() }
              .padding(horizontal = 8.dp, vertical = 6.dp)
              .testTag("apply_ev_quick_action")
          ) {
            Text(
              text = String.format(Locale.US, "Lift +%.1f EV", backlightState.recommendedEvCompensation),
              color = Color.Black,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp)
            )
          }
        }
      }
    }
  }
}
