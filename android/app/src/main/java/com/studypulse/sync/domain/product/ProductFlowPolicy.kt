package com.studypulse.sync.domain.product

data class FlowProfile(
    val onboardingCompleted: Boolean = false,
    val autoPlanEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val slotRemindersEnabled: Boolean = true,
    val deadlineRemindersEnabled: Boolean = true,
    val morningDigestEnabled: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: Int = 23,
    val quietHoursEnd: Int = 7
)

data class FlowTask(
    val status: String,
    val remainingMinutes: Int = 0
)

data class FlowSlot(
    val status: String
)

object ProductFlowPolicy {

    fun shouldShowOnboarding(
        profile: FlowProfile
    ): Boolean {
        return !profile.onboardingCompleted
    }

    fun shouldRunAutoPlan(
        profile: FlowProfile,
        hasActiveTasks: Boolean,
        hasTodaySlots: Boolean
    ): Boolean {
        return profile.onboardingCompleted &&
            profile.autoPlanEnabled &&
            hasActiveTasks &&
            !hasTodaySlots
    }

    fun shouldScheduleSlotReminder(
        profile: FlowProfile,
        slot: FlowSlot,
        minutesUntilStart: Int,
        currentHour: Int
    ): Boolean {
        return profile.notificationsEnabled &&
            profile.slotRemindersEnabled &&
            slot.status == "PLANNED" &&
            minutesUntilStart in 1..15 &&
            !isQuietNow(profile, currentHour)
    }

    fun shouldSendDeadlineReminder(
        profile: FlowProfile,
        task: FlowTask,
        hoursBeforeDeadline: Int,
        currentHour: Int
    ): Boolean {
        return profile.notificationsEnabled &&
            profile.deadlineRemindersEnabled &&
            task.status != "DONE" &&
            task.remainingMinutes > 0 &&
            (hoursBeforeDeadline == 24 || hoursBeforeDeadline == 3) &&
            !isQuietNow(profile, currentHour)
    }

    fun shouldSendMorningDigest(
        profile: FlowProfile,
        hasTodaySlots: Boolean,
        currentHour: Int = 7
    ): Boolean {
        return profile.notificationsEnabled &&
            profile.morningDigestEnabled &&
            hasTodaySlots &&
            !isQuietNow(profile, currentHour)
    }

    fun shouldAllowBackupRestore(
        backupExists: Boolean,
        jsonLooksValid: Boolean
    ): Boolean {
        return backupExists && jsonLooksValid
    }

    fun isQuietNow(
        profile: FlowProfile,
        hourOfDay: Int
    ): Boolean {
        if (!profile.quietHoursEnabled) {
            return false
        }

        val hour = normalizeHour(hourOfDay)
        val start = normalizeHour(profile.quietHoursStart)
        val end = normalizeHour(profile.quietHoursEnd)

        if (start == end) {
            return false
        }

        return if (start < end) {
            hour in start until end
        } else {
            hour >= start || hour < end
        }
    }

    private fun normalizeHour(value: Int): Int {
        return ((value % 24) + 24) % 24
    }
}