StudyPulse Phase 4.1 passive stats refresh

Goal

Make the Stats screen load data automatically, without requiring the user to press Refresh every time.

What changed

StatsScreen now triggers the existing stats loading method automatically:

- when the screen is first composed;
- when the app returns to RESUMED state.

The script detects the actual ViewModel variable name and the actual refresh/load method from the repository, then inserts a lifecycle-aware LaunchedEffect block.

Expected user behavior

Open Stats:
- stats and leaderboard start loading immediately.

Return to app from background:
- stats refresh automatically.

Manual Refresh button:
- still available, but no longer required for the basic flow.

Safety

If the script cannot find a safe no-argument or default-argument refresh method, it does not modify the project and prints diagnostics.