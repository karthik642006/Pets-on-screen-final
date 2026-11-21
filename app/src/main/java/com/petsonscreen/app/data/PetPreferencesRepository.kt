package com.petsonscreen.app.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.provider.OpenableColumns
import com.petsonscreen.app.model.BuiltInPets
import com.petsonscreen.app.model.CommandEvent
import com.petsonscreen.app.model.ConversationTurn
import com.petsonscreen.app.model.PetAppPreferences
import com.petsonscreen.app.model.PetCommandAction
import com.petsonscreen.app.model.PetItem
import com.petsonscreen.app.sprites.PetSpriteCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

class PetPreferencesRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(readSnapshot())
    val state: StateFlow<PetAppPreferences> = _state.asStateFlow()

    private val _commandEvents = MutableStateFlow<CommandEvent?>(null)
    val commandEvents: StateFlow<CommandEvent?> = _commandEvents.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        _state.value = readSnapshot()
        val seq = prefs.getLong(KEY_CMD_SEQ, 0L)
        val cmdName = prefs.getString(KEY_CMD_ACTION, null)
        if (seq > 0L && cmdName != null) {
            runCatching { PetCommandAction.valueOf(cmdName) }.getOrNull()?.let { action ->
                _commandEvents.value = CommandEvent(seq, action)
            }
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun reload() {
        _state.value = readSnapshot()
    }

    private fun readSnapshot(): PetAppPreferences {
        val slots = parseSlots(prefs.getString(KEY_SLOTS, null))
        val imported = parseImported(prefs.getString(KEY_IMPORTED, null))
        val conversation = parseConversation(prefs.getString(KEY_CONVERSATION, null))
        return PetAppPreferences(
            onboarded = prefs.getBoolean(KEY_ONBOARDED, false),
            enabled = prefs.getBoolean(KEY_ENABLED, false),
            petsHidden = prefs.getBoolean(KEY_PETS_HIDDEN, false),
            numberOnScreen = prefs.getInt(KEY_NUMBER_ON_SCREEN, 1).coerceIn(1, 6),
            size = prefs.getFloat(KEY_SIZE, 1.6f).coerceIn(1.0f, 2.6f),
            speed = prefs.getFloat(KEY_SPEED, 1.5f).coerceIn(1.0f, 5.0f),
            timeChange = prefs.getInt(KEY_TIME_CHANGE, 5).coerceIn(1, 60),
            slots = slots,
            imported = imported,
            conversation = conversation,
            surveyAnswer = prefs.getString(KEY_SURVEY_ANSWER, null)
        )
    }

    fun completeOnboarding(surveyAnswer: String?, enablePets: Boolean = true) {
        prefs.edit()
            .putBoolean(KEY_ONBOARDED, true)
            .putBoolean(KEY_ENABLED, enablePets)
            .putBoolean(KEY_PETS_HIDDEN, false)
            .apply {
                if (surveyAnswer != null) putString(KEY_SURVEY_ANSWER, surveyAnswer)
            }
            .apply()
        reload()
    }

    fun setEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply {
                if (enabled) putBoolean(KEY_PETS_HIDDEN, false)
            }
            .apply()
        reload()
    }

    fun setPetsHidden(hidden: Boolean) {
        prefs.edit().putBoolean(KEY_PETS_HIDDEN, hidden).apply()
        reload()
    }

    fun setNumberOnScreen(count: Int) {
        prefs.edit().putInt(KEY_NUMBER_ON_SCREEN, count.coerceIn(1, 6)).apply()
        reload()
    }

    fun setSize(size: Float) {
        val snapped = ((size * 10f).toInt() / 10f).coerceIn(1.0f, 2.6f)
        prefs.edit().putFloat(KEY_SIZE, snapped).apply()
        reload()
    }

    fun setSpeed(speed: Float) {
        val snapped = ((speed * 10f).toInt() / 10f).coerceIn(1.0f, 5.0f)
        prefs.edit().putFloat(KEY_SPEED, snapped).apply()
        reload()
    }

    fun setTimeChange(minutes: Int) {
        prefs.edit().putInt(KEY_TIME_CHANGE, minutes.coerceIn(1, 60)).apply()
        reload()
    }

    fun setSlot(slotIndex: Int, petId: Int?) {
        if (slotIndex !in 0 until 9) return
        val current = _state.value.slots.toMutableList()
        current[slotIndex] = petId
        prefs.edit().putString(KEY_SLOTS, encodeSlots(current)).apply()
        reload()
    }

    fun addToFirstEmptySlot(petId: Int): Boolean {
        val current = _state.value.slots.toMutableList()
        val emptyIdx = current.indexOfFirst { it == null }
        if (emptyIdx < 0) return false
        current[emptyIdx] = petId
        prefs.edit().putString(KEY_SLOTS, encodeSlots(current)).apply()
        reload()
        return true
    }

    fun removeFromAllSlots(petId: Int) {
        val current = _state.value.slots.map { if (it == petId) null else it }
        prefs.edit().putString(KEY_SLOTS, encodeSlots(current)).apply()
        reload()
    }

    fun dispatchPetCommand(action: PetCommandAction) {
        val seq = prefs.getLong(KEY_CMD_SEQ, 0L) + 1L
        prefs.edit()
            .putLong(KEY_CMD_SEQ, seq)
            .putString(KEY_CMD_ACTION, action.name)
            .apply()
        _commandEvents.value = CommandEvent(seq, action)
    }

    fun appendConversation(userText: String, replyText: String) {
        val now = System.currentTimeMillis()
        val updated = (_state.value.conversation + listOf(
            ConversationTurn(id = now, isUser = true, text = userText, timestamp = now),
            ConversationTurn(id = now + 1, isUser = false, text = replyText, timestamp = now + 1)
        )).takeLast(20)
        prefs.edit().putString(KEY_CONVERSATION, encodeConversation(updated)).apply()
        reload()
    }

    fun clearConversation() {
        prefs.edit().remove(KEY_CONVERSATION).apply()
        reload()
    }

    suspend fun importShimejiZip(uri: Uri): Result<PetItem> = withContext(Dispatchers.IO) {
        runCatching {
            val currentImported = _state.value.imported
            val nextId = (currentImported.maxOfOrNull { it.id } ?: 1000) + 1
            val rawName = resolveDisplayName(uri)
                ?.substringBeforeLast('.')
                ?.replace('_', ' ')
                ?.replace('-', ' ')
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.replaceFirstChar { it.uppercase() }
                ?: "Custom Pet #$nextId"

            val targetDir = File(appContext.filesDir, "pets/data_$nextId")
            if (targetDir.exists()) targetDir.deleteRecursively()
            targetDir.mkdirs()

            val extractedFrames = mutableMapOf<Int, ByteArray>()
            var firstFallbackPng: ByteArray? = null

            appContext.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input).use { zis ->
                    var entry = zis.nextEntry
                    val buffer = ByteArray(8192)
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            val fileName = entry.name.substringAfterLast('/').substringAfterLast('\\')
                            val normalized = fileName.lowercase()
                                .replace(" ", "")
                                .replace("_", "")
                            if (normalized.endsWith(".png")) {
                                val out = ByteArrayOutputStream()
                                var read: Int
                                while (zis.read(buffer).also { read = it } != -1) {
                                    out.write(buffer, 0, read)
                                }
                                val bytes = out.toByteArray()
                                if (bytes.isNotEmpty()) {
                                    if (firstFallbackPng == null) firstFallbackPng = bytes
                                    val match = SHIME_REGEX.matchEntire(normalized)
                                    if (match != null) {
                                        val frameNum = match.groupValues[1].toIntOrNull()
                                        if (frameNum != null && frameNum in 1..46) {
                                            extractedFrames[frameNum] = bytes
                                        }
                                    }
                                }
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            } ?: throw IllegalStateException("Could not open selected ZIP file.")

            val baseFrame = extractedFrames[1] ?: firstFallbackPng
                ?: throw IllegalStateException("No shime1.png–shime46.png frames found in the ZIP archive.")

            for (frameNum in 1..46) {
                val data = extractedFrames[frameNum] ?: baseFrame
                FileOutputStream(File(targetDir, "shime$frameNum.png")).use { fos ->
                    fos.write(data)
                }
            }

            val newPet = PetItem(
                id = nextId,
                name = rawName,
                category = "My Pets",
                isBuiltIn = false
            )
            val updatedList = currentImported + newPet
            prefs.edit().putString(KEY_IMPORTED, encodeImported(updatedList)).apply()
            PetSpriteCache.clearPet(nextId)
            withContext(Dispatchers.Main) { reload() }
            newPet
        }
    }

    fun deleteImportedPet(petId: Int) {
        if (petId < 1001) return
        val updatedImported = _state.value.imported.filterNot { it.id == petId }
        val updatedSlots = _state.value.slots.map { if (it == petId) null else it }
        prefs.edit()
            .putString(KEY_IMPORTED, encodeImported(updatedImported))
            .putString(KEY_SLOTS, encodeSlots(updatedSlots))
            .apply()
        File(appContext.filesDir, "pets/data_$petId").deleteRecursively()
        PetSpriteCache.clearPet(petId)
        reload()
    }

    private fun resolveDisplayName(uri: Uri): String? {
        appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) return cursor.getString(idx)
                }
            }
        return uri.lastPathSegment
    }

    private fun parseSlots(raw: String?): List<Int?> {
        if (raw.isNullOrBlank()) return DEFAULT_SLOTS
        return runCatching {
            val arr = JSONArray(raw)
            List(9) { i ->
                if (i < arr.length() && !arr.isNull(i)) arr.getInt(i) else null
            }
        }.getOrDefault(DEFAULT_SLOTS)
    }

    private fun encodeSlots(slots: List<Int?>): String {
        val arr = JSONArray()
        for (i in 0 until 9) {
            val v = slots.getOrNull(i)
            if (v == null) arr.put(JSONObject.NULL) else arr.put(v)
        }
        return arr.toString()
    }

    private fun parseImported(raw: String?): List<PetItem> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        PetItem(
                            id = obj.getInt("id"),
                            name = obj.getString("name"),
                            category = obj.optString("category", "My Pets"),
                            isBuiltIn = false
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeImported(items: List<PetItem>): String {
        val arr = JSONArray()
        items.forEach { pet ->
            arr.put(JSONObject().apply {
                put("id", pet.id)
                put("name", pet.name)
                put("category", pet.category)
            })
        }
        return arr.toString()
    }

    private fun parseConversation(raw: String?): List<ConversationTurn> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        ConversationTurn(
                            id = obj.getLong("id"),
                            isUser = obj.getBoolean("isUser"),
                            text = obj.getString("text"),
                            timestamp = obj.getLong("timestamp")
                        )
                    )
                }
            }.takeLast(20)
        }.getOrDefault(emptyList())
    }

    private fun encodeConversation(turns: List<ConversationTurn>): String {
        val arr = JSONArray()
        turns.takeLast(20).forEach { turn ->
            arr.put(JSONObject().apply {
                put("id", turn.id)
                put("isUser", turn.isUser)
                put("text", turn.text)
                put("timestamp", turn.timestamp)
            })
        }
        return arr.toString()
    }

    companion object {
        private const val PREFS_NAME = "pets_on_screen_prefs"
        private const val KEY_ONBOARDED = "onboarded"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_PETS_HIDDEN = "pets_hidden"
        private const val KEY_NUMBER_ON_SCREEN = "number_on_screen"
        private const val KEY_SIZE = "size"
        private const val KEY_SPEED = "speed"
        private const val KEY_TIME_CHANGE = "time_change"
        private const val KEY_SLOTS = "slots"
        private const val KEY_IMPORTED = "imported"
        private const val KEY_CONVERSATION = "conversation"
        private const val KEY_SURVEY_ANSWER = "survey_answer"
        private const val KEY_CMD_SEQ = "cmd_seq"
        private const val KEY_CMD_ACTION = "cmd_action"

        private val DEFAULT_SLOTS = listOf(1, 3, 5, null, null, null, null, null, null)
        private val SHIME_REGEX = Regex("shime(\\d{1,2})\\.png")

        @Volatile
        private var instance: PetPreferencesRepository? = null

        fun getInstance(context: Context): PetPreferencesRepository {
            return instance ?: synchronized(this) {
                instance ?: PetPreferencesRepository(context).also { instance = it }
            }
        }
    }
}
