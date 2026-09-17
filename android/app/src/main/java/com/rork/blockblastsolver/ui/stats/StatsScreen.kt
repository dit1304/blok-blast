package com.rork.blockblastsolver.ui.stats

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rork.blockblastsolver.domain.Achievement
import com.rork.blockblastsolver.domain.StatsSummary
import com.rork.blockblastsolver.ui.components.IconTile
import com.rork.blockblastsolver.ui.components.PanelCard
import com.rork.blockblastsolver.ui.components.SectionHeader
import com.rork.blockblastsolver.ui.theme.Divider
import com.rork.blockblastsolver.ui.theme.NeonGold
import com.rork.blockblastsolver.ui.theme.NeonPink
import com.rork.blockblastsolver.ui.theme.NeonTeal
import com.rork.blockblastsolver.ui.theme.SurfaceElevated
import com.rork.blockblastsolver.ui.theme.SurfaceElevatedHigh
import com.rork.blockblastsolver.ui.theme.TextPrimary
import com.rork.blockblastsolver.ui.theme.TextSecondary
import com.rork.blockblastsolver.util.formatScore
import kotlin.math.max

/** Statistik tab: progress derived entirely from saved turns. */
@Composable
fun StatsScreen(
    stats: StatsSummary,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 4.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp
            )
    ) {
        Text(
            text = "Statistik",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary
        )
        Text(
            text = "Lihat progres dan pencapaian kamu",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )

        Spacer(Modifier.height(16.dp))

        PanelCard(contentPadding = 16) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(background = NeonTeal.copy(alpha = 0.14f)) {
                    Icon(
                        Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = NeonTeal,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Total Skor Terkumpul",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                    Text(
                        text = formatScore(stats.totalScore),
                        style = MaterialTheme.typography.displayMedium,
                        color = TextPrimary
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        PanelCard(contentPadding = 16) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Tren Skor 7 Hari Terakhir",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }
            Spacer(Modifier.height(14.dp))
            WeeklyChart(stats = stats)
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SmallStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.BarChart,
                tint = NeonTeal,
                label = "Rata-rata Combo",
                value = if (stats.averageCombo <= 0f) "—" else "x${"%.1f".format(stats.averageCombo)}"
            )
            SmallStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.AutoAwesome,
                tint = NeonPink,
                label = "Scan Terbanyak",
                value = if (stats.bestDayScans == 0) "—" else "${stats.bestDayScans}/hari"
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SmallStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.WorkspacePremium,
                tint = NeonGold,
                label = "Baris Terhapus",
                value = stats.totalLines.toString()
            )
            SmallStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.CalendarMonth,
                tint = NeonTeal,
                label = "Hari Beruntun",
                value = "${stats.streakDays} hari"
            )
        }

        Spacer(Modifier.height(20.dp))

        SectionHeader(title = "Pencapaian")
        Spacer(Modifier.height(10.dp))

        stats.achievements.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                pair.forEach { achievement ->
                    AchievementCard(
                        achievement = achievement,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        if (stats.isEmpty) {
            PanelCard(contentPadding = 16) {
                Text(
                    text = "Belum ada data. Pindai papan pertamamu di tab Scan, lalu simpan langkahnya.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun WeeklyChart(stats: StatsSummary) {
    val maxScore = max(1, stats.week.maxOfOrNull { it.score } ?: 0)
    val axisTop = niceCeiling(maxScore)

    Row(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .height(150.dp)
                .width(34.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            listOf(axisTop, axisTop * 2 / 3, axisTop / 3, 0).forEach { tick ->
                Text(
                    text = shortNumber(tick),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Row(
            modifier = Modifier
                .weight(1f)
                .height(150.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            stats.week.forEach { bucket ->
                val fraction = (bucket.score.toFloat() / axisTop).coerceIn(0f, 1f)
                val animated by animateFloatAsState(
                    targetValue = fraction,
                    animationSpec = tween(durationMillis = 650),
                    label = "bar-${bucket.label}"
                )
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    if (bucket.score > 0) {
                        Text(
                            text = formatScore(bucket.score),
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonPink,
                            maxLines = 1
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = true),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(animated.coerceAtLeast(0.012f))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(NeonPink, NeonPink.copy(alpha = 0.55f))
                                    ),
                                    RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                )
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = bucket.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun SmallStatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    PanelCard(modifier = modifier, contentPadding = 14) {
        IconTile(background = tint.copy(alpha = 0.14f), modifier = Modifier.size(40.dp)) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary
        )
    }
}

@Composable
private fun AchievementCard(achievement: Achievement, modifier: Modifier = Modifier) {
    val accent = if (achievement.unlocked) NeonGold else TextSecondary
    Column(
        modifier = modifier
            .background(
                if (achievement.unlocked) SurfaceElevatedHigh else SurfaceElevated,
                RoundedCornerShape(16.dp)
            )
            .border(
                BorderStroke(1.dp, if (achievement.unlocked) accent.copy(alpha = 0.4f) else Divider),
                RoundedCornerShape(16.dp)
            )
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(accent.copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (achievement.unlocked) Icons.Rounded.WorkspacePremium else Icons.Rounded.Lock,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = achievement.title,
            style = MaterialTheme.typography.titleSmall,
            color = if (achievement.unlocked) TextPrimary else TextSecondary
        )
        Text(
            text = achievement.description,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

private fun niceCeiling(value: Int): Int {
    if (value <= 10) return 10
    var step = 10
    while (step * 10 < value) step *= 10
    val rounded = ((value + step - 1) / step) * step
    return max(rounded, 10)
}

private fun shortNumber(value: Int): String = when {
    value >= 1000 && value % 1000 == 0 -> "${value / 1000}K"
    value >= 1000 -> "${"%.1f".format(value / 1000f)}K"
    else -> value.toString()
}
