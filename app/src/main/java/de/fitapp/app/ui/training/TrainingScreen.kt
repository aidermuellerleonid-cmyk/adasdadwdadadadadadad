@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package de.fitapp.app.ui.training

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import de.fitapp.app.data.entity.ExerciseLogEntity
import de.fitapp.app.data.entity.MuscleGroup
import de.fitapp.app.ui.AppViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val commonExercises = mapOf(
    "Bankdrücken" to MuscleGroup.BRUST,
    "Kniebeuge" to MuscleGroup.BEINE,
    "Kreuzheben" to MuscleGroup.RUECKEN,
    "Klimmzug" to MuscleGroup.RUECKEN,
    "Schulterdrücken" to MuscleGroup.SCHULTERN,
    "Bizepscurls" to MuscleGroup.ARME,
    "Trizepsdrücken" to MuscleGroup.ARME,
    "Crunches" to MuscleGroup.BAUCH,
    "Beinpresse" to MuscleGroup.BEINE
)

private fun MuscleGroup.label() = when (this) {
    MuscleGroup.BRUST -> "Brust"
    MuscleGroup.BEINE -> "Beine"
    MuscleGroup.BAUCH -> "Bauch"
    MuscleGroup.ARME -> "Arme"
    MuscleGroup.RUECKEN -> "Rücken"
    MuscleGroup.SCHULTERN -> "Schultern"
}

@Composable
fun TrainingScreen(viewModel: AppViewModel) {
    val logs by viewModel.exerciseLogs.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    val formatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }

    Scaffold(
        topBar = { LargeTopAppBar(title = { Text("Training", fontWeight = FontWeight.Bold) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Filled.Add, contentDescription = "Übung hinzufügen") }
        }
    ) { padding ->
        if (logs.isEmpty()) {
            Column(Modifier.padding(padding).padding(24.dp)) {
                Text("Noch keine Trainingseinträge. Tippe auf +, um deine erste Übung zu protokollieren.")
            }
        } else {
            LazyColumn(
                Modifier.padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs) { log -> ExerciseRow(log, formatter) }
            }
        }
    }

    if (showAdd) {
        AddExerciseDialog(
            onDismiss = { showAdd = false },
            onSave = { name, group, weight, reps, sets ->
                viewModel.addExerciseLog(name, group, weight, reps, sets)
                showAdd = false
            }
        )
    }
}

@Composable
private fun ExerciseRow(log: ExerciseLogEntity, formatter: DateTimeFormatter) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("${log.exerciseName} · ${log.muscleGroup.label()}", style = MaterialTheme.typography.titleSmall)
            Text(
                "${log.weightKg} kg × ${log.reps} Wdh. × ${log.sets} Sätze · " +
                    LocalDate.ofEpochDay(log.dateEpochDay).format(formatter),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun AddExerciseDialog(
    onDismiss: () -> Unit,
    onSave: (String, MuscleGroup, Float, Int, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var group by remember { mutableStateOf(MuscleGroup.BRUST) }
    var weight by remember { mutableStateOf("") }
    var reps by remember { mutableStateOf("") }
    var sets by remember { mutableStateOf("3") }
    var expanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        ElevatedCard {
            Column(Modifier.padding(16.dp)) {
                Text("Übung protokollieren", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Übungsname") },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        commonExercises.forEach { (exName, exGroup) ->
                            DropdownMenuItem(text = { Text("$exName (${exGroup.label()})") }, onClick = {
                                name = exName; group = exGroup; expanded = false
                            })
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text("Muskelgruppe:")
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    MuscleGroup.values().forEach { g ->
                        FilterChip(selected = group == g, onClick = { group = g }, label = { Text(g.label()) })
                    }
                }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(weight, { weight = it }, label = { Text("Gewicht (kg)") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(reps, { reps = it }, label = { Text("Wiederholungen") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(sets, { sets = it }, label = { Text("Sätze") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss) { Text("Abbrechen") }
                    Button(
                        onClick = {
                            onSave(name.trim(), group, weight.toFloatOrNull() ?: 0f, reps.toIntOrNull() ?: 0, sets.toIntOrNull() ?: 1)
                        },
                        enabled = name.isNotBlank() && weight.toFloatOrNull() != null && reps.toIntOrNull() != null
                    ) { Text("Speichern") }
                }
            }
        }
    }
}
