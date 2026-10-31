package com.studypulse.sync.domain.product

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductFlowPolicyTest {

    private fun defaultProfile() = FlowProfile()

    @Test
    fun newUsersShouldSeeOnboarding() {
        val freshProfile = FlowProfile(onboardingCompleted = false)
        val onboardedProfile = FlowProfile(onboardingCompleted = true)

        assertTrue(ProductFlowPolicy.shouldShowOnboarding(freshProfile))
        assertFalse(ProductFlowPolicy.shouldShowOnboarding(onboardedProfile))
    }

    @Test
    fun autoPlanRunsOnlyWhenOnboardedEnabledHasTasksAndNoTodaySlots() {
        val profile = defaultProfile().copy(onboardingCompleted = true, autoPlanEnabled = true)

        assertTrue(ProductFlowPolicy.shouldRunAutoPlan(profile, hasActiveTasks = true, hasTodaySlots = false))
        assertFalse(ProductFlowPolicy.shouldRunAutoPlan(profile, hasActiveTasks = false, hasTodaySlots = false))
        assertFalse(ProductFlowPolicy.shouldRunAutoPlan(profile, hasActiveTasks = true, hasTodaySlots = true))
        assertFalse(ProductFlowPolicy.shouldRunAutoPlan(profile.copy(autoPlanEnabled = false), hasActiveTasks = true, hasTodaySlots = false))
        assertFalse(ProductFlowPolicy.shouldRunAutoPlan(profile.copy(onboardingCompleted = false), hasActiveTasks = true, hasTodaySlots = false))
    }

    @Test
    fun slotReminderIsScheduledOnlyForPlannedSlotsWithin15Minutes() {
        val profile = defaultProfile().copy(onboardingCompleted = true)
        val plannedSlot = FlowSlot(status = "PLANNED")
        val doneSlot = FlowSlot(status = "DONE")

        assertTrue(ProductFlowPolicy.shouldScheduleSlotReminder(profile, plannedSlot, minutesUntilStart = 15, currentHour = 10))
        assertTrue(ProductFlowPolicy.shouldScheduleSlotReminder(profile, plannedSlot, minutesUntilStart = 1, currentHour = 10))
        assertFalse(ProductFlowPolicy.shouldScheduleSlotReminder(profile, plannedSlot, minutesUntilStart = 16, currentHour = 10))
        assertFalse(ProductFlowPolicy.shouldScheduleSlotReminder(profile, plannedSlot, minutesUntilStart = 0, currentHour = 10))
        assertFalse(ProductFlowPolicy.shouldScheduleSlotReminder(profile, doneSlot, minutesUntilStart = 10, currentHour = 10))
        assertFalse(ProductFlowPolicy.shouldScheduleSlotReminder(profile.copy(notificationsEnabled = false), plannedSlot, minutesUntilStart = 10, currentHour = 10))
        assertFalse(ProductFlowPolicy.shouldScheduleSlotReminder(profile.copy(slotRemindersEnabled = false), plannedSlot, minutesUntilStart = 10, currentHour = 10))
    }

    @Test
    fun deadlineReminderWorksFor24And3HoursOnly() {
        val profile = defaultProfile().copy(onboardingCompleted = true)
        val activeTask = FlowTask(status = "PENDING", remainingMinutes = 60)
        val doneTask = FlowTask(status = "DONE", remainingMinutes = 0)

        assertTrue(ProductFlowPolicy.shouldSendDeadlineReminder(profile, activeTask, hoursBeforeDeadline = 24, currentHour = 12))
        assertTrue(ProductFlowPolicy.shouldSendDeadlineReminder(profile, activeTask, hoursBeforeDeadline = 3, currentHour = 12))
        assertFalse(ProductFlowPolicy.shouldSendDeadlineReminder(profile, activeTask, hoursBeforeDeadline = 6, currentHour = 12))
        assertFalse(ProductFlowPolicy.shouldSendDeadlineReminder(profile, doneTask, hoursBeforeDeadline = 24, currentHour = 12))
        assertFalse(ProductFlowPolicy.shouldSendDeadlineReminder(profile, activeTask.copy(remainingMinutes = 0), hoursBeforeDeadline = 24, currentHour = 12))
        assertFalse(ProductFlowPolicy.shouldSendDeadlineReminder(profile.copy(deadlineRemindersEnabled = false), activeTask, hoursBeforeDeadline = 24, currentHour = 12))
    }

    @Test
    fun morningDigestRequiresSlotsAndEnabledNotification() {
        val profile = defaultProfile().copy(onboardingCompleted = true)

        assertTrue(ProductFlowPolicy.shouldSendMorningDigest(profile, hasTodaySlots = true, currentHour = 7))
        assertFalse(ProductFlowPolicy.shouldSendMorningDigest(profile, hasTodaySlots = false, currentHour = 7))
        assertFalse(ProductFlowPolicy.shouldSendMorningDigest(profile.copy(morningDigestEnabled = false), hasTodaySlots = true, currentHour = 7))
        assertFalse(ProductFlowPolicy.shouldSendMorningDigest(profile.copy(notificationsEnabled = false), hasTodaySlots = true, currentHour = 7))
    }

    @Test
    fun quietHoursSuppressReminders() {
        val quietProfile = defaultProfile().copy(
            onboardingCompleted = true,
            quietHoursEnabled = true,
            quietHoursStart = 23,
            quietHoursEnd = 7
        )

        assertTrue(ProductFlowPolicy.isQuietNow(quietProfile, hourOfDay = 23))
        assertTrue(ProductFlowPolicy.isQuietNow(quietProfile, hourOfDay = 2))
        assertTrue(ProductFlowPolicy.isQuietNow(quietProfile, hourOfDay = 6))
        assertFalse(ProductFlowPolicy.isQuietNow(quietProfile, hourOfDay = 7))
        assertFalse(ProductFlowPolicy.isQuietNow(quietProfile, hourOfDay = 22))

        assertFalse(
            ProductFlowPolicy.shouldScheduleSlotReminder(
                profile = quietProfile,
                slot = FlowSlot("PLANNED"),
                minutesUntilStart = 10,
                currentHour = 2
            )
        )

        assertTrue(
            ProductFlowPolicy.shouldScheduleSlotReminder(
                profile = quietProfile,
                slot = FlowSlot("PLANNED"),
                minutesUntilStart = 10,
                currentHour = 10
            )
        )
    }

    @Test
    fun quietHoursWithNormalRangeWorkCorrectly() {
        val napProfile = defaultProfile().copy(
            quietHoursEnabled = true,
            quietHoursStart = 13,
            quietHoursEnd = 15
        )

        assertTrue(ProductFlowPolicy.isQuietNow(napProfile, hourOfDay = 13))
        assertTrue(ProductFlowPolicy.isQuietNow(napProfile, hourOfDay = 14))
        assertFalse(ProductFlowPolicy.isQuietNow(napProfile, hourOfDay = 15))
        assertFalse(ProductFlowPolicy.isQuietNow(napProfile, hourOfDay = 12))
    }

    @Test
    fun backupRestoreRequiresExistingAndValidBackup() {
        assertTrue(ProductFlowPolicy.shouldAllowBackupRestore(backupExists = true, jsonLooksValid = true))
        assertFalse(ProductFlowPolicy.shouldAllowBackupRestore(backupExists = false, jsonLooksValid = true))
        assertFalse(ProductFlowPolicy.shouldAllowBackupRestore(backupExists = true, jsonLooksValid = false))
        assertFalse(ProductFlowPolicy.shouldAllowBackupRestore(backupExists = false, jsonLooksValid = false))
    }
}