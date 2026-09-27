package de.fitapp.app.util

import java.time.LocalDate

/**
 * Ein Tag gilt als "guter Schlaf-Tag", wenn die protokollierte Schlafdauer mindestens
 * MIN_HEALTHY_SLEEP_MINUTES erreicht. Tage ohne Eintrag zählen bewusst NICHT automatisch
 * als erreicht – analog zum Kalorienziel-Streak in StreakCalculator.
 */
object SleepStreakCalculator {

    const val MIN_HEALTHY_SLEEP_MINUTES = 7 * 60 // 7 Stunden

    fun calculate(
        sleepMinutesPerDay: Map<Long, Long>, // epochDay -> Schlafdauer in Minuten
        goalMinutes: Int = MIN_HEALTHY_SLEEP_MINUTES,
        today: LocalDate = LocalDate.now()
    ): StreakResult {
        val achieved = sleepMinutesPerDay.filterValues { it >= goalMinutes }.keys

        var longest = 0
        var running = 0
        var current = 0

        val sortedDays = achieved.sorted()
        var previousDay: Long? = null
        for (day in sortedDays) {
            running = if (previousDay != null && day == previousDay + 1) running + 1 else 1
            longest = maxOf(longest, running)
            previousDay = day
        }

        var cursor = today.toEpochDay()
        if (cursor !in achieved) cursor -= 1
        while (cursor in achieved) {
            current += 1
            cursor -= 1
        }

        return StreakResult(currentStreak = current, longestStreak = longest, achievedDays = achieved)
    }
}
