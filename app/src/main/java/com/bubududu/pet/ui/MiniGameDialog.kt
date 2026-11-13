package com.bubududu.pet.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bubududu.pet.model.PetGlasses
import com.bubududu.pet.model.PetHat
import com.bubududu.pet.model.PetMoodPose
import com.bubududu.pet.model.RoomTheme
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

private enum class FallingKind(
    val icon: String,
    val label: String,
    val points: Int
) {
    ONIGIRI("🍙", "+5", 5),
    STRAWBERRY("🍓", "+8", 8),
    COIN("🪙", "+15", 15),
    SOCK("🧦", "-10", -10)
}

private data class FallingDrop(
    val id: Long,
    val xFrac: Float,
    var yFrac: Float,
    val speedPerSec: Float,
    val kind: FallingKind
)

private enum class RpsChoice(val icon: String, val label: String) {
    ROCK("✊", "Búa"),
    SCISSORS("✌️", "Kéo"),
    PAPER("✋", "Bao")
}

@Composable
fun MiniGameDialog(
    roomTheme: RoomTheme,
    equippedHat: PetHat,
    equippedGlasses: PetGlasses,
    onClaimCoins: (coins: Int, xp: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val bgColor = Color(roomTheme.wallColorHex)
    val cardColor = Color(roomTheme.cardColorHex)
    val textPrimary = Color(roomTheme.textPrimaryHex)
    val textSecondary = Color(roomTheme.textSecondaryHex)
    val accent = Color(roomTheme.accentHex)
    val border = Color(roomTheme.borderColorHex)

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Catch game, 1 = RPS

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = bgColor,
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 20.dp)
                .border(1.5.dp, border, RoundedCornerShape(26.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🎮 Mini-Game Kiếm Xu",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(cardColor)
                            .border(1.dp, border, RoundedCornerShape(10.dp))
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Đóng", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GameTabPill(
                        title = "🍙 Hứng Bánh & Xu",
                        selected = selectedTab == 0,
                        accent = accent,
                        cardColor = cardColor,
                        border = border,
                        textPrimary = textPrimary,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = 0 }
                    )
                    GameTabPill(
                        title = "✊ Oẳn Tù Tì",
                        selected = selectedTab == 1,
                        accent = accent,
                        cardColor = cardColor,
                        border = border,
                        textPrimary = textPrimary,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = 1 }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedTab == 0) {
                    CatchSnacksGame(
                        equippedHat = equippedHat,
                        equippedGlasses = equippedGlasses,
                        cardColor = cardColor,
                        border = border,
                        accent = accent,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        onFinishClaim = { coins ->
                            onClaimCoins(coins, 18)
                            onDismiss()
                        }
                    )
                } else {
                    RockPaperScissorsGame(
                        equippedHat = equippedHat,
                        equippedGlasses = equippedGlasses,
                        cardColor = cardColor,
                        border = border,
                        accent = accent,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        onWinRound = { coins -> onClaimCoins(coins, 8) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CatchSnacksGame(
    equippedHat: PetHat,
    equippedGlasses: PetGlasses,
    cardColor: Color,
    border: Color,
    accent: Color,
    textPrimary: Color,
    textSecondary: Color,
    onFinishClaim: (Int) -> Unit
) {
    val context = LocalContext.current
    val playerBitmap = remember(equippedHat, equippedGlasses) {
        Pet2DRenderer.getDressedBitmap(
            context,
            PetMoodPose.HAPPY_WAVE,
            equippedHat,
            equippedGlasses
        ).asImageBitmap()
    }

    var gameRoundId by remember { mutableIntStateOf(0) }
    var timeLeftMs by remember(gameRoundId) { mutableIntStateOf(25_000) }
    var score by remember(gameRoundId) { mutableIntStateOf(0) }
    var playerXFrac by remember { mutableFloatStateOf(0.5f) }
    var feedbackText by remember(gameRoundId) { mutableStateOf("Kéo trái/phải để hứng đồ ăn & xu!") }
    val drops = remember(gameRoundId) { mutableStateListOf<FallingDrop>() }

    val gameEnded = timeLeftMs <= 0
    val earnedCoins = if (score <= 0) 5 else (score / 4).coerceAtLeast(8) + if (score >= 120) 10 else 0

    // Main 60fps loop
    LaunchedEffect(gameRoundId) {
        var nextId = 1L
        var spawnAccumMs = 0L
        var lastTick = System.currentTimeMillis()
        while (timeLeftMs > 0) {
            delay(16L)
            val now = System.currentTimeMillis()
            val dtMs = (now - lastTick).coerceIn(1L, 60L)
            lastTick = now
            val dtSec = dtMs / 1000f

            timeLeftMs = (timeLeftMs - dtMs.toInt()).coerceAtLeast(0)
            spawnAccumMs += dtMs
            if (spawnAccumMs >= 480L) {
                spawnAccumMs = 0L
                val roll = Random.nextInt(100)
                val kind = when {
                    roll < 42 -> FallingKind.ONIGIRI
                    roll < 68 -> FallingKind.STRAWBERRY
                    roll < 84 -> FallingKind.COIN
                    else -> FallingKind.SOCK
                }
                drops.add(
                    FallingDrop(
                        id = nextId++,
                        xFrac = Random.nextFloat() * 0.80f + 0.10f,
                        yFrac = -0.06f,
                        speedPerSec = 0.42f + Random.nextFloat() * 0.25f,
                        kind = kind
                    )
                )
            }

            val iterator = drops.listIterator()
            while (iterator.hasNext()) {
                val item = iterator.next()
                val newY = item.yFrac + item.speedPerSec * dtSec
                // Collision check near player at yFrac ~ 0.78..0.92
                if (newY in 0.74f..0.92f && abs(item.xFrac - playerXFrac) < 0.14f) {
                    score = (score + item.kind.points).coerceAtLeast(0)
                    feedbackText = if (item.kind == FallingKind.SOCK) {
                        "Ối vớ thối! -10 điểm"
                    } else {
                        "Hứng trúng ${item.kind.icon} (${item.kind.label} điểm)"
                    }
                    iterator.remove()
                } else if (newY > 1.04f) {
                    iterator.remove()
                } else {
                    iterator.set(item.copy(yFrac = newY))
                }
            }
        }
        drops.clear()
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Text(
                text = "⏱ ${(timeLeftMs + 999) / 1000}s",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Text(
                text = feedbackText,
                fontSize = 11.5.sp,
                color = textSecondary
            )
            Text(
                text = "Điểm: $score",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(cardColor)
                .border(1.dp, border, RoundedCornerShape(20.dp))
                .pointerInput(gameEnded) {
                    if (!gameEnded) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            val delta = dragAmount / size.width.toFloat()
                            playerXFrac = (playerXFrac + delta).coerceIn(0.12f, 0.88f)
                        }
                    }
                }
        ) {
            val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
            val heightPx = with(LocalDensity.current) { maxHeight.toPx() }
            val density = LocalDensity.current

            if (!gameEnded) {
                // Render falling drops
                drops.forEach { drop ->
                    val dx = (drop.xFrac * widthPx) - with(density) { 18.dp.toPx() }
                    val dy = (drop.yFrac * heightPx) - with(density) { 18.dp.toPx() }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .offset { IntOffset(dx.roundToInt(), dy.roundToInt()) }
                            .size(36.dp)
                    ) {
                        Text(text = drop.kind.icon, fontSize = 26.sp)
                    }
                }

                // Render controllable 2D Bubu & Dudu at bottom lane
                val petW = 104.dp
                val petH = 82.dp
                val px = (playerXFrac * widthPx) - with(density) { (petW / 2).toPx() }
                val py = heightPx - with(density) { petH.toPx() } - with(density) { 10.dp.toPx() }
                Image(
                    bitmap = playerBitmap,
                    contentDescription = "Bubu Dudu Catcher",
                    modifier = Modifier
                        .offset { IntOffset(px.roundToInt(), py.roundToInt()) }
                        .size(width = petW, height = petH)
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize().padding(20.dp)
                ) {
                    Text(
                        text = "🎉 Hết giờ!",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Điểm của bạn: $score\nPhần thưởng: +$earnedCoins Xu",
                        textAlign = TextAlign.Center,
                        fontSize = 15.sp,
                        color = textSecondary
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(cardColor)
                                .border(1.dp, border, RoundedCornerShape(14.dp))
                                .clickable { gameRoundId++ }
                                .padding(horizontal = 16.dp, vertical = 11.dp)
                        ) {
                            Text("Chơi lại", fontWeight = FontWeight.SemiBold, color = textPrimary)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(accent)
                                .clickable { onFinishClaim(earnedCoins) }
                                .padding(horizontal = 20.dp, vertical = 11.dp)
                        ) {
                            Text("Nhận +$earnedCoins xu", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RockPaperScissorsGame(
    equippedHat: PetHat,
    equippedGlasses: PetGlasses,
    cardColor: Color,
    border: Color,
    accent: Color,
    textPrimary: Color,
    textSecondary: Color,
    onWinRound: (Int) -> Unit
) {
    val context = LocalContext.current
    var duduPick by remember { mutableStateOf<RpsChoice?>(null) }
    var userPick by remember { mutableStateOf<RpsChoice?>(null) }
    var outcomeText by remember { mutableStateOf("Chọn Búa, Kéo hoặc Bao để đấu với Dudu!") }
    var resultPose by remember { mutableStateOf(PetMoodPose.CHILL_PLAY) }

    val avatarBmp = remember(resultPose, equippedHat, equippedGlasses) {
        Pet2DRenderer.getDressedBitmap(context, resultPose, equippedHat, equippedGlasses).asImageBitmap()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardColor)
            .border(1.dp, border, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Image(
            bitmap = avatarBmp,
            contentDescription = "Dudu opponent",
            modifier = Modifier.size(width = 160.dp, height = 120.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (userPick != null && duduPick != null) {
            Text(
                text = "Bạn: ${userPick!!.icon}  vs  Dudu: ${duduPick!!.icon}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        Text(
            text = outcomeText,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = textSecondary
        )
        Spacer(modifier = Modifier.height(18.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            RpsChoice.entries.forEach { choice ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(accent.copy(alpha = 0.14f))
                        .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                        .clickable {
                            val bot = RpsChoice.entries[Random.nextInt(RpsChoice.entries.size)]
                            userPick = choice
                            duduPick = bot
                            when {
                                choice == bot -> {
                                    resultPose = PetMoodPose.CHILL_PLAY
                                    outcomeText = "Hòa nhau! Nhận nhẹ +3 Xu"
                                    onWinRound(3)
                                }
                                (choice == RpsChoice.ROCK && bot == RpsChoice.SCISSORS) ||
                                    (choice == RpsChoice.SCISSORS && bot == RpsChoice.PAPER) ||
                                    (choice == RpsChoice.PAPER && bot == RpsChoice.ROCK) -> {
                                    resultPose = PetMoodPose.HUNGRY
                                    outcomeText = "Bạn thắng rồi!! +12 Xu vào ví!"
                                    onWinRound(12)
                                }
                                else -> {
                                    resultPose = PetMoodPose.HAPPY_WAVE
                                    outcomeText = "Dudu thắng rồi hihi! Thử lại nhé (+1 Xu an ủi)"
                                    onWinRound(1)
                                }
                            }
                        }
                        .padding(vertical = 12.dp)
                ) {
                    Text(choice.icon, fontSize = 28.sp)
                    Text(
                        text = choice.label,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun GameTabPill(
    title: String,
    selected: Boolean,
    accent: Color,
    cardColor: Color,
    border: Color,
    textPrimary: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) accent else cardColor)
            .border(1.dp, if (selected) accent else border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp, horizontal = 8.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else textPrimary
        )
    }
}
