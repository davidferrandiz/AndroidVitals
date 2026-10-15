package com.ferryapps.vitals.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ferryapps.vitals.designsystem.R
import com.ferryapps.vitals.designsystem.theme.VitalsTheme

enum class StatusKind { GOOD, WARNING, CRITICAL, NEUTRAL }

@Composable
fun StatusKind.color(): Color = when (this) {
    StatusKind.GOOD -> VitalsTheme.palette.good
    StatusKind.WARNING -> VitalsTheme.palette.warning
    StatusKind.CRITICAL -> VitalsTheme.palette.critical
    StatusKind.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
}

/** Aviso destacado: "Rendimiento sin limitar", "Memoria baja"… */
@Composable
fun StatusBanner(
    kind: StatusKind,
    @DrawableRes icon: Int,
    title: String,
    message: String?,
    modifier: Modifier = Modifier,
) {
    val color = kind.color()
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.38f), shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (message != null) {
                Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Pastilla pequeña: "Wi-Fi 6", "Widevine L1". */
@Composable
fun InfoChip(text: String, modifier: Modifier = Modifier, accent: Color? = null) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = if (accent != null) FontWeight.Bold else FontWeight.Medium,
        color = accent ?: MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(accent?.copy(alpha = 0.14f) ?: VitalsTheme.palette.raised)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) { content() }
}

/** Etiqueta de estado redonda ("Sin limitación", "Buena"). */
@Composable
fun StatusPill(text: String, kind: StatusKind, modifier: Modifier = Modifier) {
    InfoChip(text = text, accent = kind.color(), modifier = modifier)
}

/**
 * Dato que este dispositivo no ofrece. Nunca una sección vacía: dice qué falta y,
 * con "¿Por qué?", el motivo.
 */
@Composable
fun UnavailableRow(
    title: String,
    reason: String,
    modifier: Modifier = Modifier,
) {
    var showReason by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(VitalsTheme.palette.raised)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(
                painterResource(R.drawable.ic_info),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(R.string.ds_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                stringResource(R.string.ds_why),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .clickable(role = Role.Button) { showReason = !showReason }
                    .padding(horizontal = 4.dp, vertical = 12.dp)
            )
        }
        if (showReason) {
            Text(
                reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 30.dp, bottom = 6.dp)
            )
        }
    }
}

/** Cabecera de pantalla secundaria: atrás + título. */
@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier.semantics { heading() }
    )
}
