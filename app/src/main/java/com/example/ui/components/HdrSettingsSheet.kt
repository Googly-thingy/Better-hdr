package com.example.ui.components

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.HdrMode
import com.example.model.HdrSettings
import com.example.ui.theme.HdrAmber
import com.example.ui.theme.HighlightCyan
import com.example.ui.theme.TitaniumDark
import com.example.ui.theme.TitaniumSurface
import com.example.ui.theme.TitaniumSurfaceElevated
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HdrSettingsSheet(
  settings: HdrSettings,
  onSettingsChanged: (HdrSettings) -> Unit,
  onDismiss: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = TitaniumSurface,
    dragHandle = null,
    modifier = Modifier.testTag("hdr_settings_sheet")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp)
        .navigationBarsPadding()
    ) {
      // Header
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
            imageVector = Icons.Default.Tune,
            contentDescription = null,
            tint = HdrAmber,
            modifier = Modifier.size(22.dp)
          )
          Text(
            text = "Backlight HDR Engine",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          )
        }

        IconButton(
          onClick = onDismiss,
          modifier = Modifier.size(32.dp)
        ) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Mode Selection Chips
      Text(
        text = "HDR CAPTURE MODE",
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.Bold,
          color = Color.White.copy(alpha = 0.5f),
          fontSize = 10.sp
        )
      )

      Spacer(modifier = Modifier.height(8.dp))

      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        HdrMode.entries.forEach { mode ->
          val isSelected = settings.mode == mode
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(if (isSelected) HdrAmber.copy(alpha = 0.15f) else TitaniumSurfaceElevated)
              .border(
                1.dp,
                if (isSelected) HdrAmber else Color.White.copy(alpha = 0.08f),
                RoundedCornerShape(10.dp)
              )
              .clickable { onSettingsChanged(settings.copy(mode = mode)) }
              .padding(horizontal = 12.dp, vertical = 10.dp)
              .testTag("hdr_mode_${mode.name.lowercase()}")
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = mode.label,
                  style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) HdrAmber else Color.White
                  )
                )
                Text(
                  text = mode.description,
                  style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                  )
                )
              }

              if (isSelected) {
                Icon(
                  imageVector = Icons.Default.AutoAwesome,
                  contentDescription = null,
                  tint = HdrAmber,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      // Sliders: Shadow Lift & Highlight Recovery
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = "ADAPTIVE SHADOW LIFT",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 10.sp
          )
        )
        Text(
          text = String.format(Locale.US, "%.0f%%", settings.shadowLift * 100f),
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            color = HdrAmber
          )
        )
      }

      Slider(
        value = settings.shadowLift,
        onValueChange = { onSettingsChanged(settings.copy(shadowLift = it)) },
        valueRange = 0.1f..1.0f,
        colors = SliderDefaults.colors(thumbColor = HdrAmber, activeTrackColor = HdrAmber),
        modifier = Modifier.testTag("shadow_lift_slider")
      )

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = "SKY / HIGHLIGHT RECOVERY",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 10.sp
          )
        )
        Text(
          text = String.format(Locale.US, "%.0f%%", settings.highlightRecovery * 100f),
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            color = HighlightCyan
          )
        )
      }

      Slider(
        value = settings.highlightRecovery,
        onValueChange = { onSettingsChanged(settings.copy(highlightRecovery = it)) },
        valueRange = 0.1f..1.0f,
        colors = SliderDefaults.colors(thumbColor = HighlightCyan, activeTrackColor = HighlightCyan),
        modifier = Modifier.testTag("highlight_recovery_slider")
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Switches: Fill-light & Zebra alerts
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Fill-Light Emulation",
            style = MaterialTheme.typography.bodyMedium.copy(
              fontWeight = FontWeight.SemiBold,
              color = Color.White
            )
          )
          Text(
            text = "Simulates soft bounced fill light on dark facial shadows",
            style = MaterialTheme.typography.bodySmall.copy(
              color = Color.White.copy(alpha = 0.6f),
              fontSize = 11.sp
            )
          )
        }

        Switch(
          checked = settings.fillLightEmulation,
          onCheckedChange = { onSettingsChanged(settings.copy(fillLightEmulation = it)) },
          colors = SwitchDefaults.colors(checkedThumbColor = HdrAmber, checkedTrackColor = HdrAmber.copy(alpha = 0.4f)),
          modifier = Modifier.testTag("fill_light_switch")
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Clipping Zebra Alerts",
            style = MaterialTheme.typography.bodyMedium.copy(
              fontWeight = FontWeight.SemiBold,
              color = Color.White
            )
          )
          Text(
            text = "Real-time indicators for crushed shadows and blown highlights",
            style = MaterialTheme.typography.bodySmall.copy(
              color = Color.White.copy(alpha = 0.6f),
              fontSize = 11.sp
            )
          )
        }

        Switch(
          checked = settings.showZebraAlerts,
          onCheckedChange = { onSettingsChanged(settings.copy(showZebraAlerts = it)) },
          colors = SwitchDefaults.colors(checkedThumbColor = HdrAmber, checkedTrackColor = HdrAmber.copy(alpha = 0.4f)),
          modifier = Modifier.testTag("zebra_alerts_switch")
        )
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}
