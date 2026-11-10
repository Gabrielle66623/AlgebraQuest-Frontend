package com.example.algrebraquest.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.algrebraquest.domain.RandomCharacterImage
import com.example.algrebraquest.domain.Route
import com.example.algrebraquest.domain.WorldsCatalog
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
 * R-07 · Introducción y teoría de un nivel (LevelIntroScreen.kt).
 * Basado en js/views/levelIntro.js: presenta el título del nivel, la definición teórica,
 * un ejemplo resuelto, personaje aleatorio y los botones "Comenzar reto" y "▶ Ver tutorial del tema".
 */
@Composable
fun LevelIntroScreen(worldId: String, levelId: Int, state: UiState, vm: AppViewModel) {
    val pair = WorldsCatalog.level(state.worlds, worldId, levelId)
    val characterDrawable = remember(worldId, levelId) { RandomCharacterImage.next() }

    ScreenFrame(state = state, vm = vm, showHud = true) {
        BackPill(onClick = { vm.popTo(Route.LevelSelect(worldId)) }, label = "← Volver a niveles")

        if (pair == null) {
            AqCard(Modifier.fillMaxWidth()) {
                Text("No se encontró el nivel solicitado.", color = Aq.Red)
            }
            return@ScreenFrame
        }

        val (mundo, nivel) = pair
        val worldColor = Aq.parse(mundo.color, Aq.Gold)

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                ScreenHeader(
                    kicker = "${mundo.icono} ${mundo.nombre.uppercase()} · NIVEL ${nivel.id}",
                    title = "${nivel.icono} ${nivel.titulo}",
                    subtitle = "${nivel.preguntas.size} reactivos listos para resolver.",
                )
            }
            Image(
                painterResource(characterDrawable),
                contentDescription = "Personaje de introducción",
                modifier = Modifier
                    .size(92.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(2.dp, worldColor, RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop,
            )
        }

        AqCard(Modifier.fillMaxWidth(), accent = worldColor) {
            Text(
                "📖 APRENDE LA REGLA",
                color = Aq.Mint,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
            )
            Text(
                nivel.teoria.definicion,
                color = Aq.Text,
                fontSize = 16.sp,
                lineHeight = 23.sp,
            )
        }

        AqCard(Modifier.fillMaxWidth(), accent = Aq.Gold) {
            Text(
                "✨ EJEMPLO RESUELTO",
                color = Aq.Gold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
            )
            Text(
                nivel.teoria.ejemplo,
                color = Aq.GoldSoft,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        AqButton(
            text = "🚀 Comenzar reto",
            onClick = { vm.navigate(Route.Game(mundo.id, nivel.id)) },
            modifier = Modifier.fillMaxWidth(),
            style = AqButtonStyle.Gold,
        )

        AqButton(
            text = "▶ Ver tutorial del tema",
            onClick = { vm.navigate(Route.Tutorial(mundo.id, nivel.id)) },
            modifier = Modifier.fillMaxWidth(),
            style = AqButtonStyle.Ghost,
        )
    }
}
