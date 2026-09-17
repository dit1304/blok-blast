package com.rork.blockblastsolver.ui.solution

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rork.blockblastsolver.domain.MoveSolution
import com.rork.blockblastsolver.domain.PieceCatalog
import com.rork.blockblastsolver.domain.Solver
import com.rork.blockblastsolver.ui.components.AppTopBar
import com.rork.blockblastsolver.ui.components.BoardGrid
import com.rork.blockblastsolver.ui.components.BoardHighlight
import com.rork.blockblastsolver.ui.components.PanelCard
import com.rork.blockblastsolver.ui.components.PieceView
import com.rork.blockblastsolver.ui.components.SectionHeader
import com.rork.blockblastsolver.ui.components.plainTitle
import com.rork.blockblastsolver.ui.theme.Canvas as CanvasColor
import com.rork.blockblastsolver.ui.theme.Divider
import com.rork.blockblastsolver.ui.theme.NeonGold
import com.rork.blockblastsolver.ui.theme.NeonPink
import com.rork.blockblastsolver.ui.theme.NeonTeal
import com.rork.blockblastsolver.ui.theme.SurfaceElevated
import com.rork.blockblastsolver.ui.theme.SurfaceElevatedHigh
import com.rork.blockblastsolver.ui.theme.TextPrimary
import com.rork.blockblastsolver.ui.theme.TextSecondary
import com.rork.blockblastsolver.util.formatScore

/**
 * The payoff screen. It draws the recommended placement directly on top of the board,
 * step by step, and lets the user compare alternative plans before committing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolutionScreen(
    solutions: List<MoveSolution>,
    selectedIndex: Int,
    stepIndex: Int,
    bestScore: Int,
    onBack: () -> Unit,
    onSelectSolution: (Int) -> Unit,
    onStep: (Int) -> Unit,
    onCommit: () -> Unit,
    onHelp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val solution = solutions.getOrNull(selectedIndex)
    val alternatives = remember(solutions, selectedIndex) {
        solutions.withIndex().filter { it.index != selectedIndex }
    }

    Scaffold(
        modifier = modifier,
        containerColor = CanvasColor,
        topBar = {
            AppTopBar(
                title = plainTitle("Hasil Solusi"),
                subtitle = "Solusi terbaik untuk papan ini",
                onBack = onBack,
                onHelp = onHelp
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(CanvasColor)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = onCommit,
                    enabled = solution != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(30.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonTeal,
                        contentColor = CanvasColor,
                        disabledContainerColor = SurfaceElevatedHigh,
                        disabledContentColor = TextSecondary
                    )
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Gunakan Langkah Ini", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    ) { innerPadding ->
        if (solution == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Belum ada solusi untuk ditampilkan.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )
            }
            return@Scaffold
        }

        val placement = solution.placements.getOrNull(stepIndex)
        val boardForStep = placement?.let { current ->
            // Show the board as it looks right before this step lands.
            if (stepIndex == 0) solution.startBoard
            else solution.placements[stepIndex - 1].boardAfterClear
        } ?: solution.finalBoard

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            PanelCard(contentPadding = 14) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WorkspacePremium,
                        contentDescription = null,
                        tint = NeonGold,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Total Skor Tersimpan",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = formatScore(bestScore),
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Ukuran Papan",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = "8 x 8",
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimary
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                BoardGrid(
                    bits = boardForStep,
                    highlight = BoardHighlight(
                        placementMask = placement?.mask ?: 0L,
                        clearedMask = placement?.let { current ->
                            if (current.clearedRows.isEmpty() && current.clearedCols.isEmpty()) 0L
                            else com.rork.blockblastsolver.domain.BoardOps.clearMask(
                                current.clearedRows,
                                current.clearedCols
                            ) and current.boardBeforeClear and current.mask.inv()
                        } ?: 0L,
                        accent = NeonTeal
                    ),
                    animateHighlight = true
                )

                Spacer(Modifier.height(12.dp))

                StepControls(
                    solution = solution,
                    stepIndex = stepIndex,
                    onStep = onStep
                )
            }

            Spacer(Modifier.height(12.dp))

            MetricsStrip(solution = solution)

            Spacer(Modifier.height(18.dp))

            if (alternatives.isNotEmpty()) {
                SectionHeader(title = "Alternatif Langkah Lain")
                Spacer(Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(end = 4.dp)
                ) {
                    itemsIndexed(
                        items = alternatives,
                        key = { _, item -> item.index }
                    ) { position, item ->
                        AlternativeCard(
                            label = "Opsi ${position + 2}",
                            solution = item.value,
                            onClick = { onSelectSolution(item.index) }
                        )
                    }
                }
            } else {
                PanelCard(contentPadding = 14) {
                    Text(
                        text = "Hanya satu urutan langkah yang mungkin untuk papan ini.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StepControls(
    solution: MoveSolution,
    stepIndex: Int,
    onStep: (Int) -> Unit
) {
    val placement = solution.placements.getOrNull(stepIndex)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { onStep(stepIndex - 1) },
            enabled = stepIndex > 0,
            modifier = Modifier
                .size(42.dp)
                .background(SurfaceElevatedHigh, CircleShape)
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Langkah sebelumnya",
                tint = if (stepIndex > 0) TextPrimary else TextSecondary.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Langkah ${stepIndex + 1} dari ${solution.placements.size}",
                style = MaterialTheme.typography.labelMedium,
                color = NeonTeal
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (placement != null) {
                    PieceView(piece = placement.piece, box = 30.dp, color = NeonTeal)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${PieceCatalog.label(placement.piece)} → baris ${placement.row + 1}, kolom ${placement.col + 1}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        IconButton(
            onClick = { onStep(stepIndex + 1) },
            enabled = stepIndex < solution.placements.lastIndex,
            modifier = Modifier
                .size(42.dp)
                .background(SurfaceElevatedHigh, CircleShape)
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = "Langkah berikutnya",
                tint = if (stepIndex < solution.placements.lastIndex) TextPrimary
                else TextSecondary.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun MetricsStrip(solution: MoveSolution) {
    PanelCard(contentPadding = 0) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MetricCell(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Layers,
                iconTint = NeonTeal,
                label = if (solution.totalLinesCleared > 0) "+${solution.totalLinesCleared} baris" else "0 baris",
                value = if (solution.totalLinesCleared > 0) "terhapus" else "tidak ada",
                valueColor = TextSecondary
            )
            MetricDivider()
            MetricCell(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.LocalFireDepartment,
                iconTint = NeonPink,
                label = "Combo",
                value = "x${solution.maxCombo}",
                valueColor = NeonPink
            )
            MetricDivider()
            MetricCell(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Star,
                iconTint = NeonTeal,
                label = "Skor",
                value = "+${formatScore(solution.totalScore)}",
                valueColor = NeonTeal
            )
        }
        if (!solution.survivesNextWave) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NeonPink.copy(alpha = 0.1f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(NeonPink, CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Hati-hati: papan jadi sempit untuk gelombang berikutnya.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeonPink
                )
            }
        }
    }
}

@Composable
private fun MetricCell(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 1
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = valueColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MetricDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(34.dp)
            .background(Divider)
    )
}

@Composable
private fun AlternativeCard(
    label: String,
    solution: MoveSolution,
    onClick: () -> Unit
) {
    val firstPlacement = solution.placements.firstOrNull()
    Row(
        modifier = Modifier
            .width(248.dp)
            .background(SurfaceElevated, RoundedCornerShape(16.dp))
            .border(BorderStroke(1.dp, Divider), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(92.dp)) {
            BoardGrid(
                bits = solution.startBoard,
                highlight = BoardHighlight(
                    ghostMask = firstPlacement?.mask ?: 0L
                )
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
            Text(
                text = Solver.describe(solution),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Skor +${formatScore(solution.totalScore)}",
                style = MaterialTheme.typography.titleSmall,
                color = NeonPink
            )
        }
    }
}
