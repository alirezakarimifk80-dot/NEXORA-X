package com.nexora.x.domain.model

enum class SettingsCategory(val title: String) {
    GENERAL("GENERAL"),
    CONNECTION("CONNECTION"),
    ADVANCED("ADVANCED"),
    ABOUT("ABOUT")
}

enum class SettingImplementationStatus(val badgeText: String) {
    ACTIVE_PHASE_1("ACTIVE"),
    COMING_SOON_PHASE_2("COMING SOON · PHASE 2"),
    COMING_SOON_PHASE_3("COMING SOON · PHASE 3")
}

data class SettingsOption(
    val id: String,
    val category: SettingsCategory,
    val title: String,
    val subtitle: String,
    val currentValue: String,
    val status: SettingImplementationStatus
)
