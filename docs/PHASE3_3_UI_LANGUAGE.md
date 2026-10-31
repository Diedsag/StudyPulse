StudyPulse Phase 3.3 UI language cleanup

Goal

Remove technical jargon from the user-facing interface and hide developer-only controls.

Changes

- Added UserLanguageMapper that converts internal statuses and sources into human labels.
  Slot statuses: PROPOSED -> Recommended, PLANNED -> Scheduled, IN_PROGRESS -> Now, DONE -> Completed, MISSED -> Missed.
  Sources: LOCAL -> My data, PACKAGE -> Course, MATERIAL -> From material.
- PlanScreen now shows human slot status and friendlier section titles.
- MaterialsScreen button renamed from Photo of notes to Scan notes; source label uses the mapper.
- MockOcrScreen and MockOcrViewModel no longer show the words mock, demo, or chunks to the user.
- MainActivity tab Packages renamed to Courses.
- SettingsScreen split into a simple human top section (name, reminders, auto plan, soft sync status, backup)
  and a collapsible For developers section holding server URL, device UID, nickname, rating opt-in, connection check.

Out of scope for 3.3

- Removing manual plan buttons entirely (belongs to later polish).
- Real backup file storage (belongs to 3.4).
- Navigation restructure (belongs to later).