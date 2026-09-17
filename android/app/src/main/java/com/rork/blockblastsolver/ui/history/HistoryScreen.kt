package com.rork.blockblastsolver.ui.history

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rork.blockblastsolver.data.ScanRecord
import com.rork.blockblastsolver.domain.PieceCatalog
import com.rork.blockblastsolver.ui.components.BoardHighlight
import com.rork.blockblastsolver.ui.components.BoardThumbnail
import com.rork.blockblastsolver.ui.theme.Divider
import com.rork.blockblastsolver.ui.theme.NeonPink
import com.rork.blockblastsolver.ui.theme.NeonTeal
import com.rork.blockblastsolver.ui.theme.SurfaceElevated
import com.rork.blockblastsolver.ui.theme.TextPrimary
import com.rork.blockblastsolver.ui.theme.TextSecondary
import com.rork.blockblastsolver.util.formatRelativeTimestamp
import com.rork.blockblastsolver.util.formatScore

/** Riwayat tab: every saved turn, searchable, reusable as a starting board. */
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onQueryChange: (String) -> Unit,
    onOpen: (ScanRecord) -> Unit,
    onDelete: (String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = {
                Text(
                    "Cari riwayat...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )
            },
            leadingIcon = {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = TextSecondary)
            },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Hapus pencarian", tint = TextSecondary)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(30.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceElevated,
                unfocusedContainerColor = SurfaceElevated,
                focusedBorderColor = NeonTeal.copy(alpha = 0.5f),
                unfocusedBorderColor = Divider,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = NeonTeal
            )
        )

        if (state.records.isEmpty()) {
            EmptyHistory(
                hasQuery = state.query.isNotBlank(),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = contentPadding.calculateBottomPadding())
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = contentPadding.calculateBottomPadding() + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.records, key = { it.id }) { record ->
                    HistoryRow(
                        record = record,
                        onClick = { onOpen(record) },
                        onDelete = { onDelete(record.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    record: ScanRecord,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceElevated, RoundedCornerShape(16.dp))
            .border(BorderStroke(1.dp, Divider), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(64.dp)) {
            BoardThumbnail(
                bits = record.boardBits,
                highlight = BoardHighlight()
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = formatRelativeTimestamp(record.timestampMillis),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                text = "Skor +${formatScore(record.gainedScore)}",
                style = MaterialTheme.typography.titleSmall,
                color = NeonPink
            )
            Text(
                text = buildString {
                    append(if (record.linesCleared > 0) "${record.linesCleared} baris terhapus" else "Tanpa hapus baris")
                    append(" · combo x${record.combo}")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            val pieces = record.pieceIds.mapNotNull { PieceCatalog.find(it) }
            if (pieces.isNotEmpty()) {
                Text(
                    text = pieces.joinToString(" · ") { PieceCatalog.label(it) },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary.copy(alpha = 0.75f),
                    maxLines = 1
                )
            }
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
            Icon(
                Icons.Rounded.DeleteOutline,
                contentDescription = "Hapus riwayat",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
        Icon(
            Icons.AutoMirrored.Rounded.ArrowForwardIos,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier
                .padding(start = 4.dp)
                .size(14.dp)
        )
    }
}

@Composable
private fun EmptyHistory(hasQuery: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(SurfaceElevated, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.History,
                contentDescription = null,
                tint = NeonTeal,
                modifier = Modifier.size(34.dp)
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = if (hasQuery) "Tidak ada yang cocok" else "Belum ada riwayat",
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (hasQuery) {
                "Coba kata kunci lain, misal \"combo\" atau tanggal."
            } else {
                "Setiap langkah yang kamu pakai akan tersimpan di sini."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}
