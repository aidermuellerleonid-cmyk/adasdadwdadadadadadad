package de.fitapp.app.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.fitapp.app.data.entity.*
import de.fitapp.app.ui.AppViewModel
import de.fitapp.app.util.CalorieCalculator

private val stepTitles = listOf(
    "Willkommen", "Ziel", "Körperdaten", "Aktivität", "Ernährungsziele", "Trainingsstand", "Startwerte"
)

private data class ExerciseBaseline(val exerciseName: String, var weightKg: String, var reps: String)

private val trainingOrder = listOf(
    MuscleGroup.BRUST, MuscleGroup.RUECKEN, MuscleGroup.BEINE,
    MuscleGroup.SCHULTERN, MuscleGroup.ARME, MuscleGroup.BAUCH
)

private fun defaultBaselines(): Map<MuscleGroup, ExerciseBaseline> = linkedMapOf(
    MuscleGroup.BRUST to ExerciseBaseline("Bankdrücken", "", ""),
    MuscleGroup.RUECKEN to ExerciseBaseline("Rudern", "", ""),
    MuscleGroup.BEINE to ExerciseBaseline("Kniebeuge", "", ""),
    MuscleGroup.SCHULTERN to ExerciseBaseline("Schulterdrücken", "", ""),
    MuscleGroup.ARME to ExerciseBaseline("Bizepscurls", "", ""),
    MuscleGroup.BAUCH to ExerciseBaseline("Crunches", "", "")
)

private fun muscleGroupLabel(group: MuscleGroup) = when (group) {
    MuscleGroup.BRUST -> "Brust"
    MuscleGroup.BEINE -> "Beine"
    MuscleGroup.BAUCH -> "Bauch"
    MuscleGroup.ARME -> "Arme"
    MuscleGroup.RUECKEN -> "Rücken"
    MuscleGroup.SCHULTERN -> "Schultern"
}

@Composable
fun OnboardingScreen(viewModel: AppViewModel) {
    var step by remember { mutableIntStateOf(0) }

    var goal by remember { mutableStateOf(Goal.STAY) }
    var age by remember { mutableStateOf("30") }
    var heightCm by remember { mutableStateOf("175") }
    var weightKg by remember { mutableStateOf("75") }
    var targetWeightKg by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf(Gender.DIVERS) }
    var activity by remember { mutableStateOf(ActivityLevel.MODERATE) }
    var experience by remember { mutableStateOf(TrainingExperience.BEGINNER) }

    var calorieGoal by remember { mutableStateOf(2200) }
    var proteinGoal by remember { mutableStateOf(120) }
    var carbsGoal by remember { mutableStateOf(250) }
    var fatGoal by remember { mutableStateOf(70) }

    val baselines = remember { mutableStateMapOf<MuscleGroup, ExerciseBaseline>().apply { putAll(defaultBaselines()) } }

    fun applyMacrosForCalories(newCalorieGoal: Int) {
        calorieGoal = newCalorieGoal
        val w = weightKg.toFloatOrNull() ?: 75f
        val macros = CalorieCalculator.macrosForCalories(newCalorieGoal, w, goal)
        proteinGoal = macros.proteinG
        carbsGoal = macros.carbsG
        fatGoal = macros.fatG
    }

    fun applyEstimateIfPossible() {
        val a = age.toIntOrNull() ?: return
        val h = heightCm.toIntOrNull() ?: return
        val w = weightKg.toFloatOrNull() ?: return
        val estimate = viewModel.estimateCalories(a, h, w, gender, activity, goal)
        calorieGoal = estimate.goalKcal
        proteinGoal = estimate.proteinG
        carbsGoal = estimate.carbsG
        fatGoal = estimate.fatG
    }

    fun finish(skippedRest: Boolean = false) {
        val profile = UserProfile(
            goal = goal,
            age = age.toIntOrNull() ?: 30,
            heightCm = heightCm.toIntOrNull() ?: 175,
            weightKg = weightKg.toFloatOrNull() ?: 75f,
            targetWeightKg = targetWeightKg.toFloatOrNull(),
            gender = gender,
            activityLevel = activity,
            trainingExperience = experience,
            dailyCalorieGoal = calorieGoal,
            dailyProteinGoalG = proteinGoal,
            dailyCarbsGoalG = carbsGoal,
            dailyFatGoalG = fatGoal,
            onboardingCompleted = true
        )
        viewModel.saveProfile(profile)

        // Trainings-Startwerte speichern, damit das Fortschritts-Radardiagramm von Anfang
        // an Daten hat, statt erst nach mehreren Trainingseinheiten etwas anzuzeigen.
        baselines.forEach { (group, baseline) ->
            val w = baseline.weightKg.toFloatOrNull()
            val r = baseline.reps.toIntOrNull()
            if (w != null && w > 0f && r != null && r > 0) {
                viewModel.addExerciseLog(
                    name = baseline.exerciseName,
                    group = group,
                    weightKg = w,
                    reps = r,
                    sets = 3
                )
            }
        }
    }

    Scaffold(
        topBar = { LargeTopAppBar(title = { Text("Einrichtung – ${stepTitles[step]}", fontWeight = FontWeight.Bold) }) },
        bottomBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { finish(skippedRest = true) }) {
                    Text(if (step == stepTitles.lastIndex) "Fertig" else "Überspringen")
                }
                Button(onClick = {
                    if (step == 1) applyEstimateIfPossible()
                    if (step < stepTitles.lastIndex) step++ else finish()
                }) {
                    Text(if (step == stepTitles.lastIndex) "Los geht's" else "Weiter")
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (step) {
                0 -> {
                    Text(
                        "Willkommen bei FitApp!",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        "In wenigen Schritten richten wir deine App ein. Wir fragen nach deinem " +
                            "Ziel, ein paar Körperdaten und deinem Trainingsstand. Damit können wir " +
                            "passende Kalorien- und Trainingsvorschläge berechnen. Du kannst jeden " +
                            "Schritt überspringen und später im Profil ändern."
                    )
                }
                1 -> {
                    Text("Was ist dein Hauptziel?", style = MaterialTheme.typography.titleMedium)
                    GoalOption("Abnehmen", goal == Goal.LOSE_WEIGHT) { goal = Goal.LOSE_WEIGHT }
                    GoalOption("Gewicht halten", goal == Goal.STAY) { goal = Goal.STAY }
                    GoalOption("Muskeln aufbauen", goal == Goal.GAIN_MUSCLE) { goal = Goal.GAIN_MUSCLE }
                    GoalOption("Allgemein fitter werden", goal == Goal.GENERAL_FITNESS) { goal = Goal.GENERAL_FITNESS }
                }
                2 -> {
                    Text("Deine Körperdaten", style = MaterialTheme.typography.titleMedium)
                    Text("Diese Angaben werden nur für die Berechnung deines Kalorienbedarfs verwendet.")
                    OutlinedTextField(age, { age = it }, label = { Text("Alter (Jahre)") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(heightCm, { heightCm = it }, label = { Text("Größe (cm)") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(weightKg, { weightKg = it }, label = { Text("Aktuelles Gewicht (kg)") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    if (goal == Goal.LOSE_WEIGHT || goal == Goal.GAIN_MUSCLE) {
                        OutlinedTextField(targetWeightKg, { targetWeightKg = it }, label = { Text("Zielgewicht (kg, optional)") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    }
                    Text("Geschlecht (für die Berechnung des Grundumsatzes):")
                    GoalOption("Männlich", gender == Gender.MALE) { gender = Gender.MALE }
                    GoalOption("Weiblich", gender == Gender.FEMALE) { gender = Gender.FEMALE }
                    GoalOption("Divers", gender == Gender.DIVERS) { gender = Gender.DIVERS }
                }
                3 -> {
                    Text("Wie aktiv bist du im Alltag?", style = MaterialTheme.typography.titleMedium)
                    GoalOption("Überwiegend sitzend", activity == ActivityLevel.SEDENTARY) { activity = ActivityLevel.SEDENTARY }
                    GoalOption("Leicht aktiv (wenig Bewegung)", activity == ActivityLevel.LIGHT) { activity = ActivityLevel.LIGHT }
                    GoalOption("Moderat aktiv", activity == ActivityLevel.MODERATE) { activity = ActivityLevel.MODERATE }
                    GoalOption("Sehr aktiv", activity == ActivityLevel.ACTIVE) { activity = ActivityLevel.ACTIVE }
                    GoalOption("Extrem aktiv (körperliche Arbeit/Leistungssport)", activity == ActivityLevel.VERY_ACTIVE) { activity = ActivityLevel.VERY_ACTIVE }
                }
                4 -> {
                    LaunchedEffect(Unit) { applyEstimateIfPossible() }
                    Text("Deine Tagesziele", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Basierend auf deinen Angaben schlagen wir folgende Werte vor – ein " +
                            "Näherungswert, den du jederzeit anpassen kannst. Wenn du die Kalorien " +
                            "änderst, passen sich Eiweiß/Kohlenhydrate/Fett automatisch an."
                    )
                    NumberField("Kalorien (kcal)", calorieGoal) { applyMacrosForCalories(it) }
                    NumberField("Eiweiß (g)", proteinGoal) { proteinGoal = it }
                    NumberField("Kohlenhydrate (g)", carbsGoal) { carbsGoal = it }
                    NumberField("Fett (g)", fatGoal) { fatGoal = it }
                }
                5 -> {
                    Text("Trainingserfahrung", style = MaterialTheme.typography.titleMedium)
                    GoalOption("Anfänger:in", experience == TrainingExperience.BEGINNER) { experience = TrainingExperience.BEGINNER }
                    GoalOption("Fortgeschritten", experience == TrainingExperience.INTERMEDIATE) { experience = TrainingExperience.INTERMEDIATE }
                    GoalOption("Erfahren", experience == TrainingExperience.ADVANCED) { experience = TrainingExperience.ADVANCED }
                }
                6 -> {
                    Text("Deine aktuellen Werte pro Muskelgruppe", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Trag zu jeder Gruppe eine Übung mit dem Gewicht und den Wiederholungen ein, " +
                            "die du aktuell schaffst. Damit zeigt dir dein Fortschritts-Radar von Anfang " +
                            "an etwas an, statt erst nach mehreren Trainingseinheiten. Du kannst Felder " +
                            "auch leer lassen und später im Trainingsbereich nachtragen."
                    )
                    trainingOrder.forEach { group ->
                        val baseline = baselines.getValue(group)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(muscleGroupLabel(group), style = MaterialTheme.typography.titleSmall)
                            OutlinedTextField(
                                value = baseline.exerciseName,
                                onValueChange = { baselines[group] = baseline.copy(exerciseName = it) },
                                label = { Text("Übung") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = baseline.weightKg,
                                    onValueChange = { baselines[group] = baseline.copy(weightKg = it) },
                                    label = { Text("Gewicht (kg)") },
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = baseline.reps,
                                    onValueChange = { baselines[group] = baseline.copy(reps = it) },
                                    label = { Text("Wiederholungen") },
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun NumberField(label: String, value: Int, onChange: (Int) -> Unit) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { it.toIntOrNull()?.let(onChange) },
        label = { Text(label) },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}
