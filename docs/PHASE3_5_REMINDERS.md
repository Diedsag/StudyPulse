StudyPulse Phase 3.5 - Precise reminders

Goal

Replace the coarse 15-minute periodic worker with exact, per-event reminders
that respect user preferences and quiet hours.

What changed

- StudentProfile extended with slotRemindersEnabled, deadlineRemindersEnabled,
  morningDigestEnabled, quietHoursEnabled, quietHoursStart, quietHoursEnd.
- New workers: SlotReminderWorker (fires 15 min before a planned slot),
  DeadlineReminderWorker (fires 24h and 3h before a deadline),
  MorningDigestWorker (daily 07:00 summary of today plan).
- ReminderScheduler plans one-time WorkManager requests with exact delays and
  a daily periodic digest. Old NotificationScheduler periodic call removed
  from StudyPulseApplication.
- Quiet hours are checked at delivery time inside each worker; if active,
  the notification is skipped (not delayed) to keep the implementation simple.
- MainActivity schedules slot and deadline reminders after onboarding and
  after auto-plan, and re-schedules when notifications toggle changes.
- SettingsScreen gained a "Reminder types" card with per-type switches and
  quiet-hours counters, visible only when notifications are enabled.

Known limitations

- Reminders are scheduled at app open / plan change, not reactively on every
  task edit from TasksScreen. Full reactive rescheduling belongs to later polish.
- Quiet hours skip rather than postpone the notification.