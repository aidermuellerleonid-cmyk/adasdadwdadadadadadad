package de.fitapp.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.fitapp.app.FitApplication
import de.fitapp.app.data.entity.*
import de.fitapp.app.data.food.BarcodeLookupResult
import de.fitapp.app.util.CalorieCalculator
import de.fitapp.app.util.MuscleProgressCalculator
import de.fitapp.app.util.MuscleProgressResult
import de.fitapp.app.util.SleepStreakCalculator
import de.fitapp.app.util.StreakCalculator
import de.fitapp.app.util.StreakResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Unterscheidet "wird noch aus der DB geladen" von "es existiert (noch) kein Profil". */
sealed class ProfileState {
    object Loading : ProfileState()
    data class Loaded(val profile: UserProfile?) : ProfileState()
}

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FitApplication
    private val db = app.database
    private val foodRepository = app.foodRepository

    val profileState: StateFlow<ProfileState> =
        db.userProfileDao().observeProfile()
            .map<UserProfile?, ProfileState> { ProfileState.Loaded(it) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, ProfileState.Loading)

    // Bequemer Zugriff auf das Profil, falls schon geladen (z. B. für Berechnungen).
    val profile: StateFlow<UserProfile?> =
        profileState
            .map { (it as? ProfileState.Loaded)?.profile }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val today: Long get() = LocalDate.now().toEpochDay()

    val todayEntries: StateFlow<List<DiaryEntryEntity>> =
        db.diaryDao().observeForDay(today)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val exerciseLogs: StateFlow<List<ExerciseLogEntity>> =
        db.exerciseDao().observeAll()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val sleepEntries: StateFlow<List<SleepEntryEntity>> =
        db.sleepDao().observeAll()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val weightEntries: StateFlow<List<WeightEntryEntity>> =
        db.weightDao().observeAll()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Fließt zusammen mit dem Profil ein, da die Bewertung Alter, Geschlecht und Körpergewicht
    // der Person berücksichtigt (siehe StrengthBenchmark).
    val muscleProgress: StateFlow<List<MuscleProgressResult>> =
        combine(exerciseLogs, profile) { logs, prof -> MuscleProgressCalculator.calculate(logs, prof) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Schlafdauer je Tag (Minuten), abgeleitet aus den protokollierten Schlafeinträgen.
    private val sleepMinutesPerDay: StateFlow<Map<Long, Long>> =
        sleepEntries.map { entries ->
            entries.associate { entry ->
                entry.dateEpochDay to (entry.wakeUpEpochMinute - entry.sleepStartEpochMinute).coerceAtLeast(0)
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val sleepStreak: StateFlow<StreakResult> = sleepMinutesPerDay
        .map { SleepStreakCalculator.calculate(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, StreakResult(0, 0, emptySet()))

    // Alle Tage mit Tagebucheintrag + Kalorien-Summe, für Streak-Berechnung und Verlauf.
    private val _kcalHistory = MutableStateFlow<Map<Long, Float>>(emptyMap())
    val kcalHistory: StateFlow<Map<Long, Float>> = _kcalHistory.asStateFlow()

    val streak: StateFlow<StreakResult> = combine(kcalHistory, profile) { history, prof ->
        StreakCalculator.calculate(history, prof?.dailyCalorieGoal ?: 2200)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, StreakResult(0, 0, emptySet()))

    init {
        viewModelScope.launch { foodRepository.ensureSeeded() }
        refreshKcalHistory()
    }

    private fun refreshKcalHistory() {
        viewModelScope.launch {
            val days = db.diaryDao().getAllLoggedDays()
            val map = mutableMapOf<Long, Float>()
            // Für eine überschaubare App reicht ein einmaliger Batch-Read je Änderung;
            // bei Bedarf könnte dies durch eine dedizierte SUM-Query pro Tag ersetzt werden.
            for (day in days) {
                val sum = db.diaryDao().observeKcalSumForDay(day).first() ?: 0f
                map[day] = sum
            }
            _kcalHistory.value = map
        }
    }

    // ---------- Profil / Onboarding ----------

    fun saveProfile(profile: UserProfile) {
        viewModelScope.launch {
            db.userProfileDao().upsert(profile)
        }
    }

    fun estimateCalories(
        age: Int, heightCm: Int, weightKg: Float,
        gender: Gender, activityLevel: ActivityLevel, goal: Goal
    ) = CalorieCalculator.estimate(age, heightCm, weightKg, gender, activityLevel, goal)

    // ---------- Ernährung ----------

    suspend fun searchFood(query: String) = foodRepository.search(query)

    suspend fun lookupBarcode(barcode: String): BarcodeLookupResult = foodRepository.lookupBarcode(barcode)

    suspend fun saveCustomFood(
        name: String, kcal: Float, protein: Float, carbs: Float, sugar: Float, fat: Float, fiber: Float
    ) = foodRepository.saveCustomFood(name, kcal, protein, carbs, sugar, fat, fiber)

    fun addDiaryEntry(food: FoodItemEntity, grams: Float, mealType: MealType) {
        val factor = grams / 100f
        viewModelScope.launch {
            db.diaryDao().insert(
                DiaryEntryEntity(
                    foodItemId = food.id,
                    foodName = food.name,
                    gramAmount = grams,
                    mealType = mealType,
                    dateEpochDay = today,
                    kcal = food.kcalPer100g * factor,
                    protein = food.proteinPer100g * factor,
                    carbs = food.carbsPer100g * factor,
                    sugar = food.sugarPer100g * factor,
                    fat = food.fatPer100g * factor,
                    fiber = food.fiberPer100g * factor,
                    valueQuality = food.source
                )
            )
            refreshKcalHistory()
        }
    }

    fun deleteDiaryEntry(entry: DiaryEntryEntity) {
        viewModelScope.launch {
            db.diaryDao().delete(entry)
            refreshKcalHistory()
        }
    }

    // ---------- Training ----------

    fun addExerciseLog(name: String, group: MuscleGroup, weightKg: Float, reps: Int, sets: Int, dateEpochDay: Long = today) {
        viewModelScope.launch {
            db.exerciseDao().insert(
                ExerciseLogEntity(
                    exerciseName = name, muscleGroup = group,
                    weightKg = weightKg, reps = reps, sets = sets, dateEpochDay = dateEpochDay
                )
            )
        }
    }

    // ---------- Schlaf ----------

    fun addSleepEntry(sleepStartEpochMinute: Long, wakeUpEpochMinute: Long, dateEpochDay: Long = today) {
        viewModelScope.launch {
            db.sleepDao().insert(
                SleepEntryEntity(
                    dateEpochDay = dateEpochDay,
                    sleepStartEpochMinute = sleepStartEpochMinute,
                    wakeUpEpochMinute = wakeUpEpochMinute
                )
            )
        }
    }

    // ---------- Gewicht ----------

    fun addWeightEntry(weightKg: Float, dateEpochDay: Long = today) {
        viewModelScope.launch {
            db.weightDao().insert(WeightEntryEntity(dateEpochDay = dateEpochDay, weightKg = weightKg))
        }
    }
}
