package com.petsonscreen.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.petsonscreen.app.model.PetAppPreferences
import com.petsonscreen.app.ui.theme.CleanSansFamily
import com.petsonscreen.app.ui.theme.PetColors
import com.petsonscreen.app.ui.theme.TabularNumberStyle
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    prefs: PetAppPreferences,
    onSpeedChange: (Float) -> Unit,
    onSizeChange: (Float) -> Unit,
    onTimeChange: (Int) -> Unit,
    onSendCommand: (String) -> Unit,
    onClearConversation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var commandText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var speechError by remember { mutableStateOf<String?>(null) }

    val speechRecognizer = remember(context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            null
        }
    }

    DisposableEffect(speechRecognizer) {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                speechError = null
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isListening = false
            }

            override fun onError(error: Int) {
                isListening = false
                speechError = "Could not capture voice command. Please try typing or speaking again."
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val spoken = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                if (!spoken.isNullOrEmpty()) {
                    commandText = ""
                    onSendCommand(spoken)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        onDispose {
            runCatching { speechRecognizer?.destroy() }
        }
    }

    fun startVoiceListening() {
        val recognizer = speechRecognizer
        if (recognizer == null) {
            speechError = "Speech recognition is unavailable on this device."
            return
        }
        speechError = null
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        isListening = true
        recognizer.startListening(intent)
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startVoiceListening()
        } else {
            speechError = "Microphone permission is needed for voice commands."
        }
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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.displaySmall
                )
                Text(
                    text = "Tune companion physics and issue live commands",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Motion & Display Card
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = PetColors.CardSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PetColors.HairlineBorder, RoundedCornerShape(22.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PetColors.ChampagneTint)
                                .border(1.dp, PetColors.ChampagneBorder, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Tune,
                                contentDescription = null,
                                tint = PetColors.ChampagneGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "Motion & Display",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    // Movement Speed (1.0..5.0, step 0.1)
                    EditorialSliderRow(
                        label = "Movement Speed",
                        valueLabel = String.format("%.1f×", prefs.speed),
                        value = prefs.speed,
                        valueRange = 1.0f..5.0f,
                        steps = 39,
                        onValueChange = { raw ->
                            val snapped = (raw * 10f).roundToInt() / 10f
                            onSpeedChange(snapped.coerceIn(1.0f, 5.0f))
                        },
                        testTag = "slider_speed"
                    )

                    HorizontalDivider(color = PetColors.HairlineBorder)

                    // Companion Scale (1.0..2.6, step 0.1)
                    EditorialSliderRow(
                        label = "Companion Scale",
                        valueLabel = String.format("%.1f×", prefs.size),
                        value = prefs.size,
                        valueRange = 1.0f..2.6f,
                        steps = 15,
                        onValueChange = { raw ->
                            val snapped = (raw * 10f).roundToInt() / 10f
                            onSizeChange(snapped.coerceIn(1.0f, 2.6f))
                        },
                        testTag = "slider_size"
                    )

                    HorizontalDivider(color = PetColors.HairlineBorder)

                    // Rotation Interval (1..60 min, step 1)
                    EditorialSliderRow(
                        label = "Rotation Interval",
                        valueLabel = "${prefs.timeChange} min",
                        value = prefs.timeChange.toFloat(),
                        valueRange = 1f..60f,
                        steps = 58,
                        onValueChange = { raw ->
                            onTimeChange(raw.roundToInt().coerceIn(1, 60))
                        },
                        testTag = "slider_rotation"
                    )
                }
            }

            // Companion Commands Card
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = PetColors.CardSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PetColors.HairlineBorder, RoundedCornerShape(22.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Companion Commands",
                            style = MaterialTheme.typography.titleLarge
                        )

                        if (prefs.conversation.isNotEmpty()) {
                            TextButton(
                                onClick = onClearConversation,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("settings_clear_history")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteSweep,
                                    contentDescription = null,
                                    tint = PetColors.SecondaryMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Clear",
                                    color = PetColors.SecondaryMuted,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = commandText,
                        onValueChange = { commandText = it },
                        placeholder = {
                            Text(
                                text = "Try: jump, sit, walk, hide, or show",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PetColors.SubtleNumbers
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PetColors.ChampagneGold,
                            unfocusedBorderColor = PetColors.HairlineBorder,
                            focusedContainerColor = PetColors.CanvasBackground,
                            unfocusedContainerColor = PetColors.CanvasBackground,
                            cursorColor = PetColors.ObsidianInk
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (commandText.isNotBlank()) {
                                    onSendCommand(commandText)
                                    commandText = ""
                                }
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_command_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (isListening) {
                                    speechRecognizer?.stopListening()
                                    isListening = false
                                } else {
                                    val granted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (granted) {
                                        startVoiceListening()
                                    } else {
                                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isListening) PetColors.ChampagneGold else PetColors.HairlineBorder
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isListening) PetColors.ChampagneTint else PetColors.CanvasBackground,
                                contentColor = PetColors.ObsidianInk
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("settings_talk_button")
                        ) {
                            Icon(
                                imageVector = if (isListening) Icons.Filled.GraphicEq else Icons.Filled.Mic,
                                contentDescription = "Talk",
                                tint = if (isListening) PetColors.ChampagneGold else PetColors.ObsidianInk,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isListening) "Listening..." else "Talk",
                                fontFamily = CleanSansFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }

                        Button(
                            onClick = {
                                if (commandText.isNotBlank()) {
                                    onSendCommand(commandText)
                                    commandText = ""
                                }
                            },
                            enabled = commandText.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PetColors.ObsidianInk,
                                contentColor = Color.White,
                                disabledContainerColor = PetColors.StageBackground,
                                disabledContentColor = PetColors.SubtleNumbers
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("settings_send_button")
                        ) {
                            Text(
                                text = "Send",
                                fontFamily = CleanSansFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (speechError != null) {
                        Text(
                            text = speechError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = PetColors.DestructiveRed
                        )
                    }

                    if (prefs.conversation.isNotEmpty()) {
                        HorizontalDivider(color = PetColors.HairlineBorder)
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            prefs.conversation.forEach { turn ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (turn.isUser) Arrangement.End else Arrangement.Start
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(
                                            topStart = 16.dp,
                                            topEnd = 16.dp,
                                            bottomStart = if (turn.isUser) 16.dp else 4.dp,
                                            bottomEnd = if (turn.isUser) 4.dp else 16.dp
                                        ),
                                        color = if (turn.isUser) PetColors.ObsidianInk else PetColors.ChampagneTint,
                                        modifier = Modifier
                                            .widthIn(max = 280.dp)
                                            .then(
                                                if (!turn.isUser) {
                                                    Modifier.border(
                                                        1.dp,
                                                        PetColors.ChampagneBorder,
                                                        RoundedCornerShape(
                                                            topStart = 16.dp,
                                                            topEnd = 16.dp,
                                                            bottomStart = 4.dp,
                                                            bottomEnd = 16.dp
                                                        )
                                                    )
                                                } else Modifier
                                            )
                                    ) {
                                        Text(
                                            text = turn.text,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (turn.isUser) Color.White else PetColors.ObsidianInk,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                        )
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
}

@Composable
private fun EditorialSliderRow(
    label: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    testTag: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(PetColors.ChampagneTint)
                    .border(1.dp, PetColors.ChampagneBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = valueLabel,
                    style = TabularNumberStyle.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PetColors.ObsidianInk
                    )
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = PetColors.ChampagneGold,
                activeTrackColor = PetColors.ChampagneGold,
                inactiveTrackColor = PetColors.StageBackground,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )
    }
}
