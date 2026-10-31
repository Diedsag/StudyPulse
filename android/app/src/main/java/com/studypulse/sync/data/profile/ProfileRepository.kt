package com.studypulse.sync.data.profile

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.UUID

val Context.profileDataStore: DataStore<Preferences> by preferencesDataStore(name = "profile")

data class StudentProfile(
    val onboardingCompleted: Boolean = false,
    val displayName: String = "",
    val studyStartHour: Int = 9,
    val studyEndHour: Int = 21,
    val defaultDailyMinutes: Int = 120,
    val notificationsEnabled: Boolean = true,
    val autoPlanEnabled: Boolean = true,
    val slotRemindersEnabled: Boolean = true,
    val deadlineRemindersEnabled: Boolean = true,
    val morningDigestEnabled: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: Int = 23,
    val quietHoursEnd: Int = 7
)

private object ProfileKeys {
    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    val DISPLAY_NAME = stringPreferencesKey("display_name")
    val STUDY_START_HOUR = intPreferencesKey("study_start_hour")
    val STUDY_END_HOUR = intPreferencesKey("study_end_hour")
    val DEFAULT_DAILY_MINUTES = intPreferencesKey("default_daily_minutes")
    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    val AUTO_PLAN_ENABLED = booleanPreferencesKey("auto_plan_enabled")
    val SLOT_REMINDERS_ENABLED = booleanPreferencesKey("slot_reminders_enabled")
    val DEADLINE_REMINDERS_ENABLED = booleanPreferencesKey("deadline_reminders_enabled")
    val MORNING_DIGEST_ENABLED = booleanPreferencesKey("morning_digest_enabled")
    val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
    val QUIET_HOURS_START = intPreferencesKey("quiet_hours_start")
    val QUIET_HOURS_END = intPreferencesKey("quiet_hours_end")
}

class ProfileRepository(
    private val dataStore: DataStore<Preferences>
) {

    val profile: Flow<StudentProfile> =
        dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                StudentProfile(
                    onboardingCompleted = preferences[ProfileKeys.ONBOARDING_COMPLETED] ?: false,
                    displayName = preferences[ProfileKeys.DISPLAY_NAME].orEmpty(),
                    studyStartHour = preferences[ProfileKeys.STUDY_START_HOUR] ?: 9,
                    studyEndHour = preferences[ProfileKeys.STUDY_END_HOUR] ?: 21,
                    defaultDailyMinutes = preferences[ProfileKeys.DEFAULT_DAILY_MINUTES] ?: 120,
                    notificationsEnabled = preferences[ProfileKeys.NOTIFICATIONS_ENABLED] ?: true,
                    autoPlanEnabled = preferences[ProfileKeys.AUTO_PLAN_ENABLED] ?: true,
                    slotRemindersEnabled = preferences[ProfileKeys.SLOT_REMINDERS_ENABLED] ?: true,
                    deadlineRemindersEnabled = preferences[ProfileKeys.DEADLINE_REMINDERS_ENABLED] ?: true,
                    morningDigestEnabled = preferences[ProfileKeys.MORNING_DIGEST_ENABLED] ?: true,
                    quietHoursEnabled = preferences[ProfileKeys.QUIET_HOURS_ENABLED] ?: false,
                    quietHoursStart = preferences[ProfileKeys.QUIET_HOURS_START] ?: 23,
                    quietHoursEnd = preferences[ProfileKeys.QUIET_HOURS_END] ?: 7
                )
            }

    suspend fun currentProfile(): StudentProfile = profile.first()

    suspend fun saveOnboarding(
        displayName: String,
        studyStartHour: Int,
        studyEndHour: Int,
        defaultDailyMinutes: Int,
        notificationsEnabled: Boolean,
        autoPlanEnabled: Boolean
    ) {
        val normalizedName = normalizeDisplayName(displayName)
        val normalizedWindow = normalizeStudyWindow(studyStartHour, studyEndHour)
        val normalizedMinutes = normalizeDailyMinutes(defaultDailyMinutes)

        dataStore.edit { preferences ->
            preferences[ProfileKeys.DISPLAY_NAME] = normalizedName
            preferences[ProfileKeys.STUDY_START_HOUR] = normalizedWindow.first
            preferences[ProfileKeys.STUDY_END_HOUR] = normalizedWindow.second
            preferences[ProfileKeys.DEFAULT_DAILY_MINUTES] = normalizedMinutes
            preferences[ProfileKeys.NOTIFICATIONS_ENABLED] = notificationsEnabled
            preferences[ProfileKeys.AUTO_PLAN_ENABLED] = autoPlanEnabled
            preferences[ProfileKeys.ONBOARDING_COMPLETED] = true
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[ProfileKeys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setAutoPlanEnabled(enabled: Boolean) {
        dataStore.edit { it[ProfileKeys.AUTO_PLAN_ENABLED] = enabled }
    }

    suspend fun setSlotRemindersEnabled(enabled: Boolean) {
        dataStore.edit { it[ProfileKeys.SLOT_REMINDERS_ENABLED] = enabled }
    }

    suspend fun setDeadlineRemindersEnabled(enabled: Boolean) {
        dataStore.edit { it[ProfileKeys.DEADLINE_REMINDERS_ENABLED] = enabled }
    }

    suspend fun setMorningDigestEnabled(enabled: Boolean) {
        dataStore.edit { it[ProfileKeys.MORNING_DIGEST_ENABLED] = enabled }
    }

    suspend fun setQuietHours(enabled: Boolean, start: Int, end: Int) {
        val safeStart = start.coerceIn(0, 23)
        val safeEnd = end.coerceIn(0, 23)
        dataStore.edit { preferences ->
            preferences[ProfileKeys.QUIET_HOURS_ENABLED] = enabled
            preferences[ProfileKeys.QUIET_HOURS_START] = safeStart
            preferences[ProfileKeys.QUIET_HOURS_END] = safeEnd
        }
    }

    suspend fun resetOnboarding() {
        dataStore.edit { it[ProfileKeys.ONBOARDING_COMPLETED] = false }
    }

    private fun normalizeDisplayName(value: String): String {
        val trimmed = value.trim()
        return if (trimmed.isBlank()) "Студент" else trimmed
    }

    private fun normalizeStudyWindow(startHour: Int, endHour: Int): Pair<Int, Int> {
        var safeStart = startHour.coerceIn(6, 22)
        var safeEnd = endHour.coerceIn(7, 23)
        if (safeEnd <= safeStart) safeEnd = (safeStart + 1).coerceAtMost(23)
        if (safeEnd <= safeStart) safeStart = (safeEnd - 1).coerceAtLeast(6)
        return safeStart to safeEnd
    }

    private fun normalizeDailyMinutes(minutes: Int): Int = minutes.coerceIn(30, 360)
}