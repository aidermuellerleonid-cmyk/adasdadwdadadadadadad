package de.fitapp.app.util

import de.fitapp.app.data.entity.Gender
import de.fitapp.app.data.entity.MuscleGroup

/**
 * Grobe Schätzung eines "Altersdurchschnitts" für das 1-Wiederholungs-Maximum je Muskelgruppe.
 * Das ist bewusst eine einfache Näherung (keine wissenschaftliche Referenz), die aber
 * plausible, nachvollziehbare Vergleichswerte liefert: Körpergewicht, Alter und Geschlecht
 * fließen mit ein, damit z. B. ein 90-kg-Latzug realistisch gegen "Durchschnitt für diese
 * Person" bewertet wird statt gegen einen starren Fixwert.
 */
object StrengthBenchmark {

    // Referenzwert: geschätztes 1RM (kg) einer 75 kg schweren, 30-jährigen, durchschnittlich
    // trainierten Person je Muskelgruppe (typische Übung in Klammern).
    private val referenceOneRmAt75kgAge30: Map<MuscleGroup, Float> = mapOf(
        MuscleGroup.BRUST to 55f,      // Bankdrücken
        MuscleGroup.RUECKEN to 60f,    // Latzug / Rudern
        MuscleGroup.BEINE to 85f,      // Beinpresse / Kniebeuge
        MuscleGroup.SCHULTERN to 38f,  // Schulterdrücken
        MuscleGroup.ARME to 32f,       // Bizeps-/Trizepsübungen
        MuscleGroup.BAUCH to 28f       // Kabelzug-Crunch mit Zusatzgewicht
    )

    /**
     * Geschätztes durchschnittliches 1-Wiederholungs-Maximum (kg) für die gegebene Muskelgruppe,
     * angepasst an Körpergewicht, Alter und Geschlecht der Person.
     */
    fun averageOneRepMax(group: MuscleGroup, bodyWeightKg: Float, age: Int, gender: Gender): Float {
        val reference = referenceOneRmAt75kgAge30[group] ?: 45f

        // Kraft skaliert grob (aber nicht 1:1) mit dem Körpergewicht.
        val weightFactor = (bodyWeightKg / 75f).coerceIn(0.55f, 1.8f)

        // Durchschnittliche Kraft steigt bis Mitte 20 leicht an und nimmt danach langsam ab.
        val ageFactor = when {
            age < 18 -> 0.75f
            age <= 26 -> 0.9f + (age - 18) * 0.0125f
            age <= 32 -> 1.0f
            else -> (1f - (age - 32) * 0.007f).coerceAtLeast(0.5f)
        }

        // Grobe Näherung für den durchschnittlichen Kraftunterschied zwischen den Geschlechtern.
        val genderFactor = when (gender) {
            Gender.FEMALE -> 0.68f
            Gender.MALE -> 1.0f
            Gender.DIVERS -> 0.85f
        }

        return (reference * weightFactor * ageFactor * genderFactor).coerceAtLeast(5f)
    }
}
