package com.petsonscreen.app.model

data class PetItem(
    val id: Int,
    val name: String,
    val category: String,
    val isBuiltIn: Boolean = true
)

data class ConversationTurn(
    val id: Long,
    val isUser: Boolean,
    val text: String,
    val timestamp: Long
)

enum class PetCommandAction {
    WALK,
    JUMP,
    SIT
}

data class CommandEvent(
    val seq: Long,
    val action: PetCommandAction
)

data class PetAppPreferences(
    val onboarded: Boolean = false,
    val enabled: Boolean = false,
    val petsHidden: Boolean = false,
    val numberOnScreen: Int = 1,
    val size: Float = 1.6f,
    val speed: Float = 1.5f,
    val timeChange: Int = 5,
    val slots: List<Int?> = listOf(1, 3, 5, null, null, null, null, null, null),
    val imported: List<PetItem> = emptyList(),
    val conversation: List<ConversationTurn> = emptyList(),
    val surveyAnswer: String? = null
) {
    val allPets: List<PetItem>
        get() = BuiltInPets.list + imported

    fun findPet(petId: Int?): PetItem? {
        if (petId == null) return null
        return allPets.firstOrNull { it.id == petId }
    }

    val filledSlotsCount: Int
        get() = slots.count { it != null }

    fun activeDisplayCount(): Int {
        val filled = slots.filterNotNull()
        if (filled.isEmpty()) return 0
        return minOf(numberOnScreen, filled.size)
    }
}

object BuiltInPets {
    val list: List<PetItem> = listOf(
        PetItem(id = 1, name = "Mochi", category = "Cats", isBuiltIn = true),
        PetItem(id = 2, name = "Smokey", category = "Cats", isBuiltIn = true),
        PetItem(id = 3, name = "Bun", category = "Bunnies", isBuiltIn = true),
        PetItem(id = 4, name = "Biscuit", category = "Dogs", isBuiltIn = true),
        PetItem(id = 5, name = "Bao", category = "Bears", isBuiltIn = true),
        PetItem(id = 6, name = "Kero", category = "Frogs", isBuiltIn = true),
        PetItem(id = 7, name = "Foxy", category = "Foxes", isBuiltIn = true)
    )

    val categories: List<String> = listOf(
        "All",
        "Cats",
        "Bunnies",
        "Dogs",
        "Bears",
        "Frogs",
        "Foxes"
    )
}
