package com.bubududu.pet.model

enum class DailyRoutine(
    val shortTitle: String,
    val statusLine: String
) {
    MORNING_BRUSH("Sáng đánh răng", "Hai bé đang đánh răng sảng khoái!"),
    LUNCH_EAT("Bữa trưa ngon", "Đang cùng nhau ăn cơm trưa nè!"),
    DINNER_EAT("Bữa tối ấm áp", "Ăn tối no nê cùng Bubu & Dudu"),
    NIGHT_SLEEP("Đắp chăn ngủ ngon", "Khò khò... Hai bé ngủ say rồi"),
    DAY_CHILL("Giờ vui chơi", "Đang chơi ngoan và đợi bạn xoa đầu")
}

enum class PreviewScheduleMode(
    val displayName: String,
    val forcedHour: Int?
) {
    AUTO("Giờ thực", null),
    MORNING("Sáng (07:00)", 7),
    NOON("Trưa (12:00)", 12),
    AFTERNOON("Chiều (16:00)", 16),
    NIGHT("Đêm (23:00)", 23)
}

enum class PetMoodPose {
    BRUSH_TEETH,
    EAT_RICE,
    SLEEP_BLANKET,
    HUNGRY,
    CHILL_PLAY,
    HAPPY_WAVE
}

enum class TemporaryAction(
    val bubbleText: String,
    val pose: PetMoodPose
) {
    FEED("Cơm ngon quá! Chén sạch bách luôn!", PetMoodPose.EAT_RICE),
    BRUSH_CLEAN("Chải sạch bọt trắng, phòng thơm tho!", PetMoodPose.BRUSH_TEETH),
    PAT("Êm quá đi~ Thích được xoa đầu nhất!", PetMoodPose.HAPPY_WAVE),
    NAP("Kéo chăn lên chợp mắt tí nhé...", PetMoodPose.SLEEP_BLANKET)
}

enum class PetHat(
    val displayName: String,
    val price: Int
) {
    NONE("Không đội", 0),
    STRAW_HAT("Mũ cói nắng", 0),
    DAISY_CLIP("Kẹp hoa cúc", 25),
    BERRY_BEANIE("Mũ len gấu", 35),
    MINI_CROWN("Vương miện mini", 70)
}

enum class PetGlasses(
    val displayName: String,
    val price: Int
) {
    NONE("Không đeo", 0),
    ROUND_SUN("Kính râm tròn", 0),
    PINK_SHADES("Kính hồng cute", 30),
    STUDY_GLASSES("Kính cận tròn", 45),
    STAR_SHADES("Kính ngôi sao", 80)
}

enum class RoomTheme(
    val displayName: String,
    val wallColorHex: Long,
    val cardColorHex: Long,
    val floorColorHex: Long,
    val borderColorHex: Long,
    val textPrimaryHex: Long,
    val textSecondaryHex: Long,
    val accentHex: Long,
    val isDark: Boolean
) {
    CREAM_DAY(
        displayName = "Kem Sữa Ấm",
        wallColorHex = 0xFFFFF8F0,
        cardColorHex = 0xFFFFFFFF,
        floorColorHex = 0xFFF3E2D2,
        borderColorHex = 0xFFEAD7C7,
        textPrimaryHex = 0xFF33241F,
        textSecondaryHex = 0xFF846B60,
        accentHex = 0xFFE17D7D,
        isDark = false
    ),
    SKY_BREEZE(
        displayName = "Trời Xanh Mát",
        wallColorHex = 0xFFF1FAFC,
        cardColorHex = 0xFFFFFFFF,
        floorColorHex = 0xFFDFEFF4,
        borderColorHex = 0xFFD0E3EA,
        textPrimaryHex = 0xFF1F2D33,
        textSecondaryHex = 0xFF647D87,
        accentHex = 0xFF569FC2,
        isDark = false
    ),
    NIGHT_LAMP(
        displayName = "Đèn Ngủ Dịu",
        wallColorHex = 0xFF1D1B26,
        cardColorHex = 0xFF272434,
        floorColorHex = 0xFF322C42,
        borderColorHex = 0xFF3E3852,
        textPrimaryHex = 0xFFF5ECE4,
        textSecondaryHex = 0xFFB0A3B8,
        accentHex = 0xFFF2AE82,
        isDark = true
    )
}

data class PetState(
    val hunger: Int = 76,
    val hygiene: Int = 82,
    val happiness: Int = 84,
    val energy: Int = 88,
    val messCount: Int = 1,
    val coins: Int = 45,
    val xp: Int = 20,
    val equippedHat: PetHat = PetHat.STRAW_HAT,
    val equippedGlasses: PetGlasses = PetGlasses.NONE,
    val unlockedHats: Set<PetHat> = setOf(PetHat.NONE, PetHat.STRAW_HAT),
    val unlockedGlasses: Set<PetGlasses> = setOf(PetGlasses.NONE, PetGlasses.ROUND_SUN),
    val roomTheme: RoomTheme = RoomTheme.CREAM_DAY,
    val previewMode: PreviewScheduleMode = PreviewScheduleMode.AUTO,
    val lastUpdatedAt: Long = 0L,
    val totalFeeds: Int = 0,
    val totalCleans: Int = 0,
    val totalPets: Int = 0
) {
    val level: Int get() = 1 + (xp / 100)
    val levelProgress: Float get() = (xp % 100) / 100f
}
