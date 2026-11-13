package com.bubududu.pet

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bubududu.pet.data.PetRepository
import com.bubududu.pet.model.PetState
import com.bubududu.pet.model.PreviewScheduleMode
import com.bubududu.pet.model.TemporaryAction
import com.bubududu.pet.ui.HomePetScreen
import com.bubududu.pet.ui.MiniGameDialog
import com.bubududu.pet.ui.WardrobeSheet
import com.bubududu.pet.widget.PetWidgetProvider
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repo = PetRepository(applicationContext)
        PetWidgetProvider.schedulePeriodicRefresh(applicationContext)

        setContent {
            var petState by remember { mutableStateOf(repo.loadState()) }
            var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
            var tempAction by remember { mutableStateOf<TemporaryAction?>(null) }
            var tempActionSeq by remember { mutableIntStateOf(0) }
            var showWardrobe by remember { mutableStateOf(false) }
            var showMiniGame by remember { mutableStateOf(false) }

            val snapshot = remember(petState, tempAction, nowMillis) {
                repo.resolveRoutine(petState, tempAction, nowMillis)
            }

            LaunchedEffect(petState.roomTheme.isDark) {
                val style = if (petState.roomTheme.isDark) {
                    SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                } else {
                    SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }

            // Clear temporary reaction pose after ~3.5 seconds
            LaunchedEffect(tempActionSeq) {
                if (tempActionSeq > 0) {
                    delay(3500L)
                    tempAction = null
                }
            }

            // Sync elapsed state, widget updates, and real-time clock ticks
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        val now = System.currentTimeMillis()
                        nowMillis = now
                        petState = repo.loadState(now)
                        PetWidgetProvider.updateAllWidgets(applicationContext, petState)
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)

                val timeReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        val now = System.currentTimeMillis()
                        nowMillis = now
                        petState = repo.applyElapsedDecay(petState, now)
                        PetWidgetProvider.updateAllWidgets(applicationContext, petState)
                    }
                }
                val filter = IntentFilter().apply {
                    addAction(Intent.ACTION_TIME_TICK)
                    addAction(Intent.ACTION_TIME_CHANGED)
                    addAction(Intent.ACTION_TIMEZONE_CHANGED)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    registerReceiver(timeReceiver, filter, RECEIVER_EXPORTED)
                } else {
                    @Suppress("UnspecifiedRegisterReceiverFlag")
                    registerReceiver(timeReceiver, filter)
                }

                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    runCatching { unregisterReceiver(timeReceiver) }
                }
            }

            fun applyMutation(next: PetState, action: TemporaryAction? = null) {
                petState = next
                if (action != null) {
                    tempAction = action
                    tempActionSeq++
                }
                PetWidgetProvider.updateAllWidgets(applicationContext, next, action)
            }

            fun requestPinHomeWidget() {
                val appWidgetManager = AppWidgetManager.getInstance(this@MainActivity)
                val provider = ComponentName(this@MainActivity, PetWidgetProvider::class.java)
                if (appWidgetManager.isRequestPinAppWidgetSupported) {
                    appWidgetManager.requestPinAppWidget(provider, null, null)
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Nhấn giữ ngoài màn hình chính -> Widget -> chọn Bubu Dudu Pet",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            MaterialTheme {
                HomePetScreen(
                    state = petState,
                    snapshot = snapshot,
                    tempAction = tempAction,
                    nowMillis = nowMillis,
                    onFeed = { applyMutation(repo.feedPet(petState), TemporaryAction.FEED) },
                    onCleanAll = {
                        applyMutation(repo.cleanRoomAndBrush(petState), TemporaryAction.BRUSH_CLEAN)
                    },
                    onSweepSingleMess = {
                        applyMutation(repo.sweepSingleMess(petState))
                        Toast.makeText(this@MainActivity, "Đã quét sạch! +5 xu", Toast.LENGTH_SHORT).show()
                    },
                    onPatPet = { applyMutation(repo.patPet(petState), TemporaryAction.PAT) },
                    onNap = { applyMutation(repo.restNap(petState), TemporaryAction.NAP) },
                    onCyclePreviewMode = {
                        val modes = PreviewScheduleMode.entries
                        val next = modes[(modes.indexOf(petState.previewMode) + 1) % modes.size]
                        applyMutation(repo.updateRoomAndPreview(petState, preview = next))
                        Toast.makeText(
                            this@MainActivity,
                            "Khung giờ: ${next.displayName}",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onOpenWardrobe = { showWardrobe = true },
                    onOpenMiniGame = { showMiniGame = true },
                    onPinWidget = ::requestPinHomeWidget
                )

                if (showWardrobe) {
                    WardrobeSheet(
                        state = petState,
                        currentPose = snapshot.pose,
                        onDismiss = { showWardrobe = false },
                        onSelectHat = { hat ->
                            val (next, ok) = repo.selectOrBuyHat(petState, hat)
                            if (ok) {
                                applyMutation(next)
                            } else {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Cần ${hat.price} xu! Hãy chơi Mini-Game hoặc dọn phòng",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onSelectGlasses = { glasses ->
                            val (next, ok) = repo.selectOrBuyGlasses(petState, glasses)
                            if (ok) {
                                applyMutation(next)
                            } else {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Cần ${glasses.price} xu! Hãy chơi Mini-Game hoặc dọn phòng",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onSelectTheme = { theme ->
                            applyMutation(repo.updateRoomAndPreview(petState, theme = theme))
                        },
                        onSelectPreviewMode = { mode ->
                            applyMutation(repo.updateRoomAndPreview(petState, preview = mode))
                        },
                        onPinWidget = {
                            showWardrobe = false
                            requestPinHomeWidget()
                        }
                    )
                }

                if (showMiniGame) {
                    MiniGameDialog(
                        roomTheme = petState.roomTheme,
                        equippedHat = petState.equippedHat,
                        equippedGlasses = petState.equippedGlasses,
                        onClaimCoins = { coins, xp ->
                            applyMutation(repo.addMiniGameReward(petState, coins, xp))
                            Toast.makeText(
                                this@MainActivity,
                                "Đã cộng +$coins xu!",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onDismiss = { showMiniGame = false }
                    )
                }
            }
        }
    }
}
