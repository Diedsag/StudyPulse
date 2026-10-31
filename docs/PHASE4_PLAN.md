StudyPulse Phase 4 plan

Goal

Fix product UX issues found after Phase 3 and prepare the app for comfortable demonstration.

Phase 4.0 - Honest OCR placeholder

Status: applied.

Problem:
The scan notes screen looked broken and did not clearly explain that real OCR is not connected.

Fix:
MockOcrScreen now:
- explains that OCR is demo-only;
- allows selecting an image;
- shows selected file URI;
- allows entering title and subject;
- describes future OCR pipeline;
- does not pretend to perform real recognition.

Phase 4.1 - Passive stats and leaderboard refresh

Goal:
Stats screen should load automatically when opened or resumed.

Expected behavior:
- open Stats -> data loads automatically;
- return from background -> data refreshes;
- after sync -> leaderboard updates;
- manual refresh button remains optional.

Phase 4.2 - Q&A UX repair

Goal:
Questions by material must never look silently broken.

Expected behavior:
- if answer found, show answer with citations;
- if no matches, explain that material does not contain answer;
- if material has no chunks, explain that material is not indexed;
- offer general answer fallback where appropriate.

Phase 4.3 - Schedule import confirmation

Goal:
Text schedule import must have explicit confirmation.

Expected flow:
- paste text;
- recognize;
- preview parsed lessons;
- edit if needed;
- press Confirm import;
- lessons are added.

Phase 4.4 - Schedule dialog layout fix

Goal:
Add lesson dialog must be usable.

Expected behavior:
- all weekdays are visible;
- start/end fields do not overlap;
- dialog scrolls;
- save and cancel are always reachable.

Recommended UI:
- weekday selector as horizontal chips;
- time as HH:mm text fields with validation;
- compact form layout.

Phase 4.5 - Review buttons polish

Goal:
Spaced repetition buttons should be compact and understandable.

Expected behavior:
- buttons use smaller typography;
- labels are localized: Again, Hard, Good, Easy -> Snova, Trudno, Horosho, Legko;
- buttons explain that they are recall quality ratings.