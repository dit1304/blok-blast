package com.rork.blockblastsolver.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.rork.blockblastsolver.ui.theme.Canvas
import com.rork.blockblastsolver.ui.theme.Divider
import com.rork.blockblastsolver.ui.theme.NeonTeal
import com.rork.blockblastsolver.ui.theme.SurfaceElevated
import com.rork.blockblastsolver.ui.theme.TextSecondary

/** Wordmark with the second word in the neon accent, used by the tab-level app bar. */
fun brandTitle(): AnnotatedString = buildAnnotatedString {
    append("Block Blast ")
    withStyle(SpanStyle(color = NeonTeal)) { append("Solver") }
}

fun plainTitle(text: String): AnnotatedString = AnnotatedString(text)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: AnnotatedString,
    subtitle: String?,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onHelp: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Kembali"
                    )
                }
            }
        },
        actions = {
            if (onHelp != null) {
                IconButton(onClick = onHelp) {
                    Icon(
                        imageVector = Icons.Rounded.HelpOutline,
                        contentDescription = "Bantuan",
                        tint = TextSecondary
                    )
                }
            }
            actions()
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Canvas,
            scrolledContainerColor = Canvas,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

/** Low-elevation surface card used for every panel in the app. */
@Composable
fun PanelCard(
    modifier: Modifier = Modifier,
    borderColor: Color = Divider,
    containerColor: Color = SurfaceElevated,
    contentPadding: Int = 16,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding.dp),
            content = content
        )
    }
}

/** Section header with an optional trailing text action. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = NeonTeal
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                    contentDescription = null,
                    tint = NeonTeal,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(12.dp)
                )
            }
        }
    }
}

/** Rounded icon tile used by metric cards and achievement rows. */
@Composable
fun IconTile(
    background: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .background(background, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
