package com.example.algrebraquest.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.algrebraquest.domain.TROPHIES
import com.example.algrebraquest.domain.achievementsFor
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
import com.example.algrebraquest.ui.theme.SectionTitle
import com.example.algrebraquest.ui.theme.StatChip

/**
 * R-10 · Logros y Vitrina de Trofeos (AchievementsScreen.kt).
 * Basada en js/views/achievements.js: calcula los 6 logros a partir del progreso del jugador
 * y muestra la vitrina de trofeos comprados en la tienda.
 */
@Composable
fun AchievementsScreen(state: UiState, vm: AppViewModel) {
    val player = state.player ?: emptyPlayer(
        state.session?.username ?: "alumno",
        state.session?.displayName ?: "Estudiante",
    )
    val achievements = achievementsFor(player, state.worlds)
    val ownedTrophies = TROPHIES.filter { it.id in player.trofeos }

    ScreenFrame(state = state, vm = vm, showHud = true) {
        BackPill(onClick = { vm.popTo(Route.Menu) }, label = "← Volver al menú")

        ScreenHeader(
            kicker = "PROGRESO Y COLECCIÓN",
            title = "🏆 Logros y Vitrina",
            subtitle = "Tus medallas desbloqueadas y trofeos adquiridos en Álgebra Quest.",
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatChip("Logros", "${achievements.count { it.logrado }}/${achievements.size}", Modifier.weight(1f), accent = Aq.Green)
            StatChip("Trofeos", "${ownedTrophies.size}/${TROPHIES.size}", Modifier.weight(1f), accent = Aq.Gold)
            StatChip("Niveles", "${player.completados.size}", Modifier.weight(1f), accent = Aq.Mint)
        }

        // ---------- Vitrina de Trofeos (trofeosVitrina en achievements.js) ----------
        AqCard(Modifier.fillMaxWidth(), accent = Aq.Gold) {
            Text("🏅 Tu vitrina de trofeos", color = Aq.Gold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            if (ownedTrophies.isEmpty()) {
                Text("Aún no tienes trofeos comprados. Junta monedas resolviendo niveles.", color = Aq.Muted, fontSize = 14.sp)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ownedTrophies.forEach { t ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Aq.Panel2)
                                .border(1.dp, Aq.Gold, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(t.icono, fontSize = 28.sp)
                            Column {
                                Text(t.nombre, color = Aq.GoldSoft, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(t.descripcion, color = Aq.Muted, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            AqButton(
                text = "🪙 Ir a la tienda de trofeos",
                onClick = { vm.navigate(Route.Store) },
                modifier = Modifier.fillMaxWidth(),
                style = AqButtonStyle.Gold,
            )
        }

        // ---------- Lista de Logros ----------
        SectionTitle("Medallas de Logros")
        achievements.forEach { ach ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .alpha(if (ach.logrado) 1f else 0.55f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Aq.Panel)
                    .border(
                        width = if (ach.logrado) 2.dp else 1.dp,
                        color = if (ach.logrado) Aq.Green else Aq.Border,
                        shape = RoundedCornerShape(16.dp),
                    )
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (ach.logrado) ach.icono else "🔒", fontSize = 32.sp)
                Column(Modifier.weight(1f)) {
                    Text(
                        ach.titulo,
                        color = if (ach.logrado) Aq.Gold else Aq.Text,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(ach.descripcion, color = Aq.Muted, fontSize = 13.sp)
                }
                if (ach.logrado) {
                    Text("¡Logrado!", color = Aq.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
