StudyPulse Phase 3.4 - Background sync and backup

Goal

Make synchronization automatic and invisible to the user.
Provide simple local backup and restore functionality.

Added

1. SyncWorker
   - Periodic background synchronization via WorkManager
   - Runs every 4 hours
   - Exponential backoff on failure

2. SyncScheduler
   - Schedules periodic sync
   - Triggers immediate sync when needed

3. BackupManager
   - Creates local backup files in app storage
   - Keeps last 5 backups
   - Restores from backup
   - Lists available backups

4. StudyPulseApplication
   - Schedules sync on app startup

5. SettingsScreen updates
   - Shows backup list with date and size
   - Create backup button
   - Restore dialog
   - Delete backup option

User experience

- Sync happens automatically in background
- User sees soft status: "Progress will sync automatically"
- Backup is created on demand
- Restore requires confirmation dialog
- Old backups are cleaned up automatically

Out of scope

- Cloud backup
- End-to-end encryption
- Multi-device sync
- Automatic backup (manual for now)