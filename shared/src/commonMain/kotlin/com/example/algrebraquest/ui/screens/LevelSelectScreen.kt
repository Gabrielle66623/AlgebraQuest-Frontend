package com.example.algrebraquest.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.algrebraquest.domain.RandomCharacterImage
import com.example.algrebraquest.domain.Route
import com.example.algrebraquest.domain.WorldsCatalog
import com.example.algrebraquest.domain.emptyPlayer
import com.example.algrebraquest.ui.AppViewModel
import com.example.algrebraquest.ui.ScreenFrame
import com.example.algrebraquest.ui.UiState
import com.example.algrebraquest.ui.theme.Aq
import com.example.algrebraquest.ui.theme.AqButton
import com.example.algrebraquest.ui.theme.AqButtonStyle
import com.example.algrebraquest.ui.theme.AqCard
import com.example.algrebraquest.ui.theme.BackPill
import com.example.algrebraquest.ui.theme.ScreenHeader
import org.jetbrains.compose.resources.painterResource

/**
 * R-07 · Selección de nivel dentro de un mundo (LevelSelectScreen.kt).
 * Basado en js/views/levelSelect.js: muestra los 5 niveles del mundo elegido,
 * estado de desbloqueo/completado, botón "▶ Repasar tema" e imagen aleatoria de personaje.
 */
@Composable
fun LevelSelectScreen(worldId: String, state: UiState, vm: AppViewModel) {
    val player = state.player ?: emptyPlayer(
        state.session?.username ?: "alumno",
        state.session?.displayName ?: "Estudiante",
    )
    val mundo = WorldsCatalog.world(state.worlds, worldId)
    val characterDrawable = remember(worldId) { RandomCharacterImage.next() }

    ScreenFrame(state = state, vm = vm, showHud = true) {
        BackPill(onClick = { vm.popTo(Route.Worlds) }, label = "← Volver a Mundos")

        if (mundo == null) {
            AqCard(Modifier.fillMaxWidth()) {
                Text("No se encontró el mundo solicitado.", color = Aq.Red)
            }
            return@ScreenFrame
        }

        val worldColor = Aq.parse(mundo.color, Aq.Mint)

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                ScreenHeader(
                    kicker = "MUNDO · ${mundo.icono}",
                    title = mundo.nombre.uppercase(),
                    subtitle = "Continúa con tu progreso (${WorldsCatalog.completedCount(player, mundo)}/${mundo.niveles.size} niveles).",
                )
            }
            Image(
                painterResource(characterDrawable),
                contentDescription = "Personaje del mundo",
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(2.dp, worldColor, RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Crop,
            )
        }

        mundo.niveles.forEach { nivel ->
            val unlocked = WorldsCatalog.levelUnlocked(player, mundo, nivel)
            val completed = WorldsCatalog.levelDone(player, mundo.id, nivel.id)

            Column(
                Modifier
                    .fillMaxWidth()
                    .alpha(if (unlocked) 1f else 0.55f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Aq.Panel)
                    .border(
                        width = if (completed) 2.dp else 1.dp,
                        color = when {
                            completed -> Aq.Green
                            unlocked -> worldColor
                            else -> Aq.Border
                        },
                        shape = RoundedCornerShape(16.dp),
                    )
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(enabled = unlocked) {
                            vm.navigate(Route.LevelIntro(mundo.id, nivel.id))
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (unlocked) nivel.icono else "🔒", fontSize = 28.sp)
                        Column {
                            Text(
                                "Nivel ${nivel.id}${if (completed) " · ✅ Completado" else ""}",
                                color = if (completed) Aq.Green else Aq.GoldSoft,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                nivel.titulo,
                                color = Aq.Text,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "${nivel.preguntas.size} preguntas · Teoría y práctica",
                                color = Aq.Muted,
                                fontSize = 12.sp,
                            )
                        }
                    }
                    if (unlocked) {
                        Text("Jugar →", color = Aq.Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (unlocked) {
                        AqButton(
                            text = if (completed) "🔁 Volver a jugar" else "▶ Entrar al nivel",
                            onClick = { vm.navigate(Route.LevelIntro(mundo.id, nivel.id)) },
                            modifier = Modifier.weight(1f),
                            style = AqButtonStyle.Primary,
                        )
                    }
                    AqButton(
                        text = "▶ Repasar tema",
                        onClick = { vm.navigate(Route.Tutorial(mundo.id, nivel.id)) },
                        modifier = Modifier.weight(1f),
                        style = AqButtonStyle.Ghost,
                    )
                }
            }
        }
    }
}
