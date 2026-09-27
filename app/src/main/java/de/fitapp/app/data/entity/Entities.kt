package de.fitapp.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persönliches Profil des Nutzers. Es gibt genau eine Zeile (id = 1).
 * Wird beim Einrichtungsassistenten befüllt und ist später im Profilbereich editierbar.
 */
@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val goal: Goal = Goal.STAY,
    val age: Int = 30,
    val heightCm: Int = 175,
    val weightKg: Float = 75f,
    val targetWeightKg: Float? = null,
    val gender: Gender = Gender.DIVERS,
    val activityLevel: ActivityLevel = ActivityLevel.MODERATE,
    val trainingExperience: TrainingExperience = TrainingExperience.BEGINNER,
    val preferredTrainingDaysMask: Int = 0, // Bit 0 = Montag ... Bit 6 = Sonntag
    val dailyCalorieGoal: Int = 2200,
    val dailyProteinGoalG: Int = 120,
    val dailyCarbsGoalG: Int = 250,
    val dailySugarLimitG: Int = 50,
    val dailyFatGoalG: Int = 70,
    val dailyFiberGoalG: Int = 30,
    val onboardingCompleted: Boolean = false
)

enum class Goal { LOSE_WEIGHT, STAY, GAIN_MUSCLE, GENERAL_FITNESS }
enum class Gender { MALE, FEMALE, DIVERS }
enum class ActivityLevel(val factor: Double) {
    SEDENTARY(1.2), LIGHT(1.375), MODERATE(1.55), ACTIVE(1.725), VERY_ACTIVE(1.9)
}
enum class TrainingExperience { BEGINNER, INTERMEDIATE, ADVANCED }

/** Ein Lebensmittel/Produkt mit Nährwerten pro 100g (bzw. pro Stück bei perPieceGrams). */
@Entity(tableName = "food_item")
data class FoodItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val barcode: String? = null,
    val source: FoodSource,
    val kcalPer100g: Float,
    val proteinPer100g: Float,
    val carbsPer100g: Float,
    val sugarPer100g: Float,
    val fatPer100g: Float,
    val fiberPer100g: Float,
    val isCustom: Boolean = false
)

enum class FoodSource { LOCAL_DB, BARCODE_ONLINE, MANUAL, CUSTOM }

/** Ein protokollierter Verzehr-Eintrag im Ernährungstagebuch. */
@Entity(tableName = "diary_entry")
data class DiaryEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val foodItemId: Long,
    val foodName: String,
    val gramAmount: Float,
    val mealType: MealType,
    val dateEpochDay: Long,
    val kcal: Float,
    val protein: Float,
    val carbs: Float,
    val sugar: Float,
    val fat: Float,
    val fiber: Float,
    val valueQuality: FoodSource
)

enum class MealType { FRUEHSTUECK, MITTAGESSEN, ABENDESSEN, SNACK }

/** Eine protokollierte Übung an einem Trainingstag. */
@Entity(tableName = "exercise_log")
data class ExerciseLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseName: String,
    val muscleGroup: MuscleGroup,
    val weightKg: Float,
    val reps: Int,
    val sets: Int,
    val dateEpochDay: Long
)

enum class MuscleGroup { BRUST, BEINE, BAUCH, ARME, RUECKEN, SCHULTERN }

/** Ein Schlaf-Eintrag. */
@Entity(tableName = "sleep_entry")
data class SleepEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long, // Tag des Aufwachens
    val sleepStartEpochMinute: Long,
    val wakeUpEpochMinute: Long
)

/** Gewichtsverlauf, separat vom Profil-Feld protokolliert. */
@Entity(tableName = "weight_entry")
data class WeightEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val weightKg: Float
)
