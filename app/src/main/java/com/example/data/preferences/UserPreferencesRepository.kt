package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val HIDE_AMOUNTS = booleanPreferencesKey("hide_amounts")
        val TTS_ENABLED = booleanPreferencesKey("tts_enabled")
        val TTS_LANGUAGE = stringPreferencesKey("tts_language")
        val TTS_SPEECH_RATE = floatPreferencesKey("tts_speech_rate")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val APP_PIN = stringPreferencesKey("app_pin")
        val DEMO_DATA_LOADED = booleanPreferencesKey("demo_data_loaded")
        val SELECTED_PERIOD = stringPreferencesKey("selected_period")
        val AUTOPAY_ALERTS_ENABLED = booleanPreferencesKey("autopay_alerts_enabled")
        val FRAUD_ALERTS_ENABLED = booleanPreferencesKey("fraud_alerts_enabled")
        val HIGH_VALUE_ALERTS_ENABLED = booleanPreferencesKey("high_value_alerts_enabled")
        val HIGH_VALUE_THRESHOLD = androidx.datastore.preferences.core.longPreferencesKey("high_value_threshold")
        val CLOUD_AUTO_SYNC_ENABLED = booleanPreferencesKey("cloud_auto_sync_enabled")
    }

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
    }

    val hideAmounts: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.HIDE_AMOUNTS] ?: false
    }

    val ttsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.TTS_ENABLED] ?: false
    }

    val ttsLanguage: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.TTS_LANGUAGE] ?: "en"
    }

    val ttsSpeechRate: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.TTS_SPEECH_RATE] ?: 1.0f
    }

    val appLockEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.APP_LOCK_ENABLED] ?: false
    }

    val appPin: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.APP_PIN]
    }

    val demoDataLoaded: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.DEMO_DATA_LOADED] ?: false
    }

    val selectedPeriod: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.SELECTED_PERIOD] ?: "THIS_MONTH"
    }

    val autoPayAlertsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.AUTOPAY_ALERTS_ENABLED] ?: true
    }

    val fraudAlertsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.FRAUD_ALERTS_ENABLED] ?: true
    }

    val highValueAlertsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.HIGH_VALUE_ALERTS_ENABLED] ?: true
    }

    val highValueThresholdRupees: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.HIGH_VALUE_THRESHOLD] ?: 5000L
    }

    val cloudAutoSyncEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.CLOUD_AUTO_SYNC_ENABLED] ?: true
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun toggleHideAmounts() {
        context.dataStore.edit { prefs ->
            val current = prefs[PreferencesKeys.HIDE_AMOUNTS] ?: false
            prefs[PreferencesKeys.HIDE_AMOUNTS] = !current
        }
    }

    suspend fun setTtsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.TTS_ENABLED] = enabled
        }
    }

    suspend fun setTtsLanguage(languageCode: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.TTS_LANGUAGE] = languageCode
        }
    }

    suspend fun setTtsSpeechRate(rate: Float) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.TTS_SPEECH_RATE] = rate
        }
    }

    suspend fun setAppLock(enabled: Boolean, pin: String? = null) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.APP_LOCK_ENABLED] = enabled
            if (pin != null) {
                prefs[PreferencesKeys.APP_PIN] = pin
            }
        }
    }

    suspend fun setDemoDataLoaded(loaded: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.DEMO_DATA_LOADED] = loaded
        }
    }

    suspend fun setSelectedPeriod(period: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SELECTED_PERIOD] = period
        }
    }

    suspend fun setAutoPayAlertsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.AUTOPAY_ALERTS_ENABLED] = enabled
        }
    }

    suspend fun setFraudAlertsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.FRAUD_ALERTS_ENABLED] = enabled
        }
    }

    suspend fun setHighValueAlertsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.HIGH_VALUE_ALERTS_ENABLED] = enabled
        }
    }

    suspend fun setHighValueThreshold(threshold: Long) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.HIGH_VALUE_THRESHOLD] = threshold
        }
    }

    suspend fun setCloudAutoSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.CLOUD_AUTO_SYNC_ENABLED] = enabled
        }
    }
}
