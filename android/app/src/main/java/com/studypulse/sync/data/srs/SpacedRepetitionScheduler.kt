package com.studypulse.sync.data.srs

import com.studypulse.sync.data.local.entity.CardEntity

object SpacedRepetitionScheduler {

    const val AGAIN = 1
    const val HARD = 2
    const val GOOD = 3
    const val EASY = 4

    private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
    private const val AGAIN_DELAY_MILLIS = 10L * 60L * 1000L

    fun review(
        card: CardEntity,
        rating: Int,
        now: Long
    ): CardEntity {
        val currentEase = card.easeFactor.coerceIn(1.3, 3.0)

        return when (rating) {
            AGAIN -> {
                card.copy(
                    repetitions = 0,
                    lapses = card.lapses + 1,
                    intervalDays = 0,
                    easeFactor = (currentEase - 0.2).coerceIn(1.3, 3.0),
                    dueAt = now + AGAIN_DELAY_MILLIS,
                    updatedAt = now
                )
            }

            HARD -> {
                val newInterval = if (card.intervalDays <= 0) {
                    1
                } else {
                    (card.intervalDays * 1.2).toInt().coerceAtLeast(1)
                }

                card.copy(
                    repetitions = card.repetitions + 1,
                    intervalDays = newInterval,
                    easeFactor = (currentEase - 0.15).coerceIn(1.3, 3.0),
                    dueAt = now + newInterval * DAY_MILLIS,
                    updatedAt = now
                )
            }

            GOOD -> {
                val newInterval = when (card.repetitions) {
                    0 -> 1
                    1 -> 3
                    else -> (card.intervalDays * currentEase).toInt().coerceAtLeast(1)
                }

                card.copy(
                    repetitions = card.repetitions + 1,
                    intervalDays = newInterval,
                    easeFactor = currentEase,
                    dueAt = now + newInterval * DAY_MILLIS,
                    updatedAt = now
                )
            }

            EASY -> {
                val newInterval = when (card.repetitions) {
                    0 -> 3
                    1 -> 5
                    else -> (card.intervalDays * currentEase * 1.3).toInt().coerceAtLeast(1)
                }

                card.copy(
                    repetitions = card.repetitions + 1,
                    intervalDays = newInterval,
                    easeFactor = (currentEase + 0.15).coerceIn(1.3, 3.0),
                    dueAt = now + newInterval * DAY_MILLIS,
                    updatedAt = now
                )
            }

            else -> card.copy(updatedAt = now)
        }
    }
}