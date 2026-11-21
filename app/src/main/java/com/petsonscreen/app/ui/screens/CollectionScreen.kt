package com.petsonscreen.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petsonscreen.app.model.BuiltInPets
import com.petsonscreen.app.model.PetAppPreferences
import com.petsonscreen.app.model.PetItem
import com.petsonscreen.app.ui.components.PetSpriteFrame
import com.petsonscreen.app.ui.theme.CleanSansFamily
import com.petsonscreen.app.ui.theme.PetColors
import com.petsonscreen.app.ui.theme.TabularNumberStyle

private const val PAGE_SIZE = 12

@Composable
fun CollectionScreen(
    prefs: PetAppPreferences,
    selectedCategory: String,
    currentPage: Int,
    targetSlotIndex: Int?,
    statusBanner: String?,
    onSelectCategory: (String) -> Unit,
    onPageChange: (Int) -> Unit,
    onDismissBanner: () -> Unit,
    onPetClick: (PetItem) -> Unit,
    onCancelSlotPick: () -> Unit,
    modifier: Modifier = Modifier
) {

    val categories = if (prefs.imported.isNotEmpty()) {
        BuiltInPets.categories + "My Pets"
    } else {
        BuiltInPets.categories
    }

    val filteredPets = if (selectedCategory == "All") {
        prefs.allPets
    } else {
        prefs.allPets.filter { it.category == selectedCategory }
    }

    val totalPages = ((filteredPets.size + PAGE_SIZE - 1) / PAGE_SIZE).coerceAtLeast(1)
    val safePage = currentPage.coerceIn(0, totalPages - 1)
    val pageItems = filteredPets.drop(safePage * PAGE_SIZE).take(PAGE_SIZE)
    val activeSlotIds = prefs.slots.filterNotNull().toSet()

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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (targetSlotIndex != null) {
                        IconButton(
                            onClick = onCancelSlotPick,
                            modifier = Modifier.testTag("collection_cancel_pick")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Cancel slot selection",
                                tint = PetColors.ObsidianInk
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = if (targetSlotIndex != null) "Choose Pet" else "Collection",
                            style = MaterialTheme.typography.displaySmall
                        )
                        val slotSuffix = if (targetSlotIndex != null) {
                            " · Slot ${String.format("%02d", targetSlotIndex + 1)}"
                        } else ""
                        Text(
                            text = "${filteredPets.size} companions · Page ${safePage + 1} of $totalPages$slotSuffix",
                            style = TabularNumberStyle.copy(
                                fontSize = 14.sp,
                                color = PetColors.SecondaryMuted
                            )
                        )
                    }
                }
            }

            if (statusBanner != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = PetColors.ObsidianInk,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onDismissBanner)
                ) {
                    Text(
                        text = statusBanner,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                    )
                }
            }

            // Segmented Category Filter Bar
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { category ->
                    val selected = selectedCategory == category
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selected) PetColors.ObsidianInk else PetColors.CardSurface)
                            .border(
                                width = 1.dp,
                                color = if (selected) PetColors.ObsidianInk else PetColors.HairlineBorder,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { onSelectCategory(category) }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .testTag("category_chip_${category.lowercase().replace(" ", "_")}")
                    ) {
                        Text(
                            text = category,
                            fontFamily = CleanSansFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = if (selected) Color.White else PetColors.SecondaryMuted
                        )
                    }
                }
            }

            // Paginated Grid (3 columns, up to 12 per page)
            val rows = pageItems.chunked(3)
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rows.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { pet ->
                            val inHome = pet.id in activeSlotIds
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = PetColors.CardSurface,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(0.84f)
                                    .clip(RoundedCornerShape(18.dp))
                                    .border(
                                        width = if (inHome) 1.5.dp else 1.dp,
                                        color = if (inHome) PetColors.ChampagneGold else PetColors.HairlineBorder,
                                        shape = RoundedCornerShape(18.dp)
                                    )
                                    .clickable { onPetClick(pet) }
                                    .testTag("collection_pet_${pet.id}")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(10.dp)
                                ) {
                                    if (inHome) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .align(Alignment.TopEnd)
                                                .clip(CircleShape)
                                                .background(PetColors.ChampagneGold),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = "In Home Roster",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Bottom
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(PetColors.StageBackground),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            PetSpriteFrame(
                                                petId = pet.id,
                                                frameIndex = 0,
                                                facingRight = false,
                                                modifier = Modifier.size(54.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = pet.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = pet.category,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                        repeat(3 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Pagination Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = { onPageChange(safePage - 1) },
                    enabled = safePage > 0,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, PetColors.HairlineBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = PetColors.CardSurface,
                        contentColor = PetColors.ObsidianInk
                    ),
                    modifier = Modifier.testTag("collection_prev_page")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous page",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Prev")
                }

                Text(
                    text = "Page ${safePage + 1} of $totalPages",
                    style = TabularNumberStyle.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = PetColors.SecondaryMuted
                    )
                )

                OutlinedButton(
                    onClick = { onPageChange(safePage + 1) },
                    enabled = safePage < totalPages - 1,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, PetColors.HairlineBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = PetColors.CardSurface,
                        contentColor = PetColors.ObsidianInk
                    ),
                    modifier = Modifier.testTag("collection_next_page")
                ) {
                    Text("Next")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next page",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
