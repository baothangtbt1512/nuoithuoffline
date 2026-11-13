package com.bubududu.pet.data

import android.content.Context
import com.bubududu.pet.model.DailyRoutine
import com.bubududu.pet.model.PetGlasses
import com.bubududu.pet.model.PetHat
import com.bubududu.pet.model.PetMoodPose
import com.bubududu.pet.model.PetState
import com.bubududu.pet.model.PreviewScheduleMode
import com.bubududu.pet.model.RoomTheme
import com.bubududu.pet.model.TemporaryAction
import java.util.Calendar

data class RoutineSnapshot(
    val routine: DailyRoutine,
    val pose: PetMoodPose,
    val effectiveHour: Int,
    val isNightVisual: Boolean
)

class PetRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadState(nowMillis: Long = System.currentTimeMillis()): PetState {
        val raw = PetState(
            hunger = prefs.getInt(KEY_HUNGER, 76).coerceIn(0, 100),
            hygiene = prefs.getInt(KEY_HYGIENE, 82).coerceIn(0, 100),
            happiness = prefs.getInt(KEY_HAPPINESS, 84).coerceIn(0, 100),
            energy = prefs.getInt(KEY_ENERGY, 88).coerceIn(0, 100),
            messCount = prefs.getInt(KEY_MESS, 1).coerceIn(0, 4),
            coins = prefs.getInt(KEY_COINS, 45).coerceAtLeast(0),
            xp = prefs.getInt(KEY_XP, 20).coerceAtLeast(0),
            equippedHat = enumOrDefault(prefs.getString(KEY_HAT, null), PetHat.STRAW_HAT),
            equippedGlasses = enumOrDefault(prefs.getString(KEY_GLASSES, null), PetGlasses.NONE),
            unlockedHats = decodeEnumSet(
                prefs.getStringSet(KEY_UNLOCKED_HATS, null),
                setOf(PetHat.NONE, PetHat.STRAW_HAT)
            ),
            unlockedGlasses = decodeEnumSet(
                prefs.getStringSet(KEY_UNLOCKED_GLASSES, null),
                setOf(PetGlasses.NONE, PetGlasses.ROUND_SUN)
            ),
            roomTheme = enumOrDefault(prefs.getString(KEY_THEME, null), RoomTheme.CREAM_DAY),
            previewMode = enumOrDefault(
                prefs.getString(KEY_PREVIEW_MODE, null),
                PreviewScheduleMode.AUTO
            ),
            lastUpdatedAt = prefs.getLong(KEY_LAST_UPDATED, 0L),
            totalFeeds = prefs.getInt(KEY_TOTAL_FEEDS, 0),
            totalCleans = prefs.getInt(KEY_TOTAL_CLEANS, 0),
            totalPets = prefs.getInt(KEY_TOTAL_PETS, 0)
        )
        return applyElapsedDecay(raw, nowMillis)
    }

    fun saveState(state: PetState) {
        prefs.edit()
            .putInt(KEY_HUNGER, state.hunger.coerceIn(0, 100))
            .putInt(KEY_HYGIENE, state.hygiene.coerceIn(0, 100))
            .putInt(KEY_HAPPINESS, state.happiness.coerceIn(0, 100))
            .putInt(KEY_ENERGY, state.energy.coerceIn(0, 100))
            .putInt(KEY_MESS, state.messCount.coerceIn(0, 4))
            .putInt(KEY_COINS, state.coins.coerceAtLeast(0))
            .putInt(KEY_XP, state.xp.coerceAtLeast(0))
            .putString(KEY_HAT, state.equippedHat.name)
            .putString(KEY_GLASSES, state.equippedGlasses.name)
            .putStringSet(KEY_UNLOCKED_HATS, state.unlockedHats.map { it.name }.toSet())
            .putStringSet(KEY_UNLOCKED_GLASSES, state.unlockedGlasses.map { it.name }.toSet())
            .putString(KEY_THEME, state.roomTheme.name)
            .putString(KEY_PREVIEW_MODE, state.previewMode.name)
            .putLong(KEY_LAST_UPDATED, state.lastUpdatedAt)
            .putInt(KEY_TOTAL_FEEDS, state.totalFeeds)
            .putInt(KEY_TOTAL_CLEANS, state.totalCleans)
            .putInt(KEY_TOTAL_PETS, state.totalPets)
            .apply()
    }

    fun applyElapsedDecay(state: PetState, nowMillis: Long): PetState {
        if (state.lastUpdatedAt <= 0L) {
            val initialized = state.copy(lastUpdatedAt = nowMillis)
            saveState(initialized)
            return initialized
        }
        val elapsedMs = (nowMillis - state.lastUpdatedAt).coerceAtLeast(0L)
        val steps = (elapsedMs / DECAY_STEP_MS).toInt().coerceAtMost(48)
        if (steps <= 0) return state

        var hunger = state.hunger
        var hygiene = state.hygiene
        var happiness = state.happiness
        var energy = state.energy
        var mess = state.messCount

        val cal = Calendar.getInstance()
        for (i in 1..steps) {
            cal.timeInMillis = state.lastUpdatedAt + i * DECAY_STEP_MS
            val hr = cal.get(Calendar.HOUR_OF_DAY)
            val night = hr >= 22 || hr < 6
            if (night) {
                hunger = (hunger - 1).coerceAtLeast(10)
                hygiene = (hygiene - 1).coerceAtLeast(15)
                happiness = (happiness - 1).coerceAtLeast(20)
                energy = (energy + 6).coerceAtMost(100)
            } else {
                hunger = (hunger - 3).coerceAtLeast(8)
                hygiene = (hygiene - 2).coerceAtLeast(12)
                happiness = (happiness - 2).coerceAtLeast(15)
                energy = (energy - 2).coerceAtLeast(15)
                if (i % 3 == 0 && mess < 4) {
                    mess++
                }
            }
        }

        val updated = state.copy(
            hunger = hunger,
            hygiene = hygiene,
            happiness = happiness,
            energy = energy,
            messCount = mess,
            lastUpdatedAt = state.lastUpdatedAt + steps * DECAY_STEP_MS
        )
        saveState(updated)
        return updated
    }

    fun resolveRoutine(
        state: PetState,
        temporaryAction: TemporaryAction? = null,
        nowMillis: Long = System.currentTimeMillis()
    ): RoutineSnapshot {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val hour = state.previewMode.forcedHour ?: cal.get(Calendar.HOUR_OF_DAY)
        val minute = if (state.previewMode.forcedHour != null) 0 else cal.get(Calendar.MINUTE)

        val routine = when {
            hour in 6..7 || (hour == 8 && minute <= 30) -> DailyRoutine.MORNING_BRUSH
            hour in 11..12 || (hour == 13 && minute <= 30) -> DailyRoutine.LUNCH_EAT
            hour == 18 || (hour == 19 && minute <= 30) -> DailyRoutine.DINNER_EAT
            hour >= 22 || hour < 6 -> DailyRoutine.NIGHT_SLEEP
            else -> DailyRoutine.DAY_CHILL
        }

        val basePose = when (routine) {
            DailyRoutine.MORNING_BRUSH -> PetMoodPose.BRUSH_TEETH
            DailyRoutine.LUNCH_EAT, DailyRoutine.DINNER_EAT -> PetMoodPose.EAT_RICE
            DailyRoutine.NIGHT_SLEEP -> PetMoodPose.SLEEP_BLANKET
            DailyRoutine.DAY_CHILL -> when {
                state.hunger < 30 || state.messCount >= 3 || state.hygiene < 30 -> PetMoodPose.HUNGRY
                state.happiness >= 86 -> PetMoodPose.HAPPY_WAVE
                else -> PetMoodPose.CHILL_PLAY
            }
        }

        val finalPose = temporaryAction?.pose ?: basePose
        val isNightVisual = hour >= 20 || hour < 6 || state.roomTheme == RoomTheme.NIGHT_LAMP
        return RoutineSnapshot(
            routine = routine,
            pose = finalPose,
            effectiveHour = hour,
            isNightVisual = isNightVisual
        )
    }

    fun feedPet(current: PetState, nowMillis: Long = System.currentTimeMillis()): PetState {
        val base = applyElapsedDecay(current, nowMillis)
        val extraMess = if ((base.totalFeeds + 1) % 3 == 0 && base.messCount < 4) 1 else 0
        val next = base.copy(
            hunger = (base.hunger + 24).coerceAtMost(100),
            happiness = (base.happiness + 7).coerceAtMost(100),
            energy = (base.energy + 4).coerceAtMost(100),
            messCount = (base.messCount + extraMess).coerceAtMost(4),
            xp = base.xp + 10,
            coins = base.coins + 2,
            lastUpdatedAt = nowMillis,
            totalFeeds = base.totalFeeds + 1
        )
        saveState(next)
        return next
    }

    fun cleanRoomAndBrush(current: PetState, nowMillis: Long = System.currentTimeMillis()): PetState {
        val base = applyElapsedDecay(current, nowMillis)
        val coinBonus = base.messCount * 4 + 3
        val next = base.copy(
            hygiene = (base.hygiene + 28).coerceAtMost(100),
            happiness = (base.happiness + 6).coerceAtMost(100),
            messCount = 0,
            xp = base.xp + 12,
            coins = base.coins + coinBonus,
            lastUpdatedAt = nowMillis,
            totalCleans = base.totalCleans + 1
        )
        saveState(next)
        return next
    }

    fun sweepSingleMess(current: PetState, nowMillis: Long = System.currentTimeMillis()): PetState {
        val base = applyElapsedDecay(current, nowMillis)
        if (base.messCount <= 0) return base
        val next = base.copy(
            messCount = (base.messCount - 1).coerceAtLeast(0),
            hygiene = (base.hygiene + 9).coerceAtMost(100),
            coins = base.coins + 5,
            xp = base.xp + 6,
            lastUpdatedAt = nowMillis,
            totalCleans = base.totalCleans + 1
        )
        saveState(next)
        return next
    }

    fun patPet(current: PetState, nowMillis: Long = System.currentTimeMillis()): PetState {
        val base = applyElapsedDecay(current, nowMillis)
        val next = base.copy(
            happiness = (base.happiness + 12).coerceAtMost(100),
            xp = base.xp + 5,
            coins = base.coins + 1,
            lastUpdatedAt = nowMillis,
            totalPets = base.totalPets + 1
        )
        saveState(next)
        return next
    }

    fun restNap(current: PetState, nowMillis: Long = System.currentTimeMillis()): PetState {
        val base = applyElapsedDecay(current, nowMillis)
        val next = base.copy(
            energy = (base.energy + 25).coerceAtMost(100),
            happiness = (base.happiness + 5).coerceAtMost(100),
            xp = base.xp + 8,
            lastUpdatedAt = nowMillis
        )
        saveState(next)
        return next
    }

    fun selectOrBuyHat(current: PetState, hat: PetHat): Pair<PetState, Boolean> {
        if (hat in current.unlockedHats) {
            val next = current.copy(equippedHat = hat)
            saveState(next)
            return next to true
        }
        if (current.coins < hat.price) return current to false
        val next = current.copy(
            coins = current.coins - hat.price,
            unlockedHats = current.unlockedHats + hat,
            equippedHat = hat,
            xp = current.xp + 15
        )
        saveState(next)
        return next to true
    }

    fun selectOrBuyGlasses(current: PetState, glasses: PetGlasses): Pair<PetState, Boolean> {
        if (glasses in current.unlockedGlasses) {
            val next = current.copy(equippedGlasses = glasses)
            saveState(next)
            return next to true
        }
        if (current.coins < glasses.price) return current to false
        val next = current.copy(
            coins = current.coins - glasses.price,
            unlockedGlasses = current.unlockedGlasses + glasses,
            equippedGlasses = glasses,
            xp = current.xp + 15
        )
        saveState(next)
        return next to true
    }

    fun updateRoomAndPreview(
        current: PetState,
        theme: RoomTheme = current.roomTheme,
        preview: PreviewScheduleMode = current.previewMode
    ): PetState {
        val next = current.copy(roomTheme = theme, previewMode = preview)
        saveState(next)
        return next
    }

    fun addMiniGameReward(current: PetState, coinsWon: Int, xpGain: Int): PetState {
        val base = applyElapsedDecay(current, System.currentTimeMillis())
        val next = base.copy(
            coins = base.coins + coinsWon.coerceAtLeast(0),
            happiness = (base.happiness + 10).coerceAtMost(100),
            energy = (base.energy - 6).coerceAtLeast(10),
            xp = base.xp + xpGain.coerceAtLeast(0),
            lastUpdatedAt = System.currentTimeMillis()
        )
        saveState(next)
        return next
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T {
        if (name == null) return default
        return runCatching { enumValueOf<T>(name) }.getOrDefault(default)
    }

    private inline fun <reified T : Enum<T>> decodeEnumSet(
        raw: Set<String>?,
        default: Set<T>
    ): Set<T> {
        if (raw.isNullOrEmpty()) return default
        val parsed = raw.mapNotNull { runCatching { enumValueOf<T>(it) }.getOrNull() }.toSet()
        return parsed + default
    }

    companion object {
        private const val PREFS_NAME = "bubu_dudu_pet_prefs"
        private const val DECAY_STEP_MS = 45L * 60_000L

        private const val KEY_HUNGER = "hunger"
        private const val KEY_HYGIENE = "hygiene"
        private const val KEY_HAPPINESS = "happiness"
        private const val KEY_ENERGY = "energy"
        private const val KEY_MESS = "mess_count"
        private const val KEY_COINS = "coins"
        private const val KEY_XP = "xp"
        private const val KEY_HAT = "equipped_hat"
        private const val KEY_GLASSES = "equipped_glasses"
        private const val KEY_UNLOCKED_HATS = "unlocked_hats"
        private const val KEY_UNLOCKED_GLASSES = "unlocked_glasses"
        private const val KEY_THEME = "room_theme"
        private const val KEY_PREVIEW_MODE = "preview_mode"
        private const val KEY_LAST_UPDATED = "last_updated"
        private const val KEY_TOTAL_FEEDS = "total_feeds"
        private const val KEY_TOTAL_CLEANS = "total_cleans"
        private const val KEY_TOTAL_PETS = "total_pets"
    }
}
