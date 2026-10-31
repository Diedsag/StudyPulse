StudyPulse Phase 3.6 pending widget polish

Status

Do not run Phase 3.6 until Phase 3.5 crash fix is verified.

Purpose

Save the Phase 3.6 implementation plan after the Phase 3.5 initialization fix.

Phase 3.6 goal

Polish the home-screen widget without adding complex interactive widget actions.

Widget should show:

- app name
- greeting with user display name
- current date
- nearest planned slot
- slot start time
- due cards count
- urgent tasks count

Files to add

android/app/src/main/java/com/studypulse/sync/widget/WidgetUpdateWorker.kt
android/app/src/main/java/com/studypulse/sync/widget/WidgetScheduler.kt

Files to replace

android/app/src/main/java/com/studypulse/sync/widget/StudyPulseWidget.kt

Files to patch

android/app/src/main/java/com/studypulse/sync/MainActivity.kt

MainActivity patch

Add import:

import com.studypulse.sync.widget.WidgetScheduler

Inside StudyPulseRoot LaunchedEffect, after:

application.container.reminderScheduler.scheduleDeadlineReminders()

add:

WidgetScheduler.updateSoon(application)

WidgetUpdateWorker behavior

- gets all installed StudyPulseWidget glance ids
- calls widget update for each glance id
- returns success even if update fails, to avoid background crash noise

WidgetScheduler behavior

- enqueues a unique one-time WorkManager request
- uses REPLACE policy
- initial delay about 1 second

StudyPulseWidget behavior

- reads profileRepository.currentProfile()
- reads studySlotDao.getNextSlotOnce(now)
- reads cardDao.countDueOnce(now)
- reads taskDao.getUrgentOnce(tomorrow, 10)
- renders compact Glance UI
- clicking widget opens MainActivity

Documentation to add

docs/PHASE3_6_WIDGET.md

Commit message for Phase 3.6

feat(phase-3.6): polish widget with urgent tasks and update scheduler