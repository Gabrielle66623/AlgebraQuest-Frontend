package com.example.algrebraquest.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions

/** Paleta tomada de los CSS del juego web (style.css, hud.css, menu.css, store.css). */
object Aq {
    val Bg = Color(0xFF071330)
    val Panel = Color(0xFF111D45)
    val Panel2 = Color(0xFF13234A)
    val Border = Color(0xFF2C2C55)
    val Gold = Color(0xFFFFC83D)
    val GoldSoft = Color(0xFFFFE08A)
    val Purple = Color(0xFF6C3CFF)
    val Glow = Color(0xFF8F63FF)
    val Green = Color(0xFF00FF88)
    val Mint = Color(0xFF5BE0B2)
    val Red = Color(0xFFFF6B6B)
    val Text = Color(0xFFF7F8FB)
    val Muted = Color(0xFFAEBBD2)
    val Ink = Color(0xFF17213C)

    fun parse(hex: String, fallback: Color = Mint): Color = runCatching {
        Color(hex.removePrefix("#").toLong(16) or 0xFF000000)
    }.getOrDefault(fallback)
}

private val scheme = darkColorScheme(
    primary = Aq.Purple, onPrimary = Color.White,
    secondary = Aq.Gold, onSecondary = Aq.Ink,
    background = Aq.Bg, onBackground = Aq.Text,
    surface = Aq.Panel, onSurface = Aq.Text,
    error = Aq.Red,
)

@Composable
fun AqTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = scheme, content = content)

/** Fondo azul noche con estrellas (equivale a `.stars` del juego web). */
@Composable
fun AqBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .fillMaxSize()
            .background(Aq.Bg)
            .drawBehind {
                var seed = 42L
                fun next(): Float { seed = (seed * 1103515245L + 12345L) and 0x7fffffffL; return (seed % 10000) / 10000f }
                repeat(70) {
                    val x = next() * size.width
                    val y = next() * size.height
                    drawCircle(Color.White.copy(alpha = 0.15f + next() * 0.45f), radius = 0.8f + next() * 1.8f, center = Offset(x, y))
                }
            },
        content = content,
    )
}

enum class AqButtonStyle { Primary, Gold, Ghost, Danger }

@Composable
fun AqButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AqButtonStyle = AqButtonStyle.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val container = when (style) {
        AqButtonStyle.Primary -> Aq.Purple
        AqButtonStyle.Gold -> Aq.Gold
        AqButtonStyle.Ghost -> Color.Transparent
        AqButtonStyle.Danger -> Aq.Red
    }
    val content = when (style) {
        AqButtonStyle.Gold -> Aq.Ink
        AqButtonStyle.Danger -> Aq.Ink
        else -> Color.White
    }
    val active = enabled && !loading
    val border = if (style == AqButtonStyle.Ghost) BorderStroke(1.dp, Aq.Gold.copy(alpha = 0.5f)) else null
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(container.copy(alpha = if (active) 1f else 0.4f))
            .let { if (border != null) it.border(border, RoundedCornerShape(14.dp)) else it }
            .clickable(enabled = active, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = content, strokeWidth = 2.dp)
        else Text(text, color = if (style == AqButtonStyle.Ghost) Aq.GoldSoft else content, fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp, letterSpacing = 0.3.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun AqCard(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    padding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(Aq.Panel2.copy(alpha = 0.85f), Aq.Panel)))
            .border(1.dp, accent?.copy(alpha = 0.35f) ?: Aq.Border, RoundedCornerShape(20.dp))
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
fun ScreenHeader(kicker: String, title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(kicker, color = Aq.Mint, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Text(title, color = Aq.Gold, fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp)
        if (subtitle != null) Text(subtitle, color = Aq.Muted, fontSize = 15.sp)
    }
}

@Composable
fun BackPill(onClick: () -> Unit, label: String = "← Volver", modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(Aq.Panel2)
            .border(1.dp, Aq.Border, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) { Text(label, color = Aq.GoldSoft, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
}

@Composable
fun ProgressBar(fraction: Float, modifier: Modifier = Modifier, color: Color = Aq.Green, height: Dp = 12.dp) {
    Box(modifier.fillMaxWidth().height(height).clip(CircleShape).background(Aq.Border)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f)).background(Brush.horizontalGradient(listOf(color, color.copy(alpha = 0.7f)))))
    }
}

@Composable
fun MessageBanner(error: String?, notice: String? = null, modifier: Modifier = Modifier) {
    val text = error ?: notice ?: return
    val tone = if (error != null) Aq.Red else Aq.Green
    Text(
        text,
        color = tone,
        fontSize = 13.sp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(tone.copy(alpha = 0.12f))
            .border(1.dp, tone.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
}

@Composable
fun StatChip(label: String, value: String, modifier: Modifier = Modifier, accent: Color = Aq.Gold) {
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).background(Aq.Panel2).border(1.dp, Aq.Border, RoundedCornerShape(12.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label.uppercase(), color = Aq.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(value, color = accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AqTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailing: (@Composable () -> Unit)? = null,
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label) }, singleLine = true, enabled = enabled,
        visualTransformation = visualTransformation, keyboardOptions = keyboardOptions, trailingIcon = trailing,
        modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Aq.Gold, unfocusedBorderColor = Aq.Border,
            focusedLabelColor = Aq.Gold, unfocusedLabelColor = Aq.Muted,
            focusedTextColor = Aq.Text, unfocusedTextColor = Aq.Text, cursorColor = Aq.Gold,
            focusedContainerColor = Aq.Panel2, unfocusedContainerColor = Aq.Panel2,
        ),
    )
}

@Composable
fun Loading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Aq.Gold)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, color = Aq.GoldSoft, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = modifier.padding(top = 8.dp))
}

@Composable
fun LabelValueRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Aq.Muted, fontSize = 14.sp)
        Text(value, color = Aq.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
