package com.petsonscreen.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petsonscreen.app.model.PetAppPreferences
import com.petsonscreen.app.ui.components.LivePetPlaygroundStage
import com.petsonscreen.app.ui.theme.CleanSansFamily
import com.petsonscreen.app.ui.theme.PetColors

@Composable
fun PetDetailScreen(
    petId: Int,
    prefs: PetAppPreferences,
    onBack: () -> Unit,
    onAddToHome: (Int) -> Boolean,
    onRemoveFromHome: (Int) -> Unit,
    onDeleteImported: (Int) -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    val pet = prefs.findPet(petId)
    if (pet == null) {
        onBack()
        return
    }

    val isInHome = prefs.slots.contains(pet.id)
    var showSlotsFullDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PetColors.CanvasBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.TopCenter)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Top App Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = PetColors.CardSurface,
                        modifier = Modifier
                            .size(44.dp)
                            .border(1.dp, PetColors.HairlineBorder, CircleShape)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("pet_detail_back")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = PetColors.ObsidianInk
                            )
                        }
                    }
                    Text(
                        text = pet.name,
                        style = MaterialTheme.typography.headlineMedium
                    )
                }

                if (!pet.isBuiltIn) {
                    Surface(
                        shape = CircleShape,
                        color = PetColors.DestructiveTint,
                        modifier = Modifier
                            .size(44.dp)
                            .border(1.dp, PetColors.DestructiveRed.copy(alpha = 0.35f), CircleShape)
                    ) {
                        IconButton(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier.testTag("pet_detail_delete")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "Delete imported pet",
                                tint = PetColors.DestructiveRed
                            )
                        }
                    }
                }
            }

            // Interactive 380dp Preview Stage
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(PetColors.StageBackground)
                    .border(1.dp, PetColors.HairlineBorder, RoundedCornerShape(24.dp))
                    .testTag("pet_detail_stage")
            ) {
                LivePetPlaygroundStage(
                    petIds = listOf(pet.id),
                    interactive = true,
                    speed = prefs.speed,
                    scale = prefs.size,
                    onNavigateHome = onNavigateHome,
                    onNavigateSettings = onNavigateSettings,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(14.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(PetColors.CardSurface.copy(alpha = 0.92f))
                        .border(1.dp, PetColors.HairlineBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "Drag, fling, or tap companion",
                        style = MaterialTheme.typography.labelSmall,
                        color = PetColors.SecondaryMuted
                    )
                }
            }

            // Metadata Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = PetColors.CardSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PetColors.HairlineBorder, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "${pet.category} · Companion",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isInHome) PetColors.ChampagneTint else PetColors.CanvasBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (isInHome) PetColors.ChampagneBorder else PetColors.HairlineBorder,
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = if (isInHome) PetColors.ChampagneGold else PetColors.SubtleNumbers,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isInHome) "In Home Roster" else "Not in Roster",
                                style = MaterialTheme.typography.labelMedium,
                                color = PetColors.ObsidianInk
                            )
                        }
                    }
                }
            }

            // Add / Remove Home CTA
            if (isInHome) {
                OutlinedButton(
                    onClick = { onRemoveFromHome(pet.id) },
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, PetColors.DestructiveRed.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = PetColors.DestructiveTint,
                        contentColor = PetColors.DestructiveRed
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("pet_detail_remove_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Remove from Home",
                        fontFamily = CleanSansFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            } else {
                Button(
                    onClick = {
                        val added = onAddToHome(pet.id)
                        if (!added) {
                            showSlotsFullDialog = true
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PetColors.ObsidianInk,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("pet_detail_add_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add to Home",
                        fontFamily = CleanSansFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showSlotsFullDialog) {
        AlertDialog(
            onDismissRequest = { showSlotsFullDialog = false },
            containerColor = PetColors.CardSurface,
            title = {
                Text("Companion Roster Full", style = MaterialTheme.typography.headlineSmall)
            },
            text = {
                Text(
                    "All 9 Home slots are currently filled. Remove a companion from Home first to add ${pet.name}.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showSlotsFullDialog = false }) {
                    Text("OK", color = PetColors.ObsidianInk)
                }
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = PetColors.CardSurface,
            title = {
                Text("Delete ${pet.name}?", style = MaterialTheme.typography.headlineSmall)
            },
            text = {
                Text(
                    "This will permanently remove the imported Shimeji pack and clear it from any Home slots.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteImported(pet.id)
                        onBack()
                    }
                ) {
                    Text("Delete", color = PetColors.DestructiveRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = PetColors.SecondaryMuted)
                }
            }
        )
    }
}
