package com.studypulse.sync.domain.product

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserJourneyPatternsTest {

    @Test
    fun firstLaunchJourney() {
        val freshProfile = FlowProfile(onboardingCompleted = false)

        assertTrue(ProductFlowPolicy.shouldShowOnboarding(freshProfile))

        val onboardedProfile = freshProfile.copy(onboardingCompleted = true)

        assertFalse(ProductFlowPolicy.shouldShowOnboarding(onboardedProfile))
        assertTrue(
            ProductFlowPolicy.shouldRunAutoPlan(
                profile = onboardedProfile,
                hasActiveTasks = true,
                hasTodaySlots = false
            )
        )

        assertTrue(
            ProductFlowPolicy.shouldScheduleSlotReminder(
                profile = onboardedProfile,
                slot = FlowSlot(status = "PLANNED"),
                minutesUntilStart = 12,
                currentHour = 9
            )
        )

        assertTrue(
            ProductFlowPolicy.shouldSendMorningDigest(
                profile = onboardedProfile,
                hasTodaySlots = true,
                currentHour = 7
            )
        )
    }

    @Test
    fun returningUserWithExistingPlanDoesNotRegenerateAutomatically() {
        val profile = FlowProfile(onboardingCompleted = true, autoPlanEnabled = true)

        assertFalse(
            ProductFlowPolicy.shouldRunAutoPlan(
                profile = profile,
                hasActiveTasks = true,
                hasTodaySlots = true
            )
        )

        assertTrue(
            ProductFlowPolicy.shouldScheduleSlotReminder(
                profile = profile,
                slot = FlowSlot(status = "PLANNED"),
                minutesUntilStart = 15,
                currentHour = 14
            )
        )
    }

    @Test
    fun userWithoutTasksShouldNotGetEmptyAutoPlan() {
        val profile = FlowProfile(onboardingCompleted = true, autoPlanEnabled = true)

        assertFalse(
            ProductFlowPolicy.shouldRunAutoPlan(
                profile = profile,
                hasActiveTasks = false,
                hasTodaySlots = false
            )
        )
    }

    @Test
    fun nightTimeUserShouldNotBeDisturbed() {
        val profile = FlowProfile(
            onboardingCompleted = true,
            notificationsEnabled = true,
            slotRemindersEnabled = true,
            deadlineRemindersEnabled = true,
            morningDigestEnabled = true,
            quietHoursEnabled = true,
            quietHoursStart = 23,
            quietHoursEnd = 7
        )

        assertFalse(
            ProductFlowPolicy.shouldScheduleSlotReminder(
                profile = profile,
                slot = FlowSlot(status = "PLANNED"),
                minutesUntilStart = 10,
                currentHour = 1
            )
        )

        assertFalse(
            ProductFlowPolicy.shouldSendDeadlineReminder(
                profile = profile,
                task = FlowTask(status = "PENDING", remainingMinutes = 90),
                hoursBeforeDeadline = 3,
                currentHour = 2
            )
        )

        assertFalse(
            ProductFlowPolicy.shouldSendMorningDigest(
                profile = profile,
                hasTodaySlots = true,
                currentHour = 3
            )
        )
    }

    @Test
    fun morningDigestIsAllowedAfterQuietHoursEnd() {
        val profile = FlowProfile(
            onboardingCompleted = true,
            quietHoursEnabled = true,
            quietHoursStart = 23,
            quietHoursEnd = 7
        )

        assertTrue(
            ProductFlowPolicy.shouldSendMorningDigest(
                profile = profile,
                hasTodaySlots = true,
                currentHour = 7
            )
        )
    }

    @Test
    fun offlineBackupRestoreJourney() {
        val validBackup = ProductFlowPolicy.shouldAllowBackupRestore(
            backupExists = true,
            jsonLooksValid = true
        )

        val missingBackup = ProductFlowPolicy.shouldAllowBackupRestore(
            backupExists = false,
            jsonLooksValid = true
        )

        val corruptedBackup = ProductFlowPolicy.shouldAllowBackupRestore(
            backupExists = true,
            jsonLooksValid = false
        )

        assertTrue(validBackup)
        assertFalse(missingBackup)
        assertFalse(corruptedBackup)
    }

    @Test
    fun disabledNotificationsSuppressAllReminderTypes() {
        val profile = FlowProfile(
            onboardingCompleted = true,
            notificationsEnabled = false,
            slotRemindersEnabled = true,
            deadlineRemindersEnabled = true,
            morningDigestEnabled = true
        )

        assertFalse(
            ProductFlowPolicy.shouldScheduleSlotReminder(
                profile = profile,
                slot = FlowSlot(status = "PLANNED"),
                minutesUntilStart = 10,
                currentHour = 10
            )
        )

        assertFalse(
            ProductFlowPolicy.shouldSendDeadlineReminder(
                profile = profile,
                task = FlowTask(status = "PENDING", remainingMinutes = 60),
                hoursBeforeDeadline = 24,
                currentHour = 10
            )
        )

        assertFalse(
            ProductFlowPolicy.shouldSendMorningDigest(
                profile = profile,
                hasTodaySlots = true,
                currentHour = 7
            )
        )
    }
}