package com.petsonscreen.app.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.petsonscreen.app.data.PetPreferencesRepository
import com.petsonscreen.app.model.CommandEvent
import com.petsonscreen.app.model.PetAppPreferences
import com.petsonscreen.app.model.PetCommandAction
import com.petsonscreen.app.service.PetOverlayService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface UiNavCommand {
    data object OpenHome : UiNavCommand
    data object OpenCollection : UiNavCommand
    data object OpenSettings : UiNavCommand
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PetPreferencesRepository.getInstance(application)

    val preferences: StateFlow<PetAppPreferences> = repository.state
    val commandEvents: StateFlow<CommandEvent?> = repository.commandEvents

    private val _canDrawOverlays = MutableStateFlow(Settings.canDrawOverlays(application))
    val canDrawOverlays: StateFlow<Boolean> = _canDrawOverlays.asStateFlow()

    private val _showOverlayPermissionDialog = MutableStateFlow(false)
    val showOverlayPermissionDialog: StateFlow<Boolean> = _showOverlayPermissionDialog.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _collectionPage = MutableStateFlow(0)
    val collectionPage: StateFlow<Int> = _collectionPage.asStateFlow()

    private val _targetSlotIndex = MutableStateFlow<Int?>(null)
    val targetSlotIndex: StateFlow<Int?> = _targetSlotIndex.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _navEvents = MutableSharedFlow<UiNavCommand>(extraBufferCapacity = 4)
    val navEvents: SharedFlow<UiNavCommand> = _navEvents.asSharedFlow()

    private val _ttsSpeechEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val ttsSpeechEvents: SharedFlow<String> = _ttsSpeechEvents.asSharedFlow()

    fun refreshOverlayPermission() {
        val granted = Settings.canDrawOverlays(getApplication())
        _canDrawOverlays.value = granted
        val prefs = preferences.value
        if (granted && prefs.enabled) {
            PetOverlayService.syncServiceState(getApplication(), prefs)
        }
    }

    fun dismissOverlayPermissionDialog() {
        _showOverlayPermissionDialog.value = false
    }

    fun completeOnboarding(surveyAnswer: String?) {
        repository.completeOnboarding(surveyAnswer, enablePets = true)
        val granted = Settings.canDrawOverlays(getApplication())
        _canDrawOverlays.value = granted
        if (granted) {
            PetOverlayService.syncServiceState(getApplication(), repository.state.value)
        } else {
            _showOverlayPermissionDialog.value = true
        }
    }

    fun setPetsEnabled(enabled: Boolean) {
        repository.setEnabled(enabled)
        val granted = Settings.canDrawOverlays(getApplication())
        _canDrawOverlays.value = granted
        if (enabled && !granted) {
            _showOverlayPermissionDialog.value = true
        }
        PetOverlayService.syncServiceState(getApplication(), repository.state.value)
    }

    fun togglePetsHidden() {
        val next = !preferences.value.petsHidden
        repository.setPetsHidden(next)
        PetOverlayService.syncServiceState(getApplication(), repository.state.value)
    }

    fun setNumberOnScreen(count: Int) {
        repository.setNumberOnScreen(count)
    }

    fun setSize(size: Float) {
        repository.setSize(size)
    }

    fun setSpeed(speed: Float) {
        repository.setSpeed(speed)
    }

    fun setTimeChange(minutes: Int) {
        repository.setTimeChange(minutes)
    }

    fun beginSlotSelection(slotIndex: Int) {
        _targetSlotIndex.value = slotIndex
    }

    fun clearSlotSelection() {
        _targetSlotIndex.value = null
    }

    fun assignPetToTargetSlot(petId: Int) {
        val slot = _targetSlotIndex.value
        if (slot != null) {
            repository.setSlot(slot, petId)
            _targetSlotIndex.value = null
        }
    }

    fun addPetToHome(petId: Int): Boolean {
        return repository.addToFirstEmptySlot(petId)
    }

    fun removePetFromHome(petId: Int) {
        repository.removeFromAllSlots(petId)
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
        _collectionPage.value = 0
    }

    fun setCollectionPage(page: Int) {
        _collectionPage.value = page.coerceAtLeast(0)
    }

    fun importZip(uri: Uri) {
        if (_isImporting.value) return
        viewModelScope.launch {
            _isImporting.value = true
            val result = repository.importShimejiZip(uri)
            _isImporting.value = false
            result.onSuccess { pet ->
                _selectedCategory.value = "My Pets"
                _collectionPage.value = 0
                _statusBanner.value = "Imported \"${pet.name}\" (46 frames)"
            }.onFailure { err ->
                _statusBanner.value = err.message ?: "Failed to import Shimeji ZIP pack"
            }
        }
    }

    fun deleteImportedPet(petId: Int) {
        repository.deleteImportedPet(petId)
        val remainingImported = repository.state.value.imported
        if (remainingImported.isEmpty() && _selectedCategory.value == "My Pets") {
            _selectedCategory.value = "All"
            _collectionPage.value = 0
        }
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun clearConversation() {
        repository.clearConversation()
    }

    fun processCommand(rawText: String) {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) return
        val lower = trimmed.lowercase()

        val reply: String = when {
            containsAny(lower, "walk", "move", "run") -> {
                repository.dispatchPetCommand(PetCommandAction.WALK)
                "Your companions are now walking across the screen."
            }
            containsAny(lower, "jump", "hop") -> {
                repository.dispatchPetCommand(PetCommandAction.JUMP)
                "Up they go! Companions are jumping."
            }
            containsAny(lower, "sit", "rest") -> {
                repository.dispatchPetCommand(PetCommandAction.SIT)
                "Companions settled down to sit and rest."
            }
            containsAny(lower, "hide", "disappear") -> {
                if (preferences.value.enabled) {
                    repository.setPetsHidden(true)
                    PetOverlayService.syncServiceState(getApplication(), repository.state.value)
                }
                "Companions tucked away out of sight."
            }
            containsAny(lower, "show", "appear") -> {
                if (!preferences.value.enabled) {
                    setPetsEnabled(true)
                } else {
                    repository.setPetsHidden(false)
                    PetOverlayService.syncServiceState(getApplication(), repository.state.value)
                }
                "Your companions are back on screen."
            }
            containsAny(lower, "home") -> {
                _navEvents.tryEmit(UiNavCommand.OpenHome)
                "Opening Home roster."
            }
            containsAny(lower, "collection") -> {
                _navEvents.tryEmit(UiNavCommand.OpenCollection)
                "Opening Companion Collection."
            }
            containsAny(lower, "settings") -> {
                _navEvents.tryEmit(UiNavCommand.OpenSettings)
                "You are in Settings."
            }
            else -> {
                "Try saying or typing: walk, jump, sit, hide, show, home, or collection."
            }
        }

        repository.appendConversation(trimmed, reply)
        _ttsSpeechEvents.tryEmit(reply)
    }

    private fun containsAny(text: String, vararg keywords: String): Boolean {
        return keywords.any { kw ->
            Regex("\\b${Regex.escape(kw)}\\b").containsMatchIn(text) || text.contains(kw)
        }
    }
}
