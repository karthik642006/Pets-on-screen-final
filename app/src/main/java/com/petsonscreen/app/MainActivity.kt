package com.petsonscreen.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.petsonscreen.app.model.PetAppPreferences
import com.petsonscreen.app.service.PetOverlayService
import com.petsonscreen.app.ui.components.InAppPetOverlay
import com.petsonscreen.app.ui.screens.CollectionScreen
import com.petsonscreen.app.ui.screens.HomeScreen
import com.petsonscreen.app.ui.screens.IntroCarouselScreen
import com.petsonscreen.app.ui.screens.PetDetailScreen
import com.petsonscreen.app.ui.screens.SettingsScreen
import com.petsonscreen.app.ui.screens.SplashScreen
import com.petsonscreen.app.ui.theme.CleanSansFamily
import com.petsonscreen.app.ui.theme.PetColors
import com.petsonscreen.app.ui.theme.PetsOnScreenTheme
import com.petsonscreen.app.viewmodel.MainViewModel
import com.petsonscreen.app.viewmodel.UiNavCommand
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.random.Random

object Routes {
    const val SPLASH = "splash"
    const val INTRO = "intro"
    const val HOME = "home"
    const val COLLECTION = "collection"
    const val SETTINGS = "settings"
    const val PET_DETAIL = "pet_detail/{petId}"
    fun petDetail(petId: Int) = "pet_detail/$petId"
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val pendingTabFromIntent = mutableStateOf<String?>(null)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshOverlayPermission()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleTargetTabIntent(intent)

        setContent {
            PetsOnScreenTheme {
                PetsOnScreenApp(
                    viewModel = viewModel,
                    pendingTab = pendingTabFromIntent.value,
                    onConsumePendingTab = { pendingTabFromIntent.value = null },
                    onRequestPostNotifications = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val granted = ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                            if (!granted) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    },
                    onOpenOverlaySettings = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                        startActivity(intent)
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleTargetTabIntent(intent)
    }

    private fun handleTargetTabIntent(intent: Intent?) {
        val target = intent?.getStringExtra(EXTRA_TARGET_TAB)
        if (target == TAB_HOME || target == TAB_SETTINGS) {
            pendingTabFromIntent.value = target
        }
    }

    companion object {
        const val EXTRA_TARGET_TAB = "extra_target_tab"
        const val TAB_HOME = "home"
        const val TAB_SETTINGS = "settings"
    }
}

@Composable
private fun PetsOnScreenApp(
    viewModel: MainViewModel,
    pendingTab: String?,
    onConsumePendingTab: () -> Unit,
    onRequestPostNotifications: () -> Unit,
    onOpenOverlaySettings: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = rememberNavController()

    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val commandEvent by viewModel.commandEvents.collectAsStateWithLifecycle()
    val canDrawOverlays by viewModel.canDrawOverlays.collectAsStateWithLifecycle()
    val showOverlayDialog by viewModel.showOverlayPermissionDialog.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val collectionPage by viewModel.collectionPage.collectAsStateWithLifecycle()
    val targetSlotIndex by viewModel.targetSlotIndex.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    // TextToSpeech engine for Companion Command spoken replies
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsReady by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val engine = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
            }
        }
        engine.language = Locale.getDefault()
        ttsEngine = engine
        onDispose {
            runCatching {
                engine.stop()
                engine.shutdown()
            }
        }
    }

    LaunchedEffect(ttsReady) {
        viewModel.ttsSpeechEvents.collect { reply ->
            if (ttsReady) {
                ttsEngine?.speak(reply, TextToSpeech.QUEUE_FLUSH, null, "pet_cmd_${System.currentTimeMillis()}")
            }
        }
    }

    // Refresh overlay permission & service sync on resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshOverlayPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Handle navigation events from Companion Commands
    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { cmd ->
            val route = when (cmd) {
                UiNavCommand.OpenHome -> Routes.HOME
                UiNavCommand.OpenCollection -> Routes.COLLECTION
                UiNavCommand.OpenSettings -> Routes.SETTINGS
            }
            navController.navigate(route) {
                popUpTo(Routes.HOME) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    // Handle quick-menu deep links from PetOverlayService
    LaunchedEffect(pendingTab, prefs.onboarded) {
        if (pendingTab != null && prefs.onboarded) {
            val route = if (pendingTab == MainActivity.TAB_SETTINGS) Routes.SETTINGS else Routes.HOME
            navController.navigate(route) {
                popUpTo(Routes.HOME) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
            onConsumePendingTab()
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showMainNav = currentRoute in setOf(Routes.HOME, Routes.COLLECTION, Routes.SETTINGS)

    val isWideLayout = LocalConfiguration.current.screenWidthDp >= 700

    fun navigateToTab(route: String) {
        if (route != Routes.COLLECTION) {
            viewModel.clearSlotSelection()
        }
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    // In-app overlay pets when enabled & not hidden & system overlay isn't active
    val inAppPetIds = rememberInAppActivePetIds(prefs = prefs)
    val showInAppOverlay = prefs.onboarded &&
        prefs.enabled &&
        !prefs.petsHidden &&
        !canDrawOverlays &&
        currentRoute !in setOf(Routes.SPLASH, Routes.INTRO) &&
        !(currentRoute?.startsWith("pet_detail") == true)

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = PetColors.CanvasBackground,
            contentWindowInsets = WindowInsets.navigationBars,
            bottomBar = {
                if (showMainNav && !isWideLayout) {
                    EditorialBottomBar(
                        currentRoute = currentRoute,
                        onNavigate = ::navigateToTab
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showMainNav && isWideLayout) {
                    EditorialNavigationRail(
                        currentRoute = currentRoute,
                        onNavigate = ::navigateToTab
                    )
                }

                NavHost(
                    navController = navController,
                    startDestination = Routes.SPLASH,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(Routes.SPLASH) {
                        SplashScreen(
                            onFinished = {
                                val next = if (prefs.onboarded) Routes.HOME else Routes.INTRO
                                navController.navigate(next) {
                                    popUpTo(Routes.SPLASH) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Routes.INTRO) {
                        IntroCarouselScreen(
                            onFinishIntro = {
                                onRequestPostNotifications()
                                viewModel.completeOnboarding(null)
                                navController.navigate(Routes.HOME) {
                                    popUpTo(Routes.INTRO) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Routes.HOME) {
                        HomeScreen(
                            prefs = prefs,
                            canDrawOverlays = canDrawOverlays,
                            onToggleEnabled = { enable ->
                                if (enable) onRequestPostNotifications()
                                viewModel.setPetsEnabled(enable)
                            },
                            onSelectNumberOnScreen = viewModel::setNumberOnScreen,
                            onTogglePetsHidden = viewModel::togglePetsHidden,
                            onSlotEmptyClick = { slotIndex ->
                                viewModel.beginSlotSelection(slotIndex)
                                navigateToTab(Routes.COLLECTION)
                            },
                            onSlotFilledClick = { petId ->
                                navController.navigate(Routes.petDetail(petId))
                            },
                            onRequestOverlayPermission = onOpenOverlaySettings
                        )
                    }

                    composable(Routes.COLLECTION) {
                        CollectionScreen(
                            prefs = prefs,
                            selectedCategory = selectedCategory,
                            currentPage = collectionPage,
                            targetSlotIndex = targetSlotIndex,
                            statusBanner = statusBanner,
                            onSelectCategory = viewModel::setCategory,
                            onPageChange = viewModel::setCollectionPage,
                            onDismissBanner = viewModel::clearStatusBanner,
                            onPetClick = { pet ->
                                if (targetSlotIndex != null) {
                                    viewModel.assignPetToTargetSlot(pet.id)
                                    navigateToTab(Routes.HOME)
                                } else {
                                    navController.navigate(Routes.petDetail(pet.id))
                                }
                            },
                            onCancelSlotPick = {
                                viewModel.clearSlotSelection()
                                navigateToTab(Routes.HOME)
                            }
                        )
                    }

                    composable(
                        route = Routes.PET_DETAIL,
                        arguments = listOf(navArgument("petId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val petId = backStackEntry.arguments?.getInt("petId") ?: 1
                        PetDetailScreen(
                            petId = petId,
                            prefs = prefs,
                            onBack = { navController.popBackStack() },
                            onAddToHome = viewModel::addPetToHome,
                            onRemoveFromHome = viewModel::removePetFromHome,
                            onDeleteImported = viewModel::deleteImportedPet,
                            onNavigateHome = {
                                navController.popBackStack(Routes.HOME, inclusive = false)
                            },
                            onNavigateSettings = {
                                navController.navigate(Routes.SETTINGS) {
                                    popUpTo(Routes.HOME) { saveState = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable(Routes.SETTINGS) {
                        SettingsScreen(
                            prefs = prefs,
                            onSpeedChange = viewModel::setSpeed,
                            onSizeChange = viewModel::setSize,
                            onTimeChange = viewModel::setTimeChange,
                            onSendCommand = viewModel::processCommand,
                            onClearConversation = viewModel::clearConversation
                        )
                    }
                }
            }
        }

        // In-app fallback overlay when system overlay permission has not yet been granted
        if (showInAppOverlay) {
            InAppPetOverlay(
                petIds = inAppPetIds,
                speed = prefs.speed,
                scale = prefs.size,
                commandEvent = commandEvent,
                onNavigateHome = { navigateToTab(Routes.HOME) },
                onNavigateSettings = { navigateToTab(Routes.SETTINGS) },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    if (showOverlayDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissOverlayPermissionDialog,
            containerColor = PetColors.CardSurface,
            shape = RoundedCornerShape(22.dp),
            title = {
                Text(
                    text = "Allow Floating Companions",
                    color = PetColors.ObsidianInk
                )
            },
            text = {
                Text(
                    text = "To let your Shimeji companions roam over other apps and your home screen, enable \"Display over other apps\" for Pets On Screen. Until then, they will roam inside the app.",
                    color = PetColors.SecondaryMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissOverlayPermissionDialog()
                        onOpenOverlaySettings()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PetColors.ObsidianInk,
                        contentColor = Color.White
                    )
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissOverlayPermissionDialog) {
                    Text("Stay In-App", color = PetColors.SecondaryMuted)
                }
            }
        )
    }
}

@Composable
private fun rememberInAppActivePetIds(prefs: PetAppPreferences): List<Int> {
    var activeIds by remember { mutableStateOf<List<Int>>(emptyList()) }
    val filled = prefs.slots.filterNotNull()
    val targetCount = minOf(prefs.numberOnScreen, filled.size)

    LaunchedEffect(prefs.enabled, prefs.petsHidden, prefs.slots, prefs.numberOnScreen, prefs.timeChange) {
        if (!prefs.enabled || prefs.petsHidden || targetCount == 0) {
            activeIds = emptyList()
            return@LaunchedEffect
        }
        while (true) {
            activeIds = filled.shuffled(Random.Default).take(targetCount)
            delay(prefs.timeChange.coerceIn(1, 60) * 60_000L)
        }
    }
    return activeIds
}

@Composable
private fun EditorialBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    Box(modifier = Modifier.background(PetColors.CardSurface)) {
        HorizontalDivider(color = PetColors.HairlineBorder)
        NavigationBar(
            containerColor = PetColors.CardSurface,
            tonalElevation = 0.dp
        ) {
            val items = listOf(
                Triple(Routes.HOME, "Home", Pair(Icons.Filled.Home, Icons.Outlined.Home)),
                Triple(Routes.COLLECTION, "Collection", Pair(Icons.Filled.GridView, Icons.Outlined.GridView)),
                Triple(Routes.SETTINGS, "Settings", Pair(Icons.Filled.Settings, Icons.Outlined.Settings))
            )
            items.forEach { (route, label, icons) ->
                val selected = currentRoute == route
                NavigationBarItem(
                    selected = selected,
                    onClick = { onNavigate(route) },
                    icon = {
                        Icon(
                            imageVector = if (selected) icons.first else icons.second,
                            contentDescription = label
                        )
                    },
                    label = {
                        Text(
                            text = label,
                            fontFamily = CleanSansFamily,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PetColors.ChampagneGold,
                        selectedTextColor = PetColors.ObsidianInk,
                        indicatorColor = PetColors.ChampagneTint,
                        unselectedIconColor = PetColors.SecondaryMuted,
                        unselectedTextColor = PetColors.SecondaryMuted
                    ),
                    modifier = Modifier.testTag("nav_$route")
                )
            }
        }
    }
}

@Composable
private fun EditorialNavigationRail(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    Row(modifier = Modifier.fillMaxHeight()) {
        NavigationRail(
            containerColor = PetColors.CardSurface,
            modifier = Modifier.widthIn(min = 80.dp)
        ) {
            val items = listOf(
                Triple(Routes.HOME, "Home", Pair(Icons.Filled.Home, Icons.Outlined.Home)),
                Triple(Routes.COLLECTION, "Collection", Pair(Icons.Filled.GridView, Icons.Outlined.GridView)),
                Triple(Routes.SETTINGS, "Settings", Pair(Icons.Filled.Settings, Icons.Outlined.Settings))
            )
            items.forEach { (route, label, icons) ->
                val selected = currentRoute == route
                NavigationRailItem(
                    selected = selected,
                    onClick = { onNavigate(route) },
                    icon = {
                        Icon(
                            imageVector = if (selected) icons.first else icons.second,
                            contentDescription = label
                        )
                    },
                    label = {
                        Text(
                            text = label,
                            fontFamily = CleanSansFamily,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                        )
                    },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = PetColors.ChampagneGold,
                        selectedTextColor = PetColors.ObsidianInk,
                        indicatorColor = PetColors.ChampagneTint,
                        unselectedIconColor = PetColors.SecondaryMuted,
                        unselectedTextColor = PetColors.SecondaryMuted
                    ),
                    modifier = Modifier.testTag("nav_rail_$route")
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.fillMaxHeight().widthIn(max = 1.dp),
            color = PetColors.HairlineBorder
        )
    }
}
