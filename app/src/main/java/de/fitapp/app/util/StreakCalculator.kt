package de.fitapp.app.util

import java.time.LocalDate

data class StreakResult(
    val currentStreak: Int,
    val longestStreak: Int,
    val achievedDays: Set<Long> // epoch days, an denen das Kalorienziel erreicht wurde
)

/**
 * Ein Tag gilt nur dann als "erreicht", wenn tatsächlich ein Tagebucheintrag vorliegt
 * UND die geloggten Kalorien nah genug am Ziel liegen (Toleranzband). Tage ohne
 * jeglichen Eintrag zählen bewusst NICHT automatisch als erreicht.
 */
object StreakCalculator {

    private const val TOLERANCE_KCAL = 150

    fun calculate(
        kcalPerDay: Map<Long, Float>, // epochDay -> geloggte kcal (nur Tage MIT Eintrag)
        goalKcal: Int,
        today: LocalDate = LocalDate.now()
    ): StreakResult {
        val achieved = kcalPerDay.filterValues { kotlin.math.abs(it - goalKcal) <= TOLERANCE_KCAL }.keys

        var longest = 0
        var running = 0
        var current = 0

        // Längste Streak über alle bekannten Tage ermitteln, in aufsteigender Reihenfolge.
        val sortedDays = achieved.sorted()
        var previousDay: Long? = null
        for (day in sortedDays) {
            running = if (previousDay != null && day == previousDay + 1) running + 1 else 1
            longest = maxOf(longest, running)
            previousDay = day
        }

        // Aktuelle Streak: rückwärts ab heute (oder gestern, falls heute noch kein Eintrag existiert) zählen.
        var cursor = today.toEpochDay()
        if (cursor !in achieved) cursor -= 1
        while (cursor in achieved) {
            current += 1
            cursor -= 1
        }

        return StreakResult(currentStreak = current, longestStreak = longest, achievedDays = achieved)
    }
}
