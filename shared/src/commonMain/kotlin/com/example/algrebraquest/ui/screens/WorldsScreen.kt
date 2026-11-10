package com.example.algrebraquest.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.algrebraquest.domain.Route
import com.example.algrebraquest.domain.WorldsCatalog
import com.example.algrebraquest.domain.emptyPlayer
import com.example.algrebraquest.ui.AppViewModel
import com.example.algrebraquest.ui.ScreenFrame
import com.example.algrebraquest.ui.UiState
import com.example.algrebraquest.ui.theme.Aq
import com.example.algrebraquest.ui.theme.BackPill
import com.example.algrebraquest.ui.theme.ProgressBar
import com.example.algrebraquest.ui.theme.ScreenHeader

/**
 * R-06 · Selección de mundos (WorldsScreen.kt).
 * Muestra los 4 mundos (Factorización, Trigonometría, Ecuaciones Diferenciales y Álgebra Lineal)
 * respetando su estado de desbloqueo encadenado y niveles completados (worlds.js).
 */
@Composable
fun WorldsScreen(state: UiState, vm: AppViewModel) {
    val player = state.player ?: emptyPlayer(
        state.session?.username ?: "alumno",
        state.session?.displayName ?: "Estudiante",
    )

    ScreenFrame(state = state, vm = vm, showHud = true) {
        BackPill(onClick = { vm.popTo(Route.Menu) }, label = "← Regresar al menú")

        ScreenHeader(
            kicker = "ÁLGEBRA QUEST · MAPA DE AVENTURA",
            title = "Selecciona un Mundo",
            subtitle = "Completa todos los niveles de un mundo para desbloquear el siguiente.",
        )

        state.worlds.forEach { mundo ->
            val unlocked = WorldsCatalog.worldUnlocked(player, mundo, state.worlds)
            val completed = unlocked && WorldsCatalog.worldDone(player, mundo)
            val doneCount = WorldsCatalog.completedCount(player, mundo)
            val totalCount = mundo.niveles.size.coerceAtLeast(1)
            val requiredWorld = mundo.requiere?.let { WorldsCatalog.world(state.worlds, it) }
            val worldColor = Aq.parse(mundo.color, Aq.Green)

            val statusLabel = when {
                !unlocked && requiredWorld != null -> "Bloqueado 🔒 · Completa ${requiredWorld.nombre}"
                !unlocked -> "Bloqueado 🔒"
                completed -> "Completado ⭐ ($doneCount/${mundo.niveles.size})"
                else -> "Disponible · $doneCount/${mundo.niveles.size} niveles"
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .alpha(if (unlocked) 1f else 0.55f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Aq.Panel)
                    .border(
                        width = if (unlocked) 2.dp else 1.dp,
                        color = if (unlocked) worldColor else Aq.Border,
                        shape = RoundedCornerShape(18.dp),
                    )
                    .clickable(enabled = unlocked) {
                        vm.navigate(Route.LevelSelect(mundo.id))
                    }
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(if (unlocked) mundo.icono else "🔒", fontSize = 36.sp)
                        Column {
                            Text(
                                mundo.nombre,
                                color = if (unlocked) Aq.Gold else Aq.Text,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                statusLabel,
                                color = if (unlocked) worldColor else Aq.Muted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    if (unlocked) {
                        Text("Entrar →", color = Aq.Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                Text(mundo.descripcion, color = Aq.Muted, fontSize = 13.sp)
                ProgressBar(
                    fraction = doneCount.toFloat() / totalCount.toFloat(),
                    color = if (unlocked) worldColor else Aq.Border,
                )
            }
        }
    }
}
