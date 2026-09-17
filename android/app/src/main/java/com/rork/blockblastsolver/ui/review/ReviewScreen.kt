package com.rork.blockblastsolver.ui.review

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LayersClear
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rork.blockblastsolver.domain.BOARD_SIZE
import com.rork.blockblastsolver.domain.BoardOps
import com.rork.blockblastsolver.domain.Piece
import com.rork.blockblastsolver.domain.PieceCatalog
import com.rork.blockblastsolver.ui.components.AppTopBar
import com.rork.blockblastsolver.ui.components.BoardGrid
import com.rork.blockblastsolver.ui.components.PanelCard
import com.rork.blockblastsolver.ui.components.PieceView
import com.rork.blockblastsolver.ui.components.plainTitle
import com.rork.blockblastsolver.ui.solver.SolverUiState
import com.rork.blockblastsolver.ui.theme.Canvas as CanvasColor
import com.rork.blockblastsolver.ui.theme.Divider
import com.rork.blockblastsolver.ui.theme.NeonPink
import com.rork.blockblastsolver.ui.theme.NeonTeal
import com.rork.blockblastsolver.ui.theme.SurfaceElevated
import com.rork.blockblastsolver.ui.theme.SurfaceElevatedHigh
import com.rork.blockblastsolver.ui.theme.TextPrimary
import com.rork.blockblastsolver.ui.theme.TextSecondary

/**
 * Detail screen between scanning and solving: the user confirms the detected board,
 * fixes any mis-read cell by tapping it, and sets the three tray pieces.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    state: SolverUiState,
    onBack: () -> Unit,
    onToggleCell: (Int, Int) -> Unit,
    onClearBoard: () -> Unit,
    onSetPiece: (Int, String?) -> Unit,
    onComboChange: (Int) -> Unit,
    onSolve: () -> Unit,
    onHelp: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pickerSlot by remember { mutableIntStateOf(-1) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        modifier = modifier,
        containerColor = CanvasColor,
        topBar = {
            AppTopBar(
                title = plainTitle("Periksa Papan"),
                subtitle = "Pastikan papan & blok sudah benar",
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
                    onClick = onSolve,
                    enabled = state.canSolve && !state.isSolving,
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
                    if (state.isSolving) {
                        CircularProgressIndicator(
                            color = CanvasColor,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("Menghitung...", style = MaterialTheme.typography.titleMedium)
                    } else {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Hitung Langkah Terbaik", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            if (state.detectionNote != null) {
                DetectionBanner(
                    note = state.detectionNote,
                    confidence = state.detectionConfidence,
                    sourceLabel = state.source.label
                )
                Spacer(Modifier.height(12.dp))
            }

            PanelCard(contentPadding = 12) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${BoardOps.filledCount(state.boardBits)} / 64 kotak terisi",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextSecondary
                    )
                    TextButton(onClick = onClearBoard) {
                        Icon(
                            Icons.Rounded.LayersClear,
                            contentDescription = null,
                            tint = NeonPink,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Kosongkan", style = MaterialTheme.typography.labelMedium, color = NeonPink)
                    }
                }
                Spacer(Modifier.height(8.dp))
                BoardGrid(
                    bits = state.boardBits,
                    onCellTap = onToggleCell
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Ketuk kotak untuk menandai terisi / kosong",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Blok yang Tersedia",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                state.pieceSlots.forEachIndexed { index, piece ->
                    PieceSlot(
                        index = index,
                        piece = piece,
                        modifier = Modifier.weight(1f),
                        onPick = { pickerSlot = index },
                        onClear = { onSetPiece(index, null) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            PanelCard(contentPadding = 14) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Combo saat ini",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = "Isi 1 kalau streak combo-mu baru mulai",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StepperButton(
                            icon = Icons.Rounded.Remove,
                            enabled = state.startingCombo > 1,
                            onClick = { onComboChange(state.startingCombo - 1) }
                        )
                        Text(
                            text = "x${state.startingCombo}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = NeonPink,
                            modifier = Modifier
                                .width(52.dp)
                                .padding(horizontal = 4.dp),
                            textAlign = TextAlign.Center
                        )
                        StepperButton(
                            icon = Icons.Rounded.Add,
                            enabled = state.startingCombo < 9,
                            onClick = { onComboChange(state.startingCombo + 1) }
                        )
                    }
                }
            }

            if (state.errorMessage != null) {
                Spacer(Modifier.height(12.dp))
                PanelCard(borderColor = NeonPink.copy(alpha = 0.5f), contentPadding = 14) {
                    Text(
                        text = state.errorMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        color = NeonPink
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (pickerSlot >= 0) {
        ModalBottomSheet(
            onDismissRequest = { pickerSlot = -1 },
            sheetState = sheetState,
            containerColor = SurfaceElevated
        ) {
            PiecePickerSheet(
                onSelect = { piece ->
                    onSetPiece(pickerSlot, piece.id)
                    pickerSlot = -1
                }
            )
        }
    }
}

@Composable
private fun DetectionBanner(note: String, confidence: Float?, sourceLabel: String) {
    val accent = when {
        confidence == null -> TextSecondary
        confidence >= 0.4f -> NeonTeal
        else -> NeonPink
    }
    PanelCard(borderColor = accent.copy(alpha = 0.4f), contentPadding = 14) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(accent, CircleShape)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = sourceLabel.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = accent
            )
            if (confidence != null) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "akurasi ${(confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = note,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary
        )
    }
}

@Composable
private fun PieceSlot(
    index: Int,
    piece: Piece?,
    modifier: Modifier = Modifier,
    onPick: () -> Unit,
    onClear: () -> Unit
) {
    Box(
        modifier = modifier
            .background(SurfaceElevated, RoundedCornerShape(16.dp))
            .border(
                BorderStroke(1.dp, if (piece == null) Divider else NeonTeal.copy(alpha = 0.35f)),
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onPick)
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Blok ${index + 1}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Spacer(Modifier.height(6.dp))
            if (piece == null) {
                Box(
                    modifier = Modifier.size(52.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = "Pilih blok ${index + 1}",
                        tint = TextSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Text(
                    text = "Pilih",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            } else {
                PieceView(piece = piece, box = 52.dp)
                Text(
                    text = PieceCatalog.label(piece),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
        if (piece != null) {
            IconButton(
                onClick = onClear,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(22.dp)
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Hapus blok ${index + 1}",
                    tint = TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(40.dp)
            .background(SurfaceElevatedHigh, CircleShape)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) TextPrimary else TextSecondary.copy(alpha = 0.4f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun PiecePickerSheet(onSelect: (Piece) -> Unit) {
    val grouped = remember {
        PieceCatalog.all.groupBy { it.family }.entries.sortedBy { it.value.first().size }
    }
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = "Pilih Bentuk Blok",
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Bentuk sesuai yang muncul di tray game, termasuk arah rotasinya",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(Modifier.height(12.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            modifier = Modifier.height(420.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(grouped, key = { it.key }) { entry ->
                Column {
                    Text(
                        text = PieceCatalog.label(entry.value.first()),
                        style = MaterialTheme.typography.titleSmall,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(end = 8.dp)
                    ) {
                        items(entry.value, key = { it.id }) { piece ->
                            Box(
                                modifier = Modifier
                                    .background(SurfaceElevatedHigh, RoundedCornerShape(14.dp))
                                    .border(1.dp, Divider, RoundedCornerShape(14.dp))
                                    .clickable { onSelect(piece) }
                                    .padding(8.dp)
                            ) {
                                PieceView(piece = piece, box = 46.dp)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** Board coordinate helper kept for semantics/testing of the tap mapping. */
internal fun cellLabel(row: Int, col: Int): String =
    "Baris ${row + 1} kolom ${col + 1} dari $BOARD_SIZE"
