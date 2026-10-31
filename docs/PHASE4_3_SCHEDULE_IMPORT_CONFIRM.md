StudyPulse Phase 4.3 schedule import confirmation

Goal

Add explicit confirmation to text schedule import.

Expected flow

1. User pastes schedule text.
2. App parses/recognizes lessons.
3. User sees preview count/list.
4. User presses Confirm import.
5. Lessons are saved.

Implementation

The patch automatically detects:

- ScheduleImportViewModel variable;
- UI state variable;
- parsed items collection property;
- confirm/import method in ViewModel.

It inserts a compact confirmation button that is visible only when parsed item count is greater than zero.

Safety

If the script cannot detect a safe no-argument confirm method or parsed items property, it does not modify the project and prints diagnostics.