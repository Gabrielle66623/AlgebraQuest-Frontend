package com.example.algrebraquest.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.algrebraquest.data.PlayerState
import com.example.algrebraquest.domain.WorldsCatalog
import com.example.algrebraquest.ui.theme.Aq
import com.example.algrebraquest.ui.theme.AqBackground
import com.example.algrebraquest.ui.theme.ProgressBar

/** Barra superior del juego: vidas, jugador, nivel, XP y monedas (equivale al HUD web). */
@Composable
fun TopHudBar(player: PlayerState, onOpenStore: () -> Unit, onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    var detail by remember { mutableStateOf<Pair<String, String?>?>(null) }
    Column(
        modifier.fillMaxWidth().background(Aq.Panel).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("❤️".repeat(player.vidas) + "🖤".repeat((player.vidasMax - player.vidas).coerceAtLeast(0)), fontSize = 14.sp,
                modifier = Modifier.clickable {
                    detail = "Te quedan ${player.vidas} de ${player.vidasMax} vidas. Recupera una por ${WorldsCatalog.LIFE_COST_XP} XP." to "Canjear XP"
                })
            Text("👤 ${player.displayName}", color = Aq.Text, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(horizontal = 8.dp).clickable(onClick = onOpenSettings))
            Text("⭐ Nivel ${player.nivel}", color = Aq.Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                modifier = Modifier.clickable { detail = "Nivel ${player.nivel}. Sigue resolviendo preguntas para subir." to null })
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("XP", color = Aq.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            ProgressBar(player.xp.toFloat() / player.xpMax.coerceAtLeast(1), Modifier.weight(1f))
            Text("${player.xp} / ${player.xpMax}", color = Aq.Text, fontSize = 12.sp,
                modifier = Modifier.clickable {
                    detail = "Experiencia: ${player.xp} de ${player.xpMax} XP. Una vida cuesta ${WorldsCatalog.LIFE_COST_XP} XP." to "Recuperar vida"
                })
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text("🪙 ${player.monedas} monedas", color = Aq.Gold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { detail = "Tienes ${player.monedas} monedas. Canjéalas por trofeos." to "Ver trofeos" })
        }
        detail?.let { (text, action) ->
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Aq.Panel2)
                    .border(1.dp, Aq.Gold.copy(alpha = 0.55f), RoundedCornerShape(10.dp)).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text, color = Aq.Text, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (action != null) Text(action, color = Aq.Gold, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                        modifier = Modifier.clickable { detail = null; onOpenStore() })
                    Text("Cerrar", color = Aq.Muted, fontSize = 13.sp, modifier = Modifier.clickable { detail = null })
                }
            }
        }
    }
}

/** Marco común de las pantallas: fondo, HUD opcional y contenido desplazable. */
@Composable
fun ScreenFrame(
    state: UiState,
    vm: AppViewModel,
    showHud: Boolean = true,
    scroll: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    AqBackground {
        Column(Modifier.fillMaxSize().safeContentPadding()) {
            val player = state.player
            if (showHud && player != null) {
                TopHudBar(player, onOpenStore = { vm.navigate(com.example.algrebraquest.domain.Route.Store) },
                    onOpenSettings = { vm.navigate(com.example.algrebraquest.domain.Route.Settings) })
            }
            Column(
                Modifier.weight(1f).fillMaxWidth()
                    .then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = content,
            )
        }
    }
}
