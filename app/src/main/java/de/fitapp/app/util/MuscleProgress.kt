package de.fitapp.app.util

import de.fitapp.app.data.entity.ExerciseLogEntity
import de.fitapp.app.data.entity.Gender
import de.fitapp.app.data.entity.MuscleGroup
import de.fitapp.app.data.entity.UserProfile
import java.time.LocalDate
import kotlin.math.ln
import kotlin.math.min

/** Zeitraum, der in die Fortschrittsauswertung pro Muskelgruppe einfließt. */
const val PROGRESS_WINDOW_DAYS = 90L

/** Schon ein einziger protokollierter Satz reicht, damit eine Muskelgruppe bewertet wird. */
private const val MIN_SETS_FOR_DATA = 1

// log2-Skalierung der Bewertung: pro Verdopplung des Altersdurchschnitts +3.7 Punkte.
// Damit ergibt sich z. B. 5.0 = genau Durchschnitt, 8.7 = doppelter Durchschnitt.
private const val RATING_LOG2_SCALE = 3.7f
private const val RATING_BASELINE = 5.0f

data class MuscleProgressResult(
    val group: MuscleGroup,
    val hasEnoughData: Boolean,
    val rating: Float, // 0.0..10.0 – 5.0 entspricht dem geschätzten Altersdurchschnitt
    val totalSets: Int,
    val estimatedOneRepMaxKg: Float,
    val averageOneRepMaxKg: Float
) {
    /** Für Anzeigen, die weiterhin einen 0..100-Wert erwarten (z. B. das Radardiagramm). */
    val ratingPercent: Int get() = ((rating / 10f) * 100f).toInt()
}

/**
 * Bewertet jede Muskelgruppe direkt im Vergleich zu einem alters-, geschlechts- und
 * körpergewichtsabhängigen Durchschnittswert (siehe StrengthBenchmark):
 * - 5.0 = exakt der geschätzte Durchschnitt für diese Person.
 * - Jede Verdopplung des Durchschnitts erhöht die Bewertung um +3.7 (z. B. 8.7 beim Doppelten).
 * - Es zählt das BESTE geschätzte 1-Wiederholungs-Maximum im Zeitfenster – ein einzelner
 *   starker Satz hebt die Bewertung also sofort an, ohne dass mehrere Einträge nötig sind.
 */
object MuscleProgressCalculator {

    fun calculate(
        logs: List<ExerciseLogEntity>,
        profile: UserProfile?,
        today: LocalDate = LocalDate.now()
    ): List<MuscleProgressResult> {
        val sinceEpochDay = today.minusDays(PROGRESS_WINDOW_DAYS).toEpochDay()
        val age = profile?.age ?: 30
        val gender = profile?.gender ?: Gender.DIVERS
        val bodyWeightKg = profile?.weightKg ?: 75f

        return MuscleGroup.values().map { group ->
            val groupLogs = logs.filter { it.muscleGroup == group && it.dateEpochDay >= sinceEpochDay }
            val totalSets = groupLogs.sumOf { it.sets }
            val average = StrengthBenchmark.averageOneRepMax(group, bodyWeightKg, age, gender)

            if (groupLogs.isEmpty() || totalSets < MIN_SETS_FOR_DATA) {
                return@map MuscleProgressResult(
                    group = group,
                    hasEnoughData = false,
                    rating = 0f,
                    totalSets = totalSets,
                    estimatedOneRepMaxKg = 0f,
                    averageOneRepMaxKg = average
                )
            }

            val best = groupLogs.maxOf { estimateOneRepMax(it.weightKg, it.reps) }
            val ratio = (best / average).coerceAtLeast(0.05f)
            val rating = (RATING_BASELINE + RATING_LOG2_SCALE * (ln(ratio) / ln(2f))).coerceIn(0f, 10f)

            MuscleProgressResult(
                group = group,
                hasEnoughData = true,
                rating = rating,
                totalSets = totalSets,
                estimatedOneRepMaxKg = best,
                averageOneRepMaxKg = average
            )
        }
    }

    /** Epley-Formel zur Schätzung des 1-Wiederholungs-Maximums. */
    private fun estimateOneRepMax(weightKg: Float, reps: Int): Float {
        if (reps <= 0) return weightKg
        return weightKg * (1f + min(reps, 20) / 30f)
    }
}
