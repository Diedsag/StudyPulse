StudyPulse Phase 3.6 - Widget polish

Goal

Make the home-screen widget more useful without adding complex interactive widget actions.

Changes

- StudyPulseWidget reads profile, next slot, due cards and urgent tasks from local repositories.
- Widget shows greeting, date, nearest slot, start time, due cards count and urgent tasks count.
- WidgetUpdateWorker refreshes installed Glance widgets.
- WidgetScheduler enqueues a lightweight unique update work request.
- MainActivity triggers widget update after onboarding/auto-plan/reminders scheduling.
- Phase 3.5 circular initialization safety fix is included: ReminderScheduler uses lazy container access.

Out of scope

- Interactive widget buttons.
- Multiple widget sizes.
- Custom widget themes.