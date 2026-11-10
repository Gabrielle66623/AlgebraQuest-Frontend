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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.algrebraquest.domain.Route
import com.example.algrebraquest.domain.TROPHIES
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
import com.example.algrebraquest.ui.theme.MessageBanner
import com.example.algrebraquest.ui.theme.ScreenHeader
import com.example.algrebraquest.ui.theme.SectionTitle
import com.example.algrebraquest.ui.theme.StatChip
import kotlinx.coroutines.launch

/**
 * R-10 · Tienda de recompensas (StoreScreen.kt).
 * Basada en js/views/store.js:
 * - Resumen de saldo (Vidas, XP, Monedas).
 * - Canje de 25 XP por una vida extra (A-11: /api/juego/restaurar-vida).
 * - Compra de los 4 trofeos con monedas (A-12: /api/tienda/comprar-trofeo).
 */
@Composable
fun StoreScreen(state: UiState, vm: AppViewModel) {
    val player = state.player ?: emptyPlayer(
        state.session?.username ?: "alumno",
        state.session?.displayName ?: "Estudiante",
    )
    val scope = rememberCoroutineScope()
    var notice by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    val canRestoreLife = player.vidas < player.vidasMax && player.xp >= WorldsCatalog.LIFE_COST_XP
    val lifeStatusText = when {
        player.vidas >= player.vidasMax -> "Tienes todas tus vidas completas."
        player.xp < WorldsCatalog.LIFE_COST_XP -> "Te faltan ${WorldsCatalog.LIFE_COST_XP - player.xp} XP para recuperar una vida."
        else -> "Recupera una vida y vuelve al reto."
    }

    ScreenFrame(state = state, vm = vm, showHud = true) {
        BackPill(onClick = { vm.popTo(Route.Menu) }, label = "← Menú")

        ScreenHeader(
            kicker = "INTERCAMBIOS DE AVENTURA",
            title = "Tienda de recompensas",
            subtitle = "Canjea tu experiencia por vidas o adquiere los 4 trofeos oficiales.",
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatChip("Vidas", "❤️ ${player.vidas}/${player.vidasMax}", Modifier.weight(1f), accent = Aq.Red)
            StatChip("Experiencia", "⭐ ${player.xp}/${player.xpMax}", Modifier.weight(1f), accent = Aq.Mint)
            StatChip("Monedas", "🪙 ${player.monedas}", Modifier.weight(1f), accent = Aq.Gold)
        }

        MessageBanner(error ?: state.error, notice ?: state.notice)

        // ---------- Canje de Vida Extra (25 XP) ----------
        AqCard(Modifier.fillMaxWidth(), accent = Aq.Red) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("RECUPERACIÓN", color = Aq.Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("❤️ Una vida extra", color = Aq.Text, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text(lifeStatusText, color = Aq.Muted, fontSize = 13.sp)
                }
                Text("${WorldsCatalog.LIFE_COST_XP} XP", color = Aq.Gold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            AqButton(
                text = "Canjear vida por ${WorldsCatalog.LIFE_COST_XP} XP",
                onClick = {
                    busy = true
                    error = null
                    notice = null
                    scope.launch {
                        vm.restoreLife()
                            .onSuccess {
                                busy = false
                                notice = "¡Vida recuperada! Ya puedes volver a tu misión."
                            }
                            .onFailure {
                                busy = false
                                error = it.message
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                style = AqButtonStyle.Gold,
                enabled = !busy && canRestoreLife,
                loading = busy,
            )
        }

        // ---------- Colección de los 4 Trofeos ----------
        SectionTitle("Colección de Trofeos (aparecen en Logros)")
        TROPHIES.forEach { trophy ->
            val owned = trophy.id in player.trofeos
            val affordable = player.monedas >= trophy.precio

            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Aq.Panel)
                    .border(
                        width = if (owned) 2.dp else 1.dp,
                        color = if (owned) Aq.Green else Aq.Gold.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp),
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(trophy.icono, fontSize = 36.sp)
                    Column(Modifier.weight(1f)) {
                        Text(trophy.nombre, color = Aq.Gold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(trophy.descripcion, color = Aq.Muted, fontSize = 13.sp)
                    }
                }
                val btnText = when {
                    owned -> "✅ En tu vitrina"
                    affordable -> "Comprar · ${trophy.precio} 🪙"
                    else -> "Faltan ${trophy.precio - player.monedas} 🪙"
                }
                AqButton(
                    text = btnText,
                    onClick = {
                        busy = true
                        error = null
                        notice = null
                        scope.launch {
                            vm.buyTrophy(trophy.id)
                                .onSuccess {
                                    busy = false
                                    notice = "${trophy.icono} ${trophy.nombre} agregado a tu vitrina."
                                }
                                .onFailure {
                                    busy = false
                                    error = it.message
                                }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    style = if (owned) AqButtonStyle.Ghost else AqButtonStyle.Primary,
                    enabled = !busy && !owned && affordable,
                )
            }
        }

        AqButton(
            text = "🏆 Ver mi vitrina y logros →",
            onClick = { vm.navigate(Route.Achievements) },
            modifier = Modifier.fillMaxWidth(),
            style = AqButtonStyle.Ghost,
        )
    }
}
