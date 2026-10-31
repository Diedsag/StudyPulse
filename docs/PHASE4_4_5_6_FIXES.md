StudyPulse Phase 4.4-4.6 fixes

4.4 ScheduleScreen
- Root column now scrolls vertically.
- Weekday selector and per-card action row use FlowRow so all 7 days and all
  buttons (Cancel/Edit/Delete) remain visible and wrap instead of overflowing.
- Non-lazy rendering of occurrences is acceptable for a 7-day window and keeps
  the form fully reachable by scrolling.

4.4-import ScheduleImportScreen
- Confirmation button moved to the bottom of the screen body.
- Root container made vertically scrollable so the recognized-count and preview
  are reachable. Patch is marker-based and avoids the PowerShell -replace
  three-argument error.

4.5 ReviewScreen
- Rating buttons localized and compacted: Again/Hard/Good/Easy ->
  Snova/Trudno/Horosho/Legko with labelMedium text and a short prompt line.

4.6 Q&A
- QaViewModel.start now always calls qaService.chunkMaterial(material) before
  showing the greeting, guaranteeing chunks exist for locally added materials.
- QaService.askQuestion now iterates all extracted keywords plus a full-question
  fallback and de-duplicates chunks, instead of using only the first keyword.
- extractKeywords lowered the min word length to 2 and raised the cap to 5.