package com.spamsentinel.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "spam_sentinel_settings")

class AppPreferences(private val context: Context) {

    private object Keys {
        val PROTECTION_ACTIVE = booleanPreferencesKey("protection_active")
        val TAU_HIGH = floatPreferencesKey("tau_high")
        val TAU_LOW = floatPreferencesKey("tau_low")
        val STRICT_MODE = booleanPreferencesKey("strict_mode")
        val B_MIN = intPreferencesKey("b_min")
        val RETRAIN_ON_CHARGING = booleanPreferencesKey("retrain_on_charging")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val PRIVACY_BLUR = booleanPreferencesKey("privacy_blur")
        val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
        val QUIET_HOURS_START = intPreferencesKey("quiet_hours_start")
        val QUIET_HOURS_END = intPreferencesKey("quiet_hours_end")
        val TRUST_CONTACTS = booleanPreferencesKey("trust_contacts")
        val WEEKLY_DIGEST_ENABLED = booleanPreferencesKey("weekly_digest_enabled")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val protectionActive: Flow<Boolean> = context.dataStore.data.map { it[Keys.PROTECTION_ACTIVE] ?: true }
    val tauHigh: Flow<Float> = context.dataStore.data.map { it[Keys.TAU_HIGH] ?: 0.80f }
    val tauLow: Flow<Float> = context.dataStore.data.map { it[Keys.TAU_LOW] ?: 0.20f }
    val strictMode: Flow<Boolean> = context.dataStore.data.map { it[Keys.STRICT_MODE] ?: false }
    val bMin: Flow<Int> = context.dataStore.data.map { it[Keys.B_MIN] ?: 10 }
    val retrainOnCharging: Flow<Boolean> = context.dataStore.data.map { it[Keys.RETRAIN_ON_CHARGING] ?: true }
    val themeMode: Flow<String> = context.dataStore.data.map { it[Keys.THEME_MODE] ?: "SYSTEM" }
    val biometricEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.BIOMETRIC_ENABLED] ?: false }
    val privacyBlur: Flow<Boolean> = context.dataStore.data.map { it[Keys.PRIVACY_BLUR] ?: false }
    val quietHoursEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.QUIET_HOURS_ENABLED] ?: false }
    val trustContacts: Flow<Boolean> = context.dataStore.data.map { it[Keys.TRUST_CONTACTS] ?: true }
    val weeklyDigestEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.WEEKLY_DIGEST_ENABLED] ?: true }
    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDING_COMPLETED] ?: false }

    suspend fun setProtectionActive(active: Boolean) {
        context.dataStore.edit { it[Keys.PROTECTION_ACTIVE] = active }
    }

    suspend fun setTauHigh(tau: Float) {
        context.dataStore.edit { it[Keys.TAU_HIGH] = tau }
    }

    suspend fun setTauLow(tau: Float) {
        context.dataStore.edit { it[Keys.TAU_LOW] = tau }
    }

    suspend fun setStrictMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.STRICT_MODE] = enabled }
    }

    suspend fun setBMin(bMin: Int) {
        context.dataStore.edit { it[Keys.B_MIN] = bMin }
    }

    suspend fun setRetrainOnCharging(enabled: Boolean) {
        context.dataStore.edit { it[Keys.RETRAIN_ON_CHARGING] = enabled }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun setPrivacyBlur(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PRIVACY_BLUR] = enabled }
    }

    suspend fun setQuietHoursEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.QUIET_HOURS_ENABLED] = enabled }
    }

    suspend fun setTrustContacts(trust: Boolean) {
        context.dataStore.edit { it[Keys.TRUST_CONTACTS] = trust }
    }

    suspend fun setWeeklyDigestEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.WEEKLY_DIGEST_ENABLED] = enabled }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }
}
