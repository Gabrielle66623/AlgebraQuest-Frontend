package com.example.algrebraquest.ui.screens

import algrebraquest.shared.generated.resources.Res
import algrebraquest.shared.generated.resources.personaje
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.algrebraquest.domain.Route
import com.example.algrebraquest.domain.WorldsCatalog
import com.example.algrebraquest.domain.emptyPlayer
import com.example.algrebraquest.platform.BiometricMode
import com.example.algrebraquest.platform.BiometricStatus
import com.example.algrebraquest.ui.AppViewModel
import com.example.algrebraquest.ui.ScreenFrame
import com.example.algrebraquest.ui.UiState
import com.example.algrebraquest.ui.theme.Aq
import com.example.algrebraquest.ui.theme.AqButton
import com.example.algrebraquest.ui.theme.AqButtonStyle
import com.example.algrebraquest.ui.theme.AqCard
import com.example.algrebraquest.ui.theme.MessageBanner
import com.example.algrebraquest.ui.theme.ProgressBar
import com.example.algrebraquest.ui.theme.ScreenHeader
import com.example.algrebraquest.ui.theme.StatChip
import org.jetbrains.compose.resources.painterResource

/**
 * R-04 · Panel de Alumno (AlumnoScreen / HomeScreen.kt).
 * Muestra perfil activo, ejercicio de práctica (2x + 3 = 11), progreso general,
 * configuración biométrica (huella y rostro) y botón Jugar hacia el Menú principal (R-05).
 */
@Composable
fun AlumnoScreen(state: UiState, vm: AppViewModel) = HomeScreen(state, vm)

@Composable
fun HomeScreen(state: UiState, vm: AppViewModel) {
    val player = state.player ?: emptyPlayer(
        state.session?.username ?: "alumno",
        state.session?.displayName ?: "Estudiante",
    )
    val totalLevels = WorldsCatalog.totalLevels(state.worlds).coerceAtLeast(1)
    val completedLevels = player.completados.size
    val overallProgress = completedLevels.toFloat() / totalLevels.toFloat()

    // Ejercicio interactivo de calentamiento del inventario R-04: 2x + 3 = 11
    val warmupOptions = listOf("x = 4", "x = 3", "x = 5", "x = 7")
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var warmupFeedback by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    val sessionUser = state.session?.username ?: player.username
    val bioEnrolled = state.isBiometricEnrolledFor(sessionUser)

    ScreenFrame(state = state, vm = vm, showHud = true) {
        // ---------- Bienvenida Hero (home.js + R-04) ----------
        AqCard(Modifier.fillMaxWidth(), accent = Aq.Gold) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painterResource(Res.drawable.personaje),
                    contentDescription = "Personaje principal",
                    modifier = Modifier
                        .size(86.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .border(2.dp, Aq.Gold, RoundedCornerShape(20.dp)),
                    contentScale = ContentScale.Crop,
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ScreenHeader(
                        kicker = "PANEL DE ALUMNO · PERFIL ACTIVO",
                        title = "👋 ¡Hola, ${player.displayName}!",
                        subtitle = "Usuario @${player.username} · Listo para aprender Álgebra Universitaria jugando.",
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip("Nivel", "⭐ ${player.nivel}", Modifier.weight(1f), accent = Aq.Gold)
                StatChip("Monedas", "🪙 ${player.monedas}", Modifier.weight(1f), accent = Aq.Mint)
                StatChip("Trofeos", "🏅 ${player.trofeos.size}/4", Modifier.weight(1f), accent = Aq.Green)
            }

            Text(
                "Avance general: $completedLevels de $totalLevels niveles completados (${(overallProgress * 100).toInt()}%)",
                color = Aq.Muted,
                fontSize = 13.sp,
            )
            ProgressBar(fraction = overallProgress, color = Aq.Green)

            MessageBanner(state.error, state.notice)

            AqButton(
                text = "▶ Jugar · Ir al Menú Principal",
                onClick = { vm.navigate(Route.Menu) },
                modifier = Modifier.fillMaxWidth(),
                style = AqButtonStyle.Gold,
            )
        }

        // ---------- Ejercicio de práctica del inventario R-04 (2x + 3 = 11) ----------
        AqCard(Modifier.fillMaxWidth(), accent = Aq.Purple) {
            Text(
                "⚡ EJERCICIO DE PRÁCTICA RÁPIDA",
                color = Aq.Mint,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
            )
            Text(
                "Resuelve la ecuación lineal:  2x + 3 = 11",
                color = Aq.Gold,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Despeja la incógnita x antes de entrar a tus misiones:",
                color = Aq.Muted,
                fontSize = 13.sp,
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                warmupOptions.forEach { option ->
                    val isSelected = selectedOption == option
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Aq.Purple else Aq.Panel2)
                            .border(
                                1.5.dp,
                                if (isSelected) Aq.Gold else Aq.Border,
                                RoundedCornerShape(12.dp),
                            )
                            .clickable {
                                selectedOption = option
                                val ok = option == "x = 4"
                                warmupFeedback = if (ok) {
                                    true to "¡Correcto! 2x = 11 − 3 → 2x = 8 → x = 4. ¡Estás listo para jugar!"
                                } else {
                                    false to "Casi: resta 3 en ambos lados (2x = 8) y divide entre 2."
                                }
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(option, color = Aq.Text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }

            warmupFeedback?.let { (ok, msg) ->
                Text(
                    text = if (ok) "✅ $msg" else "💡 $msg",
                    color = if (ok) Aq.Green else Aq.GoldSoft,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        // ---------- Acceso Biométrico del Alumno (Huella y Reconocimiento Facial) ----------
        AqCard(Modifier.fillMaxWidth(), accent = Aq.Mint) {
            Text(
                "🫆😊 Sensor Biométrico · Rol Alumno",
                color = Aq.Mint,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            if (state.biometricStatus == BiometricStatus.AVAILABLE) {
                Text(
                    if (bioEnrolled) "✓ Tu cuenta (@$sessionUser) ya tiene huella dactilar y reconocimiento facial activos."
                    else "Activa el inicio de sesión con tu huella dactilar o reconocimiento facial en este dispositivo.",
                    color = Aq.Muted,
                    fontSize = 13.sp,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AqButton(
                        text = "🫆 Activar Huella",
                        onClick = { vm.enrollBiometric(BiometricMode.HUELLA) },
                        modifier = Modifier.weight(1f),
                        style = AqButtonStyle.Gold,
                    )
                    AqButton(
                        text = "😊 Activar Rostro",
                        onClick = { vm.enrollBiometric(BiometricMode.ROSTRO) },
                        modifier = Modifier.weight(1f),
                        style = AqButtonStyle.Ghost,
                    )
                }
            } else {
                Text(
                    "Configura una huella o rostro en tu teléfono para usar el acceso biométrico.",
                    color = Aq.Muted,
                    fontSize = 12.sp,
                )
            }
        }

        // ---------- Accesos directos y Cerrar sesión (R-12) ----------
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AqButton("🗺️ Mundos", { vm.navigate(Route.Worlds) }, Modifier.weight(1f), style = AqButtonStyle.Primary)
            AqButton("📊 Ranking", { vm.navigate(Route.Ranking) }, Modifier.weight(1f), style = AqButtonStyle.Ghost)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AqButton("⚙️ Ajustes", { vm.navigate(Route.Settings) }, Modifier.weight(1f), style = AqButtonStyle.Ghost)
            AqButton("⏻ Cerrar sesión", { vm.logout() }, Modifier.weight(1f), style = AqButtonStyle.Danger)
        }
    }
}
