StudyPulse Phase 3.2 automatic planning

Goal

Remove manual plan generation and manual plan acceptance from the basic user scenario.

Added

AutoPlannerCoordinator ensures the today plan when all conditions are met:

- onboarding is completed
- auto plan is enabled
- there are no slots for today
- there are active tasks

Behavior

When triggered, the coordinator:

- marks missed slots
- checks whether today already has slots
- checks whether there are active tasks
- generates a plan
- accepts generated slots

Current limitation

This phase does not yet hide manual buttons from PlanScreen.
It also does not yet run a background daily worker.
Those steps belong to the next polishing phases.