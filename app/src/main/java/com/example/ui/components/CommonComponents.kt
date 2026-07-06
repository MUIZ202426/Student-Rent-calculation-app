package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.random.Random

@Composable
fun StudySplitCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    borderWidth: Dp = 1.dp,
    borderColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    content: @Composable ColumnScope.() -> Unit
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.5f),
                spotColor = MintPrimary.copy(alpha = 0.15f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        SlateSurface,
                        SlateSurface.copy(alpha = 0.9f)
                    )
                )
            )
            .border(borderWidth, borderColor, RoundedCornerShape(20.dp))
            .then(clickableModifier)
            .padding(18.dp),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudySplitTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextSecondary) },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MintPrimary,
            unfocusedBorderColor = SlateSurfaceAlt,
            cursorColor = MintPrimary,
            focusedContainerColor = ObsidianBg.copy(alpha = 0.6f),
            unfocusedContainerColor = ObsidianBg.copy(alpha = 0.6f),
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun EmojiAvatar(
    emoji: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = SlateSurfaceAlt,
    size: Dp = 48.dp,
    onClick: (() -> Unit)? = null
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(1.dp, MintPrimary.copy(alpha = 0.3f), CircleShape)
            .then(clickableModifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji.ifEmpty { "👤" },
            fontSize = (size.value * 0.5f).sp
        )
    }
}

@Composable
fun CategoryIcon(
    category: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val (emoji, color) = when (category.lowercase()) {
        "food" -> "🍔" to Color(0xFFFF9800)
        "utility" -> "⚡" to Color(0xFFFFEB3B)
        "internet" -> "🌐" to Color(0xFF2196F3)
        "cleaning" -> "🧹" to Color(0xFF00BCD4)
        "rent" -> "🏠" to Color(0xFFE91E63)
        "groceries" -> "🛒" to Color(0xFF4CAF50)
        else -> "📦" to Color(0xFF9E9E9E)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji, fontSize = (size.value * 0.55f).sp)
    }
}

@Composable
fun CustomPieChart(
    slices: List<Pair<String, Double>>,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 40f
) {
    val total = slices.sumOf { it.second }.toFloat()
    if (total == 0f) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(text = "No spending data", color = TextMuted, fontSize = 14.sp)
        }
        return
    }

    val colors = listOf(
        Color(0xFFFF9800), // Food
        Color(0xFFFFEB3B), // Utility
        Color(0xFF2196F3), // Internet
        Color(0xFF00BCD4), // Cleaning
        Color(0xFFE91E63), // Rent
        Color(0xFF4CAF50), // Groceries
        Color(0xFF9C27B0), // Others
        Color(0xFFFF5722)
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Canvas(modifier = Modifier.size(120.dp)) {
            var startAngle = -90f
            slices.forEachIndexed { index, slice ->
                val sweepAngle = (slice.second.toFloat() / total) * 360f
                val color = colors[index % colors.size]

                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth),
                    size = Size(size.width, size.height)
                )
                startAngle += sweepAngle
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            slices.take(5).forEachIndexed { index, slice ->
                val color = colors[index % colors.size]
                val percentage = ((slice.second / total) * 100).toInt()

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Text(
                        text = "${slice.first}: $percentage%",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun ConfettiCelebration(
    isActive: Boolean,
    onFinished: () -> Unit
) {
    if (!isActive) return

    val infiniteTransition = rememberInfiniteTransition(label = "Confetti")
    val duration = 2500

    val animTime by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(duration, easing = LinearEasing),
        finishedListener = { onFinished() },
        label = "Time"
    )

    val confettiCount = 50
    val particles = remember {
        List(confettiCount) {
            ConfettiParticle(
                xStart = Random.nextFloat(),
                yStart = -0.1f,
                speed = Random.nextFloat() * 1.5f + 0.5f,
                angle = Random.nextFloat() * 30f - 15f,
                color = listOf(MintPrimary, NeonBlue, SunsetOrange, GreenSuccess, AmberWarning).random(),
                size = Random.nextFloat() * 15f + 10f
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            particles.forEach { p ->
                val x = p.xStart * size.width + p.angle * animTime * 10f
                val y = p.yStart * size.height + (animTime * p.speed * size.height)

                if (y < size.height) {
                    drawRect(
                        color = p.color.copy(alpha = 1f - animTime),
                        topLeft = Offset(x, y),
                        size = Size(p.size, p.size)
                    )
                }
            }
        }
    }
}

private data class ConfettiParticle(
    val xStart: Float,
    val yStart: Float,
    val speed: Float,
    val angle: Float,
    val color: Color,
    val size: Float
)
