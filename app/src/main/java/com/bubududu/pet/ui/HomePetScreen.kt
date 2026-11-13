package com.bubududu.pet.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bubududu.pet.data.RoutineSnapshot
import com.bubududu.pet.model.PetState
import com.bubududu.pet.model.PreviewScheduleMode
import com.bubududu.pet.model.TemporaryAction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val patQuotes = listOf(
    "Hihi thích được xoa đầu quá!",
    "Bubu & Dudu ngoan lắm nè~",
    "Cảm ơn bạn đã chăm tụi mình!",
    "Hôm nay thật là dễ chịu~",
    "Tặng bạn một trái tim nè!"
)

@Composable
fun HomePetScreen(
    state: PetState,
    snapshot: RoutineSnapshot,
    tempAction: TemporaryAction?,
    nowMillis: Long,
    onFeed: () -> Unit,
    onCleanAll: () -> Unit,
    onSweepSingleMess: () -> Unit,
    onPatPet: () -> Unit,
    onNap: () -> Unit,
    onCyclePreviewMode: () -> Unit,
    onOpenWardrobe: () -> Unit,
    onOpenMiniGame: () -> Unit,
    onPinWidget: () -> Unit
) {
    val context = LocalContext.current
    val theme = state.roomTheme
    val wallBg = Color(theme.wallColorHex)
    val cardBg = Color(theme.cardColorHex)
    val floorBg = Color(theme.floorColorHex)
    val border = Color(theme.borderColorHex)
    val textPrimary = Color(theme.textPrimaryHex)
    val textSecondary = Color(theme.textSecondaryHex)
    val accent = Color(theme.accentHex)

    var patCount by remember { mutableIntStateOf(0) }
    val bounceTrigger by animateFloatAsState(
        targetValue = if (patCount % 2 == 0) 1f else 1.06f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "patBounce"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "petBreath")
    val breathScaleY by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.032f,
        animationSpec = infiniteRepeatable(
            animation = tween(1700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathY"
    )
    val floatHeartOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heartFloat"
    )

    val timeString = remember(nowMillis) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nowMillis))
    }

    val dressedBitmap = remember(snapshot.pose, state.equippedHat, state.equippedGlasses) {
        Pet2DRenderer.getDressedBitmap(
            context,
            snapshot.pose,
            state.equippedHat,
            state.equippedGlasses
        ).asImageBitmap()
    }

    val bubbleMessage = remember(tempAction, patCount, snapshot, state.hunger, state.messCount) {
        when {
            tempAction == TemporaryAction.PAT && patCount > 0 ->
                patQuotes[patCount % patQuotes.size]
            tempAction != null -> tempAction.bubbleText
            state.hunger < 30 -> "Đói bụng quá rồi bạn ơi~ Cho tụi mình ăn đi!"
            state.messCount >= 3 -> "Phòng hơi bừa rồi, bấm vào bụi để quét nhé!"
            else -> snapshot.routine.statusLine
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(wallBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Header: Title + Level XP + Coins + Pin Widget + Wardrobe
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Bubu & Dudu • Lv.${state.level}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(74.dp)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(floorBg)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(state.levelProgress.coerceIn(0.08f, 1f))
                                .clip(CircleShape)
                                .background(accent)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (state.previewMode == PreviewScheduleMode.AUTO) {
                            "$timeString • ${snapshot.routine.shortTitle}"
                        } else {
                            "🕒 ${state.previewMode.displayName}"
                        },
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (state.previewMode == PreviewScheduleMode.AUTO) textSecondary else accent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onCyclePreviewMode)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(cardBg)
                        .border(1.dp, border, RoundedCornerShape(14.dp))
                        .clickable(onClick = onOpenMiniGame)
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "🪙 ${state.coins}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(cardBg)
                        .border(1.dp, border, RoundedCornerShape(14.dp))
                        .clickable(onClick = onPinWidget)
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Text(text = "📌", fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4 Stat Bars
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            StatMeterCard(
                label = "🍚 No",
                value = state.hunger,
                barColor = Color(0xFFE88A6B),
                cardBg = cardBg,
                border = border,
                textPrimary = textPrimary,
                modifier = Modifier.weight(1f)
            )
            StatMeterCard(
                label = "✨ Sạch",
                value = state.hygiene,
                barColor = Color(0xFF6DB5D8),
                cardBg = cardBg,
                border = border,
                textPrimary = textPrimary,
                modifier = Modifier.weight(1f)
            )
            StatMeterCard(
                label = "💖 Vui",
                value = state.happiness,
                barColor = Color(0xFFE5829B),
                cardBg = cardBg,
                border = border,
                textPrimary = textPrimary,
                modifier = Modifier.weight(1f)
            )
            StatMeterCard(
                label = "⚡ Khỏe",
                value = state.energy,
                barColor = Color(0xFF75BC86),
                cardBg = cardBg,
                border = border,
                textPrimary = textPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Main Cozy 2D Tamagotchi Pocket Room Stage
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(28.dp))
                .background(cardBg)
                .border(1.5.dp, border, RoundedCornerShape(28.dp))
        ) {
            // 2D Cozy Window + Floor Backdrop
            val lineColor = Color(0xFF442B23)
            val effectiveHour = snapshot.effectiveHour
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Room warm floor
                drawRoundRect(
                    color = floorBg,
                    topLeft = Offset(0f, h * 0.66f),
                    size = Size(w, h * 0.34f),
                    cornerRadius = CornerRadius(24f, 24f)
                )
                drawLine(
                    color = lineColor.copy(alpha = 0.28f),
                    start = Offset(0f, h * 0.66f),
                    end = Offset(w, h * 0.66f),
                    strokeWidth = 3f
                )

                // Cute 2D rounded bedroom window top-left
                val winLeft = w * 0.08f
                val winTop = h * 0.07f
                val winW = w * 0.33f
                val winH = h * 0.25f
                val skyColor = when {
                    effectiveHour >= 20 || effectiveHour < 6 -> Color(0xFF333B5C)
                    effectiveHour in 16..19 -> Color(0xFFFFD8B8)
                    else -> Color(0xFFC9ECFF)
                }
                drawRoundRect(
                    color = skyColor,
                    topLeft = Offset(winLeft, winTop),
                    size = Size(winW, winH),
                    cornerRadius = CornerRadius(28f, 28f)
                )
                if (effectiveHour >= 20 || effectiveHour < 6) {
                    // Crescent moon & stars
                    drawCircle(
                        color = Color(0xFFFFEA9F),
                        radius = winH * 0.17f,
                        center = Offset(winLeft + winW * 0.72f, winTop + winH * 0.36f)
                    )
                    drawCircle(
                        color = skyColor,
                        radius = winH * 0.14f,
                        center = Offset(winLeft + winW * 0.78f, winTop + winH * 0.32f)
                    )
                    drawCircle(Color(0xFFFFF5D0), 4f, Offset(winLeft + winW * 0.26f, winTop + winH * 0.32f))
                    drawCircle(Color(0xFFFFF5D0), 3f, Offset(winLeft + winW * 0.45f, winTop + winH * 0.62f))
                } else {
                    // Sun & fluffy cloud
                    drawCircle(
                        color = if (effectiveHour in 16..19) Color(0xFFFFA566) else Color(0xFFFFDF73),
                        radius = winH * 0.18f,
                        center = Offset(winLeft + winW * 0.28f, winTop + winH * 0.36f)
                    )
                    drawCircle(Color.White, winH * 0.14f, Offset(winLeft + winW * 0.66f, winTop + winH * 0.60f))
                    drawCircle(Color.White, winH * 0.17f, Offset(winLeft + winW * 0.76f, winTop + winH * 0.58f))
                }
                // Window mullions + frame
                drawLine(
                    color = lineColor.copy(alpha = 0.75f),
                    start = Offset(winLeft + winW * 0.5f, winTop),
                    end = Offset(winLeft + winW * 0.5f, winTop + winH),
                    strokeWidth = 3.5f
                )
                drawLine(
                    color = lineColor.copy(alpha = 0.75f),
                    start = Offset(winLeft, winTop + winH * 0.55f),
                    end = Offset(winLeft + winW, winTop + winH * 0.55f),
                    strokeWidth = 3.5f
                )
                drawRoundRect(
                    color = lineColor,
                    topLeft = Offset(winLeft, winTop),
                    size = Size(winW, winH),
                    cornerRadius = CornerRadius(28f, 28f),
                    style = Stroke(width = 4.5f, cap = StrokeCap.Round)
                )
            }

            // Sleep / Rest quick moon button in upper-right room corner
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(wallBg.copy(alpha = 0.9f))
                    .border(1.dp, border, RoundedCornerShape(14.dp))
                    .clickable(onClick = onNap)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "😴 Nghỉ ngơi",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary
                )
            }

            // Speech bubble + Animated 2D Dressed Bubu & Dudu in center
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 34.dp, bottom = 14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(wallBg)
                        .border(1.dp, border, RoundedCornerShape(18.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = bubbleMessage,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = textPrimary,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Box(contentAlignment = Alignment.Center) {
                    Image(
                        bitmap = dressedBitmap,
                        contentDescription = "Bubu & Dudu 2D",
                        modifier = Modifier
                            .size(width = 250.dp, height = 205.dp)
                            .graphicsLayer {
                                scaleX = bounceTrigger
                                scaleY = breathScaleY * bounceTrigger
                            }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                patCount++
                                onPatPet()
                            }
                    )

                    androidx.compose.animation.AnimatedVisibility(
                        visible = tempAction == TemporaryAction.PAT,
                        enter = fadeIn(tween(150)),
                        exit = fadeOut(tween(450)),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = floatHeartOffset.dp)
                    ) {
                        Text(
                            text = "💕 ✨ 💕",
                            fontSize = 22.sp
                        )
                    }
                }

                Text(
                    text = "Chạm vào Bubu & Dudu để xoa đầu",
                    fontSize = 11.sp,
                    color = textSecondary
                )
            }

            // Tappable 2D mess dust spots on floor when room needs cleaning
            if (state.messCount > 0) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                ) {
                    repeat(state.messCount) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(cardBg.copy(alpha = 0.9f))
                                .border(1.dp, border, RoundedCornerShape(14.dp))
                                .clickable(onClick = onSweepSingleMess)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "🍪 Bụi (+5 xu)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bottom Tamagotchi Control Deck (4 chunky cozy action pills)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            DeckActionButton(
                emoji = "🍚",
                label = "Cho ăn",
                accent = accent,
                cardBg = cardBg,
                border = border,
                textPrimary = textPrimary,
                highlight = state.hunger < 35,
                modifier = Modifier.weight(1f),
                onClick = onFeed
            )
            DeckActionButton(
                emoji = "🪥",
                label = if (state.messCount > 0) "Dọn (${state.messCount})" else "Vệ sinh",
                accent = accent,
                cardBg = cardBg,
                border = border,
                textPrimary = textPrimary,
                highlight = state.messCount > 0 || state.hygiene < 35,
                modifier = Modifier.weight(1f),
                onClick = onCleanAll
            )
            DeckActionButton(
                emoji = "👒",
                label = "Mũ & Kính",
                accent = accent,
                cardBg = cardBg,
                border = border,
                textPrimary = textPrimary,
                highlight = false,
                modifier = Modifier.weight(1f),
                onClick = onOpenWardrobe
            )
            DeckActionButton(
                emoji = "🎮",
                label = "Kiếm Xu",
                accent = accent,
                cardBg = cardBg,
                border = border,
                textPrimary = textPrimary,
                highlight = false,
                modifier = Modifier.weight(1f),
                onClick = onOpenMiniGame
            )
        }
    }
}

@Composable
private fun StatMeterCard(
    label: String,
    value: Int,
    barColor: Color,
    cardBg: Color,
    border: Color,
    textPrimary: Color,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(
        targetValue = (value.coerceIn(0, 100) / 100f),
        animationSpec = tween(400),
        label = "statFill"
    )
    val low = value < 30
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(1.dp, if (low) Color(0xFFE05252) else border, RoundedCornerShape(14.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = label, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
            Text(
                text = "$value%",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (low) Color(0xFFD64545) else textPrimary
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(border.copy(alpha = 0.55f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceAtLeast(0.05f))
                    .clip(CircleShape)
                    .background(if (low) Color(0xFFE05252) else barColor)
            )
        }
    }
}

@Composable
private fun DeckActionButton(
    emoji: String,
    label: String,
    accent: Color,
    cardBg: Color,
    border: Color,
    textPrimary: Color,
    highlight: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (highlight) accent.copy(alpha = 0.16f) else cardBg)
            .border(
                width = if (highlight) 1.5.dp else 1.dp,
                color = if (highlight) accent else border,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp)
    ) {
        Text(text = emoji, fontSize = 22.sp)
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (highlight) accent else textPrimary,
            maxLines = 1
        )
    }
}
