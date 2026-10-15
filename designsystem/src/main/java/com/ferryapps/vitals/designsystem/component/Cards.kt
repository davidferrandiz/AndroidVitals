package com.ferryapps.vitals.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ferryapps.vitals.designsystem.R
import com.ferryapps.vitals.designsystem.theme.TabularNumbers
import com.ferryapps.vitals.designsystem.theme.VitalsTheme

private val CardShape = RoundedCornerShape(16.dp)

/** Tarjeta base de la app: superficie elevada, sin sombra, esquinas de 16 dp. */
@Composable
fun VitalCard(
    modifier: Modifier = Modifier,
    borderColor: Color? = null,
    contentPadding: CardPadding = CardPadding.Default,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .then(if (borderColor != null) Modifier.border(1.dp, borderColor, CardShape) else Modifier)
            .padding(contentPadding.dp.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

/** Relleno interior de tarjeta, en dp. */
enum class CardPadding(val dp: Int) { Default(16), Compact(14), Roomy(20) }

/** Icono de 18 dp sobre un cuadrado de color de categoría al 14 %. */
@Composable
fun IconBadge(@DrawableRes icon: Int, accent: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(accent.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
    }
}

/** Cabecera de tarjeta: icono, título, subtítulo opcional y valor destacado a la derecha. */
@Composable
fun CardHeader(
    @DrawableRes icon: Int,
    accent: Color,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    valueLabel: String? = null,
    trailing: @Composable (RowScope.() -> Unit)? = null,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon, accent)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (value != null) {
            Column(horizontalAlignment = Alignment.End) {
                Text(value, style = MaterialTheme.typography.titleMedium, color = accent, fontWeight = FontWeight.ExtraBold)
                if (valueLabel != null) {
                    Text(valueLabel, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = MaterialTheme.typography.bodySmall.letterSpacing), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        trailing?.invoke(this)
    }
}

/** Fila "etiqueta ······ valor". */
@Composable
fun MetricRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    divider: Boolean = false,
) {
    Column(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 32.dp)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium.merge(TabularNumbers),
                fontWeight = FontWeight.SemiBold,
                color = valueColor
            )
        }
        if (divider) HorizontalDivider(color = VitalsTheme.palette.divider, thickness = 1.dp)
    }
}

/** Título de sección dentro de una tarjeta ("NÚCLEOS", "TEMPERATURAS"). */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

/**
 * Botón para desplegar/plegar una sección. Fila completa de 44 dp, con rol de botón y
 * estado "expandido/plegado" para TalkBack.
 */
@Composable
fun ExpandToggle(
    label: String,
    expanded: Boolean,
    accent: Color,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, tween(250), label = "chevron")
    val stateText = stringResource(if (expanded) R.string.ds_state_expanded else R.string.ds_state_collapsed)
    Column(modifier) {
        HorizontalDivider(color = VitalsTheme.palette.divider, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .clickable(role = Role.Button, onClick = onToggle)
                .semantics { stateDescription = stateText },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = accent, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Icon(
                painterResource(R.drawable.ic_chevron_down),
                contentDescription = null,
                tint = accent,
                modifier = Modifier
                    .size(18.dp)
                    .rotate(rotation)
            )
        }
    }
}

/** Contenido que aparece y desaparece con la animación estándar de la app. */
@Composable
fun ExpandableContent(visible: Boolean, content: @Composable ColumnScope.() -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)) + expandVertically(tween(300)),
        exit = fadeOut(tween(150)) + shrinkVertically(tween(250))
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp), content = content)
    }
}

/** Tarjeta de lista con título, para Hardware y Ajustes. */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    VitalCard(modifier = modifier, contentPadding = CardPadding.Roomy) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            trailing?.invoke(this)
        }
        Column(verticalArrangement = Arrangement.spacedBy(0.dp), content = content)
    }
}
