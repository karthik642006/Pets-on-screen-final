package com.petsonscreen.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petsonscreen.app.model.PetAppPreferences
import com.petsonscreen.app.ui.components.PetSpriteFrame
import com.petsonscreen.app.ui.theme.CleanSansFamily
import com.petsonscreen.app.ui.theme.PetColors
import com.petsonscreen.app.ui.theme.TabularNumberStyle

private val COUNT_LABELS = listOf(
    "One at random",
    "Two at random",
    "Three at random",
    "Four at random",
    "Five at random",
    "Six at random"
)

@Composable
fun HomeScreen(
    prefs: PetAppPreferences,
    canDrawOverlays: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onSelectNumberOnScreen: (Int) -> Unit,
    onTogglePetsHidden: () -> Unit,
    onSlotEmptyClick: (Int) -> Unit,
    onSlotFilledClick: (Int) -> Unit,
    onRequestOverlayPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCountPicker by remember { mutableStateOf(false) }

    val filledCount = prefs.filledSlotsCount
    val activeCount = prefs.activeDisplayCount()
    val subtitleText = if (!prefs.enabled || prefs.petsHidden) {
        "$filledCount of 9 slots filled · Paused"
    } else {
        "$filledCount of 9 slots filled · $activeCount active on screen"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PetColors.CanvasBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Editorial Header
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Pets On Screen",
                    style = MaterialTheme.typography.displaySmall
                )
                Text(
                    text = subtitleText,
                    style = TabularNumberStyle.copy(
                        fontSize = 14.sp,
                        color = PetColors.SecondaryMuted
                    )
                )
            }

            // Obsidian Master Switch Card
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = PetColors.ObsidianInk,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_master_switch_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f).padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Pets on Screen",
                            fontFamily = CleanSansFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp,
                            color = Color.White
                        )
                        val modeNote = when {
                            !prefs.enabled -> "Companions are resting"
                            prefs.petsHidden -> "Hidden via quick toggle"
                            canDrawOverlays -> "Floating over all apps"
                            else -> "Roaming inside app · Tap to enable overlay"
                        }
                        Text(
                            text = modeNote,
                            fontFamily = CleanSansFamily,
                            fontSize = 13.sp,
                            color = if (prefs.enabled && !canDrawOverlays) PetColors.ChampagneBorder else Color(0xFFA0A8B8),
                            modifier = if (prefs.enabled && !canDrawOverlays) {
                                Modifier.clickable(onClick = onRequestOverlayPermission)
                            } else Modifier
                        )
                    }

                    Switch(
                        checked = prefs.enabled,
                        onCheckedChange = onToggleEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PetColors.ChampagneGold,
                            uncheckedThumbColor = Color(0xFFD0D5DD),
                            uncheckedTrackColor = Color(0xFF282C36),
                            uncheckedBorderColor = Color(0xFF3A3F4D)
                        ),
                        modifier = Modifier.testTag("home_enabled_switch")
                    )
                }
            }

            // Active Companions Picker + Hide/Show toggle
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = PetColors.CardSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, PetColors.HairlineBorder, RoundedCornerShape(20.dp))
                    .clickable { showCountPicker = true }
                    .testTag("home_active_companions_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PetColors.ChampagneTint)
                                .border(1.dp, PetColors.ChampagneBorder, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Group,
                                contentDescription = "Active Companions",
                                tint = PetColors.ChampagneGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Active Companions",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = COUNT_LABELS[(prefs.numberOnScreen - 1).coerceIn(0, 5)],
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    Text(
                        text = "${prefs.numberOnScreen}",
                        style = TabularNumberStyle.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PetColors.ChampagneGold
                        )
                    )
                }
            }

            if (prefs.enabled) {
                OutlinedButton(
                    onClick = onTogglePetsHidden,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.dp,
                        if (prefs.petsHidden) PetColors.ChampagneGold else PetColors.HairlineBorder
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (prefs.petsHidden) PetColors.ChampagneTint else PetColors.CardSurface,
                        contentColor = PetColors.ObsidianInk
                    ),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_hide_show_button")
                ) {
                    Icon(
                        imageVector = if (prefs.petsHidden) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = null,
                        tint = if (prefs.petsHidden) PetColors.ChampagneGold else PetColors.ObsidianInk,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (prefs.petsHidden) "Show pets" else "Hide pets",
                        fontFamily = CleanSansFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }

            // Companion Roster 3x3 Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Layers,
                        contentDescription = null,
                        tint = PetColors.SecondaryMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Companion Roster",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Text(
                    text = "$filledCount / 9",
                    style = TabularNumberStyle.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PetColors.SecondaryMuted
                    )
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                for (row in 0 until 3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (col in 0 until 3) {
                            val slotIdx = row * 3 + col
                            val petId = prefs.slots.getOrNull(slotIdx)
                            val pet = prefs.findPet(petId)
                            val slotNumber = String.format("%02d", slotIdx + 1)

                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = PetColors.CardSurface,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(0.88f)
                                    .clip(RoundedCornerShape(18.dp))
                                    .border(
                                        width = 1.dp,
                                        color = if (pet != null) PetColors.ChampagneBorder else PetColors.HairlineBorder,
                                        shape = RoundedCornerShape(18.dp)
                                    )
                                    .clickable {
                                        if (pet != null) {
                                            onSlotFilledClick(pet.id)
                                        } else {
                                            onSlotEmptyClick(slotIdx)
                                        }
                                    }
                                    .testTag("roster_slot_${slotIdx + 1}")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = slotNumber,
                                        style = TabularNumberStyle.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (pet != null) PetColors.ChampagneGold else PetColors.SubtleNumbers
                                        ),
                                        modifier = Modifier.align(Alignment.TopStart)
                                    )

                                    if (pet != null) {
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Bottom
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(62.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(PetColors.StageBackground),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                PetSpriteFrame(
                                                    petId = pet.id,
                                                    frameIndex = 0,
                                                    facingRight = false,
                                                    modifier = Modifier.size(52.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = pet.name,
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = PetColors.ObsidianInk
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .align(Alignment.Center)
                                                .clip(CircleShape)
                                                .background(PetColors.CanvasBackground)
                                                .border(1.dp, PetColors.HairlineBorder, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Add,
                                                contentDescription = "Assign companion to slot $slotNumber",
                                                tint = PetColors.SecondaryMuted,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showCountPicker) {
        AlertDialog(
            onDismissRequest = { showCountPicker = false },
            containerColor = PetColors.CardSurface,
            title = {
                Text(
                    text = "Active Companions",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    COUNT_LABELS.forEachIndexed { idx, label ->
                        val count = idx + 1
                        val selected = prefs.numberOnScreen == count
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) PetColors.ChampagneTint else PetColors.CanvasBackground)
                                .border(
                                    1.dp,
                                    if (selected) PetColors.ChampagneGold else PetColors.HairlineBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    onSelectNumberOnScreen(count)
                                    showCountPicker = false
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            )
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Selected",
                                    tint = PetColors.ChampagneGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCountPicker = false }) {
                    Text("Close", color = PetColors.ObsidianInk)
                }
            }
        )
    }
}
