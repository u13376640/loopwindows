package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.db.SettingEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

class LoopRepository(private val database: AppDatabase) {

    private val settingsDao = database.settingsDao()

    companion object {
        const val DEFAULT_URL = "https://dcsplayer.blogspot.com/"
        private const val KEY_ACTIVE_URL = "active_url"
        private const val KEY_JS_ENABLED = "js_enabled"
        private const val KEY_DESKTOP_MODE = "desktop_mode"
        private const val KEY_APP_PASSWORD_HASH = "app_password_hash"
        private const val KEY_ADBLOCK_ENABLED = "adblock_enabled"
        private const val KEY_BLOCKED_ADS_COUNT = "blocked_ads_count"
    }

    val activeUrl: Flow<String> = settingsDao.getSettingValue(KEY_ACTIVE_URL)
        .map { value ->
            if (value.isNullOrBlank()) DEFAULT_URL else value
        }

    val isJsEnabled: Flow<Boolean> = settingsDao.getSettingValue(KEY_JS_ENABLED)
        .map { value ->
            value?.toBooleanStrictOrNull() ?: true
        }

    val isDesktopMode: Flow<Boolean> = settingsDao.getSettingValue(KEY_DESKTOP_MODE)
        .map { value ->
            value?.toBooleanStrictOrNull() ?: false
        }

    val hasPassword: Flow<Boolean> = settingsDao.getSettingValue(KEY_APP_PASSWORD_HASH)
        .map { hash -> !hash.isNullOrBlank() }

    val isAdBlockEnabled: Flow<Boolean> = settingsDao.getSettingValue(KEY_ADBLOCK_ENABLED)
        .map { value ->
            value?.toBooleanStrictOrNull() ?: true // Default enabled
        }

    val blockedAdsCount: Flow<Int> = settingsDao.getSettingValue(KEY_BLOCKED_ADS_COUNT)
        .map { value ->
            value?.toIntOrNull() ?: 0
        }

    suspend fun setActiveUrl(rawUrl: String): String {
        val sanitized = sanitizeUrl(rawUrl)
        settingsDao.saveSetting(SettingEntity(KEY_ACTIVE_URL, sanitized))
        return sanitized
    }

    suspend fun resetToDefaultUrl(): String {
        settingsDao.saveSetting(SettingEntity(KEY_ACTIVE_URL, DEFAULT_URL))
        return DEFAULT_URL
    }

    suspend fun setJsEnabled(enabled: Boolean) {
        settingsDao.saveSetting(SettingEntity(KEY_JS_ENABLED, enabled.toString()))
    }

    suspend fun setDesktopMode(enabled: Boolean) {
        settingsDao.saveSetting(SettingEntity(KEY_DESKTOP_MODE, enabled.toString()))
    }

    suspend fun setAdBlockEnabled(enabled: Boolean) {
        settingsDao.saveSetting(SettingEntity(KEY_ADBLOCK_ENABLED, enabled.toString()))
    }

    suspend fun incrementBlockedAdsCount(amount: Int = 1) {
        val currentStr = settingsDao.getSettingValue(KEY_BLOCKED_ADS_COUNT).first()
        val current = currentStr?.toIntOrNull() ?: 0
        settingsDao.saveSetting(SettingEntity(KEY_BLOCKED_ADS_COUNT, (current + amount).toString()))
    }

    suspend fun resetBlockedAdsCount() {
        settingsDao.saveSetting(SettingEntity(KEY_BLOCKED_ADS_COUNT, "0"))
    }

    // App Password Management
    suspend fun setPassword(password: String) {
        if (password.isBlank()) {
            settingsDao.saveSetting(SettingEntity(KEY_APP_PASSWORD_HASH, ""))
        } else {
            val hash = hashPassword(password)
            settingsDao.saveSetting(SettingEntity(KEY_APP_PASSWORD_HASH, hash))
        }
    }

    suspend fun removePassword(): Boolean {
        settingsDao.saveSetting(SettingEntity(KEY_APP_PASSWORD_HASH, ""))
        return true
    }

    suspend fun verifyPassword(input: String): Boolean {
        val storedHash = settingsDao.getSettingValue(KEY_APP_PASSWORD_HASH).first()
        if (storedHash.isNullOrBlank()) return true // No password set
        return hashPassword(input) == storedHash
    }

    suspend fun isPasswordSet(): Boolean {
        val storedHash = settingsDao.getSettingValue(KEY_APP_PASSWORD_HASH).first()
        return !storedHash.isNullOrBlank()
    }

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun sanitizeUrl(input: String): String {
        var trimmed = input.trim()
        if (trimmed.isEmpty()) return DEFAULT_URL
        if (!trimmed.startsWith("http://", ignoreCase = true) &&
            !trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed = "https://$trimmed"
        }
        return trimmed
    }
}
