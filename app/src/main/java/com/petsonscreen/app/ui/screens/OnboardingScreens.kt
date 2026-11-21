package com.petsonscreen.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.petsonscreen.app.ui.components.LivePetPlaygroundStage
import com.petsonscreen.app.ui.theme.CleanSansFamily
import com.petsonscreen.app.ui.theme.EditorialSerifFamily
import com.petsonscreen.app.ui.theme.PetColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onFinished: () -> Unit
) {
    var started by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (started) 1f else 0.08f,
        animationSpec = tween(durationMillis = 1100),
        label = "splash_progress"
    )

    LaunchedEffect(Unit) {
        started = true
        delay(1200L)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PetColors.CanvasBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.widthIn(max = 360.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = PetColors.CardSurface,
                modifier = Modifier
                    .size(136.dp)
                    .border(1.dp, PetColors.HairlineBorder, RoundedCornerShape(28.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF010101)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo_custom),
                        contentDescription = "Pets On Screen Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Pets On Screen",
                fontFamily = EditorialSerifFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 32.sp,
                color = PetColors.ObsidianInk,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Desktop Companions",
                fontFamily = CleanSansFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                letterSpacing = 1.2.sp,
                color = PetColors.SecondaryMuted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .width(180.dp)
                    .height(4.dp)
                    .clip(CircleShape),
                color = PetColors.ChampagneGold,
                trackColor = PetColors.HairlineBorder,
                strokeCap = StrokeCap.Round
            )
        }
    }
}

private data class IntroPageData(
    val title: String,
    val description: String,
    val petIds: List<Int>,
    val badge: String
)

private val INTRO_PAGES = listOf(
    IntroPageData(
        title = "Welcome to Pets On Screen",
        description = "Animated companions stay with you while you work, browse, or relax.",
        petIds = listOf(1),
        badge = "01 / COMPANION STAGE"
    ),
    IntroPageData(
        title = "Curate Your Roster",
        description = "Choose from charming companions for your personal 9-slot roster.",
        petIds = listOf(3, 4),
        badge = "02 / 9-SLOT ROSTER"
    ),
    IntroPageData(
        title = "Ready to Play",
        description = "Drag, toss, or send commands to your companions anytime.",
        petIds = listOf(5, 6, 7),
        badge = "03 / LIVE PHYSICS"
    )
)

@Composable
fun IntroCarouselScreen(
    onFinishIntro: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { INTRO_PAGES.size })
    val scope = rememberCoroutineScope()

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
                .widthIn(max = 560.dp)
                .align(Alignment.Center)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onFinishIntro,
                    modifier = Modifier.testTag("intro_skip_button")
                ) {
                    Text(
                        text = "Skip",
                        color = PetColors.SecondaryMuted,
                        fontFamily = CleanSansFamily,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                val data = INTRO_PAGES[page]
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(290.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(PetColors.StageBackground)
                            .border(1.dp, PetColors.HairlineBorder, RoundedCornerShape(24.dp))
                    ) {
                        LivePetPlaygroundStage(
                            petIds = data.petIds,
                            interactive = false,
                            speed = 1.4f,
                            scale = 1.45f,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(14.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(PetColors.CardSurface.copy(alpha = 0.92f))
                                .border(1.dp, PetColors.HairlineBorder, RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = data.badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = PetColors.ChampagneGold,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = data.title,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = data.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = PetColors.SecondaryMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(INTRO_PAGES.size) { index ->
                    val selected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(if (selected) 24.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (selected) PetColors.ChampagneGold else PetColors.HairlineBorder)
                    )
                }
            }

            val isLast = pagerState.currentPage == INTRO_PAGES.lastIndex
            Button(
                onClick = {
                    if (isLast) {
                        onFinishIntro()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PetColors.ObsidianInk,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("intro_next_button")
            ) {
                Text(
                    text = if (isLast) "Start" else "Next",
                    fontFamily = CleanSansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
