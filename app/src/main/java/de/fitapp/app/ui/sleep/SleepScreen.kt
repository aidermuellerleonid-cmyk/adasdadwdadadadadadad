package de.fitapp.app.ui.sleep

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import de.fitapp.app.data.entity.SleepEntryEntity
import de.fitapp.app.ui.AppViewModel
import de.fitapp.app.ui.common.StreakCalendarGrid
import de.fitapp.app.ui.common.StreakStat
import de.fitapp.app.util.SleepStreakCalculator
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun SleepScreen(viewModel: AppViewModel) {
    val entries by viewModel.sleepEntries.collectAsState()
    val sleepStreak by viewModel.sleepStreak.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    val dateFmt = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }

    Scaffold(
        topBar = { LargeTopAppBar(title = { Text("Schlaf", fontWeight = FontWeight.Bold) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Filled.Add, contentDescription = "Schlaf protokollieren") }
        }
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Schlafstreak", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            StreakStat("Aktuell", sleepStreak.currentStreak)
                            StreakStat("Rekord", sleepStreak.longestStreak)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("Letzte 28 Tage:", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(4.dp))
                        StreakCalendarGrid(sleepStreak.achievedDays)
                        Text(
                            "Ein Tag zählt ab mindestens ${SleepStreakCalculator.MIN_HEALTHY_SLEEP_MINUTES / 60} " +
                                "Stunden protokolliertem Schlaf. Tage ohne Eintrag zählen nicht automatisch als erreicht.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            if (entries.isEmpty()) {
                item {
                    Text("Noch keine Schlafeinträge. Tippe auf +, um deinen Schlaf zu protokollieren.")
                }
            } else {
                items(entries) { entry -> SleepRow(entry, dateFmt) }
            }
        }
    }

    if (showAdd) {
        AddSleepDialog(
            onDismiss = { showAdd = false },
            onSave = { startMinute, endMinute, day ->
                viewModel.addSleepEntry(startMinute, endMinute, day)
                showAdd = false
            }
        )
    }
}

@Composable
private fun SleepRow(entry: SleepEntryEntity, dateFmt: DateTimeFormatter) {
    val durationMinutes = (entry.wakeUpEpochMinute - entry.sleepStartEpochMinute).coerceAtLeast(0)
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(LocalDate.ofEpochDay(entry.dateEpochDay).format(dateFmt), style = MaterialTheme.typography.titleSmall)
            Text("Dauer: ${durationMinutes / 60} h ${durationMinutes % 60} min", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun AddSleepDialog(onDismiss: () -> Unit, onSave: (Long, Long, Long) -> Unit) {
    var sleepTime by remember { mutableStateOf("23:00") }
    var wakeTime by remember { mutableStateOf("07:00") }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        ElevatedCard {
            Column(Modifier.padding(16.dp)) {
                Text("Schlaf protokollieren", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(sleepTime, { sleepTime = it }, label = { Text("Einschlafzeit (HH:mm)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(wakeTime, { wakeTime = it }, label = { Text("Aufwachzeit (HH:mm)") }, modifier = Modifier.fillMaxWidth())
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss) { Text("Abbrechen") }
                    Button(onClick = {
                        try {
                            val start = LocalTime.parse(sleepTime)
                            val end = LocalTime.parse(wakeTime)
                            val today = LocalDate.now()
                            // Wenn die Einschlafzeit "später" als die Aufwachzeit ist, war es der Vorabend.
                            val startDateTime = if (start.isAfter(end)) {
                                LocalDateTime.of(today.minusDays(1), start)
                            } else {
                                LocalDateTime.of(today, start)
                            }
                            val endDateTime = LocalDateTime.of(today, end)
                            val startEpochMin = startDateTime.toEpochSecond(java.time.ZoneOffset.UTC) / 60
                            val endEpochMin = endDateTime.toEpochSecond(java.time.ZoneOffset.UTC) / 60
                            if (endEpochMin <= startEpochMin) {
                                error = "Die Aufwachzeit muss nach der Einschlafzeit liegen."
                            } else {
                                onSave(startEpochMin, endEpochMin, today.toEpochDay())
                            }
                        } catch (e: Exception) {
                            error = "Bitte Uhrzeiten im Format HH:mm eingeben."
                        }
                    }) { Text("Speichern") }
                }
            }
        }
    }
}
