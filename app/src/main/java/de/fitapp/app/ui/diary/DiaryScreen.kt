package de.fitapp.app.ui.diary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.fitapp.app.data.entity.DiaryEntryEntity
import de.fitapp.app.data.entity.FoodSource
import de.fitapp.app.data.entity.MealType
import de.fitapp.app.ui.AppViewModel

@Composable
fun DiaryScreen(viewModel: AppViewModel) {
    val profile by viewModel.profile.collectAsState()
    val entries by viewModel.todayEntries.collectAsState()
    var showAddDialog by remember { mutableStateOf<MealType?>(null) }

    val totals = remember(entries) {
        Totals(
            kcal = entries.sumOf { it.kcal.toDouble() }.toFloat(),
            protein = entries.sumOf { it.protein.toDouble() }.toFloat(),
            carbs = entries.sumOf { it.carbs.toDouble() }.toFloat(),
            sugar = entries.sumOf { it.sugar.toDouble() }.toFloat(),
            fat = entries.sumOf { it.fat.toDouble() }.toFloat(),
            fiber = entries.sumOf { it.fiber.toDouble() }.toFloat()
        )
    }

    Scaffold(topBar = { LargeTopAppBar(title = { Text("Ernährungstagebuch", fontWeight = FontWeight.Bold) }) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item { DailyBalanceCard(totals, profile?.dailyCalorieGoal ?: 2200, profile?.dailyProteinGoalG ?: 0, profile?.dailyCarbsGoalG ?: 0, profile?.dailySugarLimitG ?: 50, profile?.dailyFatGoalG ?: 0, profile?.dailyFiberGoalG ?: 30) }

            items(MealType.values().toList()) { meal ->
                MealSection(
                    meal = meal,
                    entries = entries.filter { it.mealType == meal },
                    onAdd = { showAddDialog = meal },
                    onDelete = { viewModel.deleteDiaryEntry(it) }
                )
            }
        }
    }

    showAddDialog?.let { meal ->
        AddFoodDialog(
            viewModel = viewModel,
            mealType = meal,
            onDismiss = { showAddDialog = null }
        )
    }
}

private data class Totals(
    val kcal: Float, val protein: Float, val carbs: Float,
    val sugar: Float, val fat: Float, val fiber: Float
)

@Composable
private fun DailyBalanceCard(
    totals: Totals, kcalGoal: Int, proteinGoal: Int, carbsGoal: Int,
    sugarLimit: Int, fatGoal: Int, fiberGoal: Int
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Heutige Bilanz", style = MaterialTheme.typography.titleMedium)
            NutrientRow("Kalorien", totals.kcal, kcalGoal.toFloat(), "kcal")
            NutrientRow("Eiweiß", totals.protein, proteinGoal.toFloat(), "g")
            NutrientRow("Kohlenhydrate", totals.carbs, carbsGoal.toFloat(), "g")
            NutrientRow("davon Zucker", totals.sugar, sugarLimit.toFloat(), "g", isLimit = true)
            NutrientRow("Fett", totals.fat, fatGoal.toFloat(), "g")
            NutrientRow("Ballaststoffe", totals.fiber, fiberGoal.toFloat(), "g")
        }
    }
}

@Composable
private fun NutrientRow(label: String, value: Float, goal: Float, unit: String, isLimit: Boolean = false) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label)
            Text("${value.toInt()} / ${goal.toInt()} $unit")
        }
        val progress = if (goal > 0) (value / goal).coerceIn(0f, 1f) else 0f
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            color = if (isLimit && value > goal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
    }
}

private fun MealType.label() = when (this) {
    MealType.FRUEHSTUECK -> "Frühstück"
    MealType.MITTAGESSEN -> "Mittagessen"
    MealType.ABENDESSEN -> "Abendessen"
    MealType.SNACK -> "Snacks"
}

@Composable
private fun MealSection(
    meal: MealType,
    entries: List<DiaryEntryEntity>,
    onAdd: () -> Unit,
    onDelete: (DiaryEntryEntity) -> Unit
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(meal.label(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                IconButton(onClick = onAdd) { Icon(Icons.Filled.Add, contentDescription = "Lebensmittel zu ${meal.label()} hinzufügen") }
            }
            if (entries.isEmpty()) {
                Text("Noch nichts eingetragen.", style = MaterialTheme.typography.bodySmall)
            } else {
                entries.forEach { entry ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("${entry.foodName} (${entry.gramAmount.toInt()} g)")
                            Text(
                                "${entry.kcal.toInt()} kcal · ${qualityLabel(entry.valueQuality)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onDelete(entry) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Eintrag löschen")
                        }
                    }
                }
            }
        }
    }
}

private fun qualityLabel(source: FoodSource): String = when (source) {
    FoodSource.LOCAL_DB -> "aus Datenbank"
    FoodSource.BARCODE_ONLINE -> "aus Online-Produktdatenbank"
    FoodSource.MANUAL -> "manuell eingegeben"
    FoodSource.CUSTOM -> "eigener Eintrag"
}
