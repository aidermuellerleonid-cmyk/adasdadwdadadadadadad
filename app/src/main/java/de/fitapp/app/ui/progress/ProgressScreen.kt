package de.fitapp.app.ui.progress

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.fitapp.app.data.entity.MuscleGroup
import de.fitapp.app.ui.AppViewModel
import de.fitapp.app.ui.common.StreakCalendarGrid
import de.fitapp.app.ui.common.StreakStat
import de.fitapp.app.util.PROGRESS_WINDOW_DAYS
import java.time.LocalDate
import kotlin.math.round

private fun MuscleGroup.label() = when (this) {
    MuscleGroup.BRUST -> "Brust"
    MuscleGroup.BEINE -> "Beine"
    MuscleGroup.BAUCH -> "Bauch"
    MuscleGroup.ARME -> "Arme"
    MuscleGroup.RUECKEN -> "Rücken"
    MuscleGroup.SCHULTERN -> "Schultern"
}

/** Rundet auf eine Nachkommastelle, z. B. für die Anzeige "5.6". */
private fun Float.formatRating(): String {
    val rounded = round(this * 10f) / 10f
    return rounded.toString()
}

@Composable
fun ProgressScreen(viewModel: AppViewModel) {
    val muscleProgress by viewModel.muscleProgress.collectAsState()
    val streak by viewModel.streak.collectAsState()
    val weightEntries by viewModel.weightEntries.collectAsState()

    Scaffold(topBar = { LargeTopAppBar(title = { Text("Fortschritt", fontWeight = FontWeight.Bold) }) }) { padding ->
        LazyColumn(
            Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Muskelgruppen-Bewertung", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Bewertung von 0 bis 10 im Vergleich zum geschätzten Altersdurchschnitt " +
                                "(5.0 = Durchschnitt für dein Alter, Geschlecht und Gewicht). Es zählt " +
                                "dein bester Satz der letzten $PROGRESS_WINDOW_DAYS Tage – ein einzelner " +
                                "starker Satz schlägt also sofort aus. Grobe Schätzung, kein medizinischer " +
                                "Vergleichswert. Gruppen ohne Einträge zeigen „noch keine Daten“.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(8.dp))
                        if (muscleProgress.isNotEmpty()) {
                            MuscleRadarChart(muscleProgress) { it.label() }
                        }
                        Spacer(Modifier.height(8.dp))
                        muscleProgress.forEach { r ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(r.group.label())
                                Text(
                                    if (r.hasEnoughData) {
                                        "${r.rating.formatRating()} / 10 (${r.estimatedOneRepMaxKg.toInt()} kg, Ø ${r.averageOneRepMaxKg.toInt()} kg)"
                                    } else {
                                        "noch keine Daten"
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Kalorienziel-Streak", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            StreakStat("Aktuell", streak.currentStreak)
                            StreakStat("Rekord", streak.longestStreak)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("Letzte 28 Tage:", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(4.dp))
                        StreakCalendarGrid(streak.achievedDays)
                        Text(
                            "Tage ohne Eintrag zählen nicht automatisch als erreicht.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Gewichtsverlauf", style = MaterialTheme.typography.titleMedium)
                        if (weightEntries.isEmpty()) {
                            Text("Noch keine Gewichtseinträge. Du kannst dein Gewicht im Profil aktualisieren.")
                        } else {
                            weightEntries.take(10).forEach { w ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(LocalDate.ofEpochDay(w.dateEpochDay).toString())
                                    Text("${w.weightKg} kg")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
