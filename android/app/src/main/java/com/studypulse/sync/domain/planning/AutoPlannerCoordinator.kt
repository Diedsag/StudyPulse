package com.studypulse.sync.domain.planning

import com.studypulse.sync.data.profile.ProfileRepository
import com.studypulse.sync.data.repository.StudyRepository
import kotlinx.coroutines.flow.firstOrNull

data class AutoPlanOutcome(
    val executed: Boolean,
    val skipped: Boolean,
    val reason: String?,
    val generatedSlots: Int,
    val acceptedSlots: Int
)

class AutoPlannerCoordinator(
    private val studyRepository: StudyRepository,
    private val profileRepository: ProfileRepository
) {

    suspend fun ensureTodayPlan(): Result<AutoPlanOutcome> {
        return try {
            val profile = profileRepository.currentProfile()

            if (!profile.autoPlanEnabled) {
                return Result.success(
                    AutoPlanOutcome(
                        executed = false,
                        skipped = true,
                        reason = "auto plan disabled",
                        generatedSlots = 0,
                        acceptedSlots = 0
                    )
                )
            }

            studyRepository.markMissedSlots()

            val todayResult = studyRepository.getTodaySnapshot()
            if (todayResult.isFailure) {
                return Result.failure(
                    todayResult.exceptionOrNull()
                        ?: IllegalStateException("Unable to load today snapshot")
                )
            }

            val today = todayResult.getOrThrow()

            if (today.todaySlots.isNotEmpty()) {
                return Result.success(
                    AutoPlanOutcome(
                        executed = false,
                        skipped = true,
                        reason = "today plan already exists",
                        generatedSlots = 0,
                        acceptedSlots = 0
                    )
                )
            }

            val activeTasks = studyRepository.tasks.firstOrNull().orEmpty()

            if (activeTasks.isEmpty()) {
                return Result.success(
                    AutoPlanOutcome(
                        executed = false,
                        skipped = true,
                        reason = "no active tasks",
                        generatedSlots = 0,
                        acceptedSlots = 0
                    )
                )
            }

            val generationResult = studyRepository.generatePlan()
            if (generationResult.isFailure) {
                return Result.failure(
                    generationResult.exceptionOrNull()
                        ?: IllegalStateException("Unable to generate plan")
                )
            }

            val generation = generationResult.getOrThrow()

            if (generation.slotsCount == 0) {
                return Result.success(
                    AutoPlanOutcome(
                        executed = true,
                        skipped = false,
                        reason = "no slots generated",
                        generatedSlots = 0,
                        acceptedSlots = 0
                    )
                )
            }

            val acceptanceResult = studyRepository.acceptPlan()
            if (acceptanceResult.isFailure) {
                return Result.failure(
                    acceptanceResult.exceptionOrNull()
                        ?: IllegalStateException("Unable to accept plan")
                )
            }

            val accepted = acceptanceResult.getOrThrow()

            Result.success(
                AutoPlanOutcome(
                    executed = true,
                    skipped = false,
                    reason = null,
                    generatedSlots = generation.slotsCount,
                    acceptedSlots = accepted
                )
            )
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }
}