package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.ui.theme.HdrAmber
import kotlin.math.roundToInt

@Composable
fun FocusRing(
  tapPoint: Offset,
  modifier: Modifier = Modifier
) {
  val scale = remember { Animatable(1.4f) }
  val alpha = remember { Animatable(1.0f) }

  LaunchedEffect(tapPoint) {
    scale.snapTo(1.4f)
    alpha.snapTo(1.0f)
    scale.animateTo(
      targetValue = 1.0f,
      animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
    )
    kotlinx.coroutines.delay(1800)
    alpha.animateTo(
      targetValue = 0.0f,
      animationSpec = tween(durationMillis = 350)
    )
  }

  val sizeDp = 64.dp
  val density = LocalDensity.current
  val sizePx = with(density) { sizeDp.toPx() }

  Box(
    modifier = modifier.offset {
      IntOffset(
        (tapPoint.x - sizePx / 2f).roundToInt(),
        (tapPoint.y - sizePx / 2f).roundToInt()
      )
    }
  ) {
    Canvas(modifier = Modifier.size(sizeDp)) {
      val w = size.width
      val h = size.height
      val currentAlpha = alpha.value
      val currentScale = scale.value

      // Scaled reticle box
      val boxSize = w * 0.75f * currentScale
      val left = (w - boxSize) / 2f
      val top = (h - boxSize) / 2f

      // Draw focus square with corner brackets
      drawRect(
        color = HdrAmber.copy(alpha = currentAlpha * 0.9f),
        topLeft = Offset(left, top),
        size = Size(boxSize, boxSize),
        style = Stroke(width = 2.dp.toPx())
      )

      // Center dot
      drawCircle(
        color = HdrAmber.copy(alpha = currentAlpha),
        radius = 3f,
        center = Offset(w / 2f, h / 2f)
      )

      // Metering tick marks at top, bottom, left, right
      val tickLength = 5.dp.toPx()
      drawLine(
        color = HdrAmber.copy(alpha = currentAlpha),
        start = Offset(w / 2f, top - tickLength),
        end = Offset(w / 2f, top),
        strokeWidth = 2.dp.toPx()
      )
      drawLine(
        color = HdrAmber.copy(alpha = currentAlpha),
        start = Offset(w / 2f, top + boxSize),
        end = Offset(w / 2f, top + boxSize + tickLength),
        strokeWidth = 2.dp.toPx()
      )
      drawLine(
        color = HdrAmber.copy(alpha = currentAlpha),
        start = Offset(left - tickLength, h / 2f),
        end = Offset(left, h / 2f),
        strokeWidth = 2.dp.toPx()
      )
      drawLine(
        color = HdrAmber.copy(alpha = currentAlpha),
        start = Offset(left + boxSize, h / 2f),
        end = Offset(left + boxSize + tickLength, h / 2f),
        strokeWidth = 2.dp.toPx()
      )
    }
  }
}
