package com.petsonscreen.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petsonscreen.app.engine.MascotInstance
import com.petsonscreen.app.engine.TICK_MS
import com.petsonscreen.app.model.CommandEvent
import com.petsonscreen.app.sprites.PetSpriteCache
import com.petsonscreen.app.ui.theme.CleanSansFamily
import com.petsonscreen.app.ui.theme.PetColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun PetSpriteFrame(
    petId: Int,
    frameIndex: Int,
    facingRight: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageBitmap = remember(petId, frameIndex) {
        PetSpriteCache.getComposeFrame(context, petId, frameIndex)
    }
    Canvas(modifier = modifier) {
        val dstSize = IntSize(size.width.roundToInt().coerceAtLeast(1), size.height.roundToInt().coerceAtLeast(1))
        if (facingRight) {
            scale(scaleX = -1f, scaleY = 1f) {
                drawImage(
                    image = imageBitmap,
                    dstSize = dstSize
                )
            }
        } else {
            drawImage(
                image = imageBitmap,
                dstSize = dstSize
            )
        }
    }
}

@Composable
fun PetQuickMenuPopup(
    visible: Boolean,
    onNavigateHome: () -> Unit,
    onNavigateSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .shadow(8.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(PetColors.ObsidianInk)
                .border(1.2.dp, PetColors.ChampagneGold, RoundedCornerShape(24.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(onClick = onNavigateHome)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("quick_menu_home"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Home",
                    color = Color.White,
                    fontFamily = CleanSansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
            VerticalDivider(
                modifier = Modifier.height(18.dp),
                thickness = 1.dp,
                color = Color(0xFF353A47)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(onClick = onNavigateSettings)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("quick_menu_settings"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Settings",
                    color = Color.White,
                    fontFamily = CleanSansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun LivePetPlaygroundStage(
    petIds: List<Int>,
    interactive: Boolean,
    speed: Float = 1.5f,
    scale: Float = 1.5f,
    commandEvent: CommandEvent? = null,
    onNavigateHome: () -> Unit = {},
    onNavigateSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val spriteSizeDp = (68f * scale).dp
    val spriteSizePx = with(density) { spriteSizeDp.toPx() }
    val tapSlopPx = with(density) { 6.dp.toPx() }.coerceAtLeast(6f)

    BoxWithConstraints(modifier = modifier) {
        val widthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(spriteSizePx + 1f)
        val heightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(spriteSizePx + 1f)

        val mascots = remember(petIds) {
            mutableStateListOf<MascotInstance>().apply {
                petIds.forEachIndexed { index, id ->
                    val startX = ((widthPx - spriteSizePx) * ((index + 1f) / (petIds.size + 1f)))
                        .coerceAtLeast(0f)
                    val startY = (heightPx * 0.15f) + (index * 16f)
                    add(MascotInstance(instanceKey = "stage_${index}_$id", petId = id, initialX = startX, initialY = startY))
                }
            }
        }

        var tickCounter by remember { mutableIntStateOf(0) }
        var activeMenuKey by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(activeMenuKey) {
            if (activeMenuKey != null) {
                delay(5000L)
                activeMenuKey = null
            }
        }

        LaunchedEffect(commandEvent?.seq) {
            val evt = commandEvent ?: return@LaunchedEffect
            mascots.forEach { m ->
                m.applyCommand(evt.action, widthPx, heightPx, spriteSizePx)
            }
        }

        LaunchedEffect(petIds, widthPx, heightPx, speed, spriteSizePx) {
            while (isActive) {
                mascots.forEach { mascot ->
                    mascot.tick(widthPx, heightPx, spriteSizePx, speed)
                }
                tickCounter++
                delay(TICK_MS)
            }
        }

        // Read tickCounter so Canvas frames recompose cleanly at 30ms
        val currentTick = tickCounter
        if (currentTick >= 0) {
            mascots.forEach { mascot ->
                val isMenuOpen = activeMenuKey == mascot.instanceKey

                Box(
                    modifier = Modifier
                        .offset { IntOffset(mascot.x.roundToInt(), mascot.y.roundToInt()) }
                        .size(spriteSizeDp)
                        .then(
                            if (interactive) {
                                Modifier.pointerInput(mascot.instanceKey, widthPx, heightPx, spriteSizePx) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val down = awaitPointerEvent().changes.firstOrNull { it.pressed } ?: continue
                                            val downPos = down.position
                                            val downTime = down.uptimeMillis
                                            var maxDrag = 0f
                                            val tracker = VelocityTracker()
                                            tracker.addPosition(down.uptimeMillis, down.position)
                                            activeMenuKey = null
                                            mascot.startDrag()
                                            down.consume()

                                            while (true) {
                                                val event = awaitPointerEvent()
                                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                                if (!change.pressed) {
                                                    tracker.addPosition(change.uptimeMillis, change.position)
                                                    val vel = tracker.calculateVelocity()
                                                    val elapsed = change.uptimeMillis - downTime
                                                    if (maxDrag < tapSlopPx && elapsed < 400L) {
                                                        mascot.endDrag(0f, 0f)
                                                        activeMenuKey = mascot.instanceKey
                                                    } else {
                                                        mascot.endDrag(vel.x, vel.y)
                                                    }
                                                    change.consume()
                                                    break
                                                }
                                                val delta = change.position - change.previousPosition
                                                tracker.addPosition(change.uptimeMillis, change.position)
                                                val dist = hypot(
                                                    (change.position.x - downPos.x),
                                                    (change.position.y - downPos.y)
                                                )
                                                if (dist > maxDrag) maxDrag = dist
                                                mascot.dragBy(delta.x, delta.y, widthPx, heightPx, spriteSizePx)
                                                change.consume()
                                            }
                                        }
                                    }
                                }
                            } else {
                                Modifier
                            }
                        )
                ) {
                    PetSpriteFrame(
                        petId = mascot.petId,
                        frameIndex = mascot.currentFrameIndex,
                        facingRight = mascot.facingRight,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (interactive && isMenuOpen) {
                    val menuW = with(density) { 176.dp.toPx() }
                    val menuH = with(density) { 44.dp.toPx() }
                    val mx = (mascot.x + spriteSizePx * 0.5f - menuW * 0.5f)
                        .coerceIn(8f, (widthPx - menuW - 8f).coerceAtLeast(8f))
                    val my = (mascot.y - menuH - 8f)
                        .coerceIn(8f, (heightPx - menuH - 8f).coerceAtLeast(8f))
                    PetQuickMenuPopup(
                        visible = true,
                        onNavigateHome = {
                            activeMenuKey = null
                            onNavigateHome()
                        },
                        onNavigateSettings = {
                            activeMenuKey = null
                            onNavigateSettings()
                        },
                        modifier = Modifier.offset { IntOffset(mx.roundToInt(), my.roundToInt()) }
                    )
                }
            }
        }
    }
}

@Composable
fun InAppPetOverlay(
    petIds: List<Int>,
    speed: Float,
    scale: Float,
    commandEvent: CommandEvent?,
    onNavigateHome: () -> Unit,
    onNavigateSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (petIds.isEmpty()) return
    LivePetPlaygroundStage(
        petIds = petIds,
        interactive = true,
        speed = speed,
        scale = scale,
        commandEvent = commandEvent,
        onNavigateHome = onNavigateHome,
        onNavigateSettings = onNavigateSettings,
        modifier = modifier.fillMaxSize()
    )
}
