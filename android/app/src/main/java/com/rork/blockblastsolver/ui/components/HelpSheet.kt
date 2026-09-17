package com.rork.blockblastsolver.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import com.rork.blockblastsolver.ui.theme.NeonTeal
import com.rork.blockblastsolver.ui.theme.TextPrimary
import com.rork.blockblastsolver.ui.theme.TextSecondary

/** Explains the flow and sets expectations about what the app can and cannot do. */
@Composable
fun HelpSheetContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp)
    ) {
        Text(
            text = "Cara Pakai",
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary
        )
        Spacer(Modifier.height(12.dp))

        HelpStep(
            number = 1,
            title = "Pindai papan",
            body = "Arahkan kamera ke layar HP yang menjalankan Block Blast, atau impor screenshot dari galeri."
        )
        HelpStep(
            number = 2,
            title = "Periksa hasil baca",
            body = "Ketuk kotak yang salah untuk memperbaikinya, lalu pilih 3 bentuk blok yang ada di tray game."
        )
        HelpStep(
            number = 3,
            title = "Ikuti langkah terbaik",
            body = "Solver menghitung semua urutan penempatan, lalu menandai posisi terbaik langsung di atas grid."
        )
        HelpStep(
            number = 4,
            title = "Simpan & lanjut",
            body = "Tekan \"Gunakan Langkah Ini\" untuk menyimpan ke riwayat. Papan otomatis lanjut dari hasil tadi."
        )

        Spacer(Modifier.height(10.dp))
        Text(
            text = "Catatan",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Aplikasi ini adalah asisten terpisah: ia tidak menempel di atas game dan tidak mengubah apa pun " +
                "di dalam Block Blast. Kamu tetap melakukan gerakan sendiri di game, aplikasi hanya memberi saran " +
                "langkah paling menguntungkan.",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )
    }
}

@Composable
private fun HelpStep(number: Int, title: String, body: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(NeonTeal.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = NeonTeal
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.Top) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}
