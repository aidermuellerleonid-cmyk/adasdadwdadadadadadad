package de.fitapp.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate

/** Kompakte Kennzahl (z. B. "Aktuell: 4") für Streak-Karten. */
@Composable
fun StreakStat(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$value", style = MaterialTheme.typography.headlineMedium)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

/** Kalendergitter der letzten 28 Tage, hervorgehobene Kreise für erreichte Tage. */
@Composable
fun StreakCalendarGrid(achievedDays: Set<Long>) {
    val today = LocalDate.now().toEpochDay()
    val days = (0..27).map { today - (27 - it) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.height(140.dp)
    ) {
        items(days) { day ->
            val achieved = day in achievedDays
            Box(
                Modifier
                    .padding(2.dp)
                    .size(16.dp)
                    .background(
                        color = if (achieved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    )
            )
        }
    }
}
