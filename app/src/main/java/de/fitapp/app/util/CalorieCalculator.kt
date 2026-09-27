package de.fitapp.app.util

import de.fitapp.app.data.entity.ActivityLevel
import de.fitapp.app.data.entity.Gender
import de.fitapp.app.data.entity.Goal
import kotlin.math.roundToInt

data class CalorieEstimate(
    val bmr: Int,
    val maintenanceKcal: Int,
    val goalKcal: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val fiberG: Int,
    val sugarLimitG: Int
)

/**
 * Schätzt Grundumsatz (Mifflin-St Jeor) und Erhaltungskalorien und leitet daraus
 * einen Zielwert je nach persönlichem Ziel ab. Alle Werte sind Näherungswerte,
 * das wird der Nutzerin/dem Nutzer in der Oberfläche transparent gemacht.
 */
object CalorieCalculator {

    data class Macros(val proteinG: Int, val carbsG: Int, val fatG: Int)

    /**
     * Leitet aus einem (ggf. manuell angepassten) Kalorienziel passende Makro-Werte ab,
     * damit sich z. B. Eiweiß/Kohlenhydrate/Fett automatisch anpassen, wenn nur die
     * Kalorien geändert werden. Eiweiß bleibt an das Körpergewicht gekoppelt (wichtig für
     * Muskelerhalt/-aufbau unabhängig vom Kalorienziel), Fett wird als fester Anteil der
     * Kalorien berechnet, Kohlenhydrate füllen den Rest auf.
     */
    fun macrosForCalories(calorieGoal: Int, weightKg: Float, goal: Goal): Macros {
        val proteinPerKg = when (goal) {
            Goal.GAIN_MUSCLE -> 1.8
            Goal.LOSE_WEIGHT -> 1.6
            else -> 1.2
        }
        val proteinG = (proteinPerKg * weightKg).roundToInt()
        val proteinKcal = proteinG * 4
        val fatG = (calorieGoal * 0.28 / 9).roundToInt()
        val fatKcal = fatG * 9
        val carbsKcal = (calorieGoal - proteinKcal - fatKcal).toDouble().coerceAtLeast(0.0)
        val carbsG = (carbsKcal / 4).roundToInt()
        return Macros(proteinG, carbsG, fatG)
    }

    fun estimate(
        age: Int,
        heightCm: Int,
        weightKg: Float,
        gender: Gender,
        activityLevel: ActivityLevel,
        goal: Goal
    ): CalorieEstimate {
        // Mifflin-St Jeor Formel; bei "divers" wird der Mittelwert aus männlicher
        // und weiblicher Formel verwendet, da es keine etablierte dritte Formel gibt.
        val male = 10 * weightKg + 6.25 * heightCm - 5 * age + 5
        val female = 10 * weightKg + 6.25 * heightCm - 5 * age - 161
        val bmr = when (gender) {
            Gender.MALE -> male
            Gender.FEMALE -> female
            Gender.DIVERS -> (male + female) / 2.0
        }

        val maintenance = bmr * activityLevel.factor

        val goalKcal = when (goal) {
            Goal.LOSE_WEIGHT -> maintenance - 500 // ca. 0,5 kg/Woche, moderates Defizit
            Goal.GAIN_MUSCLE -> maintenance + 300 // leichter Überschuss für Muskelaufbau
            Goal.STAY, Goal.GENERAL_FITNESS -> maintenance
        }.coerceAtLeast(1200.0).roundToInt() // Sicherheitsuntergrenze gegen unrealistisch niedrige Ziele

        val macros = macrosForCalories(goalKcal, weightKg, goal)

        return CalorieEstimate(
            bmr = bmr.roundToInt(),
            maintenanceKcal = maintenance.roundToInt(),
            goalKcal = goalKcal,
            proteinG = macros.proteinG,
            carbsG = macros.carbsG,
            fatG = macros.fatG,
            fiberG = 30,
            sugarLimitG = 50
        )
    }
}
