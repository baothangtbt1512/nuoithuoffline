package com.bubududu.pet.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bubududu.pet.model.PetGlasses
import com.bubududu.pet.model.PetHat
import com.bubududu.pet.model.PetMoodPose
import com.bubududu.pet.model.PetState
import com.bubududu.pet.model.PreviewScheduleMode
import com.bubududu.pet.model.RoomTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WardrobeSheet(
    state: PetState,
    currentPose: PetMoodPose,
    onDismiss: () -> Unit,
    onSelectHat: (PetHat) -> Unit,
    onSelectGlasses: (PetGlasses) -> Unit,
    onSelectTheme: (RoomTheme) -> Unit,
    onSelectPreviewMode: (PreviewScheduleMode) -> Unit,
    onPinWidget: () -> Unit
) {
    val context = LocalContext.current
    val theme = state.roomTheme
    val bgColor = Color(theme.cardColorHex)
    val textPrimary = Color(theme.textPrimaryHex)
    val textSecondary = Color(theme.textSecondaryHex)
    val accent = Color(theme.accentHex)
    val border = Color(theme.borderColorHex)
    val pillBg = Color(theme.floorColorHex).copy(alpha = 0.7f)

    // Always showcase accessories clearly in wardrobe preview (even if night sleeping)
    val previewPose = if (currentPose == PetMoodPose.SLEEP_BLANKET) {
        PetMoodPose.CHILL_PLAY
    } else {
        currentPose
    }
    val previewBitmap = remember(previewPose, state.equippedHat, state.equippedGlasses) {
        Pet2DRenderer.getDressedBitmap(
            context,
            previewPose,
            state.equippedHat,
            state.equippedGlasses
        ).asImageBitmap()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = bgColor,
        contentColor = textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Tủ đồ & Cài đặt 2D",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Text(
                        text = "Đang có ${state.coins} xu • Chọn mũ & kính đồng bộ Widget",
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(pillBg)
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text("Xong", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Live 2D dressed preview card
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(theme.wallColorHex))
                    .border(1.dp, border, RoundedCornerShape(20.dp))
            ) {
                Image(
                    bitmap = previewBitmap,
                    contentDescription = "Bubu & Dudu 2D Preview",
                    modifier = Modifier.size(width = 190.dp, height = 132.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pin Widget button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(accent)
                    .clickable(onClick = onPinWidget)
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    text = "📌 Ghim Widget Bubu & Dudu ra màn hình chính",
                    color = Color.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            SheetSectionTitle("👒 Mũ & Kẹp Tóc 2D", accent)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PetHat.entries.forEach { hat ->
                    val unlocked = hat in state.unlockedHats
                    val equipped = hat == state.equippedHat
                    val label = when {
                        equipped -> "✓ ${hat.displayName}"
                        unlocked -> hat.displayName
                        else -> "${hat.displayName} (${hat.price} xu)"
                    }
                    SelectableChip(
                        text = label,
                        selected = equipped,
                        locked = !unlocked,
                        accent = accent,
                        pillBg = pillBg,
                        border = border,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        onClick = { onSelectHat(hat) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            SheetSectionTitle("🕶️ Kính 2D", accent)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PetGlasses.entries.forEach { glasses ->
                    val unlocked = glasses in state.unlockedGlasses
                    val equipped = glasses == state.equippedGlasses
                    val label = when {
                        equipped -> "✓ ${glasses.displayName}"
                        unlocked -> glasses.displayName
                        else -> "${glasses.displayName} (${glasses.price} xu)"
                    }
                    SelectableChip(
                        text = label,
                        selected = equipped,
                        locked = !unlocked,
                        accent = accent,
                        pillBg = pillBg,
                        border = border,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        onClick = { onSelectGlasses(glasses) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = border, thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(16.dp))

            SheetSectionTitle("🏡 Chủ đề căn phòng", accent)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                RoomTheme.entries.forEach { opt ->
                    val selected = opt == state.roomTheme
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) accent else pillBg)
                            .border(1.dp, if (selected) accent else border, RoundedCornerShape(14.dp))
                            .clickable { onSelectTheme(opt) }
                            .padding(vertical = 10.dp, horizontal = 6.dp)
                    ) {
                        Text(
                            text = opt.displayName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selected) Color.White else textPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            SheetSectionTitle("🕒 Chế độ giờ sinh hoạt", accent)
            Text(
                text = "Bình thường tự đổi Sáng đánh răng / Trưa ăn cơm / Đêm ngủ theo đồng hồ máy — bạn có thể xem thử nhanh bên dưới:",
                fontSize = 11.5.sp,
                color = textSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PreviewScheduleMode.entries.forEach { mode ->
                    val selected = mode == state.previewMode
                    SelectableChip(
                        text = if (selected) "✓ ${mode.displayName}" else mode.displayName,
                        selected = selected,
                        locked = false,
                        accent = accent,
                        pillBg = pillBg,
                        border = border,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        onClick = { onSelectPreviewMode(mode) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetSectionTitle(text: String, accent: Color) {
    Text(
        text = text,
        fontSize = 13.5.sp,
        fontWeight = FontWeight.Bold,
        color = accent,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun SelectableChip(
    text: String,
    selected: Boolean,
    locked: Boolean,
    accent: Color,
    pillBg: Color,
    border: Color,
    textPrimary: Color,
    textSecondary: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) accent else pillBg)
            .border(
                width = 1.dp,
                color = if (selected) accent else border,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = when {
                selected -> Color.White
                locked -> textSecondary
                else -> textPrimary
            }
        )
    }
}
