package de.fitapp.app.ui.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import de.fitapp.app.util.MuscleProgressResult
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Zeichnet ein Radardiagramm auf Basis der 0..10-Bewertung je Muskelgruppe (5.0 = Durchschnitt).
 * Da die Bewertung direkt aus dem besten Satz berechnet wird (siehe MuscleProgressCalculator),
 * schlägt bereits ein einzelner starker Trainingssatz sofort im Diagramm aus. Muskelgruppen
 * ohne jegliche Daten werden am Rand gestrichelt markiert statt fälschlich als 0 dargestellt.
 */
@Composable
fun MuscleRadarChart(results: List<MuscleProgressResult>, labels: (de.fitapp.app.data.entity.MuscleGroup) -> String) {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline
    val noData = MaterialTheme.colorScheme.outlineVariant

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        val n = results.size
        if (n == 0) return@Canvas
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = min(size.width, size.height) / 2f * 0.75f
        val angleStep = (2 * Math.PI / n)

        fun pointFor(index: Int, fraction: Float): Offset {
            val angle = -Math.PI / 2 + angleStep * index
            return Offset(
                x = center.x + (radius * fraction * cos(angle)).toFloat(),
                y = center.y + (radius * fraction * sin(angle)).toFloat()
            )
        }

        // Gitternetz (Bewertungsstufen 2.5 / 5.0 / 7.5 / 10.0)
        for (step in 1..4) {
            val fraction = step / 4f
            val path = androidx.compose.ui.graphics.Path()
            for (i in 0 until n) {
                val p = pointFor(i, fraction)
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            path.close()
            drawPath(path, color = outline.copy(alpha = 0.3f), style = Stroke(width = 1f))
        }

        // Achsen
        for (i in 0 until n) {
            val p = pointFor(i, 1f)
            drawLine(outline.copy(alpha = 0.4f), center, p, strokeWidth = 1f)
        }

        // Datenpolygon (nur Gruppen mit Daten fließen mit ihrem Wert ein, sonst 0-Marker gestrichelt).
        // Die Bewertung (0..10) wird 1:1 auf den Radius abgebildet, damit ein einzelner starker
        // Satz sofort sichtbar nach außen ausschlägt statt sich erst langsam "hochzuarbeiten".
        val dataPath = androidx.compose.ui.graphics.Path()
        results.forEachIndexed { i, r ->
            val fraction = if (r.hasEnoughData) (r.rating / 10f).coerceIn(0f, 1f) else 0.08f
            val p = pointFor(i, fraction)
            if (i == 0) dataPath.moveTo(p.x, p.y) else dataPath.lineTo(p.x, p.y)
        }
        dataPath.close()
        drawPath(dataPath, color = primary.copy(alpha = 0.25f))
        drawPath(dataPath, color = primary, style = Stroke(width = 3f))

        results.forEachIndexed { i, r ->
            val fraction = if (r.hasEnoughData) (r.rating / 10f).coerceIn(0f, 1f) else 0.08f
            val p = pointFor(i, fraction)
            drawCircle(
                color = if (r.hasEnoughData) primary else noData,
                radius = 6f,
                center = p
            )
        }
    }
}
