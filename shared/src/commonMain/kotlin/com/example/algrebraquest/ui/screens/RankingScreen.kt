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
import androidx.compose.runtime.LaunchedEffect
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
import com.example.algrebraquest.data.RankingEntry
import com.example.algrebraquest.data.Role
import com.example.algrebraquest.domain.Route
import com.example.algrebraquest.ui.AppViewModel
import com.example.algrebraquest.ui.ScreenFrame
import com.example.algrebraquest.ui.UiState
import com.example.algrebraquest.ui.theme.Aq
import com.example.algrebraquest.ui.theme.AqButton
import com.example.algrebraquest.ui.theme.AqButtonStyle
import com.example.algrebraquest.ui.theme.AqCard
import com.example.algrebraquest.ui.theme.BackPill
import com.example.algrebraquest.ui.theme.Loading
import com.example.algrebraquest.ui.theme.MessageBanner
import com.example.algrebraquest.ui.theme.ScreenHeader
import kotlinx.coroutines.launch

/**
 * R-11 · Ranking general de jugadores (RankingScreen.kt).
 * Basada en js/views/ranking.js y A-13 (GET /api/ranking):
 * Lista a los jugadores ordenados por nivel, XP, trofeos y monedas, resaltando al jugador actual.
 */
@Composable
fun RankingScreen(state: UiState, vm: AppViewModel) {
    var entries by remember { mutableStateOf<List<RankingEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        loading = true
        errorMsg = null
        scope.launch {
            vm.request { ranking() }
                .onSuccess { entries = it }
                .onFailure { errorMsg = it.message }
            loading = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    val currentUsername = state.session?.username
    val isStudent = state.session?.role == Role.ALUMNO

    ScreenFrame(state = state, vm = vm, showHud = isStudent) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackPill(onClick = { vm.back() }, label = "← Regresar")
            AqButton("↻ Actualizar", { refresh() }, style = AqButtonStyle.Ghost)
        }

        ScreenHeader(
            kicker = "TABLA DE POSICIONES · GENERAL",
            title = "🏆 Ranking Álgebra Quest",
            subtitle = "Los mejores jugadores ordenados por nivel, experiencia (XP) y trofeos obtenidos.",
        )

        MessageBanner(errorMsg ?: state.error, state.notice)

        if (loading && entries.isEmpty()) {
            Loading()
        } else if (entries.isEmpty()) {
            AqCard(Modifier.fillMaxWidth()) {
                Text("Todavía no hay jugadores registrados en el ranking.", color = Aq.Muted, fontSize = 14.sp)
            }
        } else {
            val medals = listOf("🥇", "🥈", "🥉")
            entries.forEachIndexed { index, item ->
                val isMe = currentUsername != null && item.username.equals(currentUsername, ignoreCase = true)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(15.dp))
                        .background(if (isMe) Aq.Panel2 else Aq.Panel)
                        .border(
                            width = if (isMe) 2.dp else 1.dp,
                            color = if (isMe) Aq.Gold else Aq.Border,
                            shape = RoundedCornerShape(15.dp),
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = medals.getOrElse(index) { "${index + 1}." },
                            color = Aq.Gold,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Column {
                            Text(
                                text = "${item.nombre}${if (isMe) " (Tú)" else ""}",
                                color = if (isMe) Aq.Gold else Aq.Text,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "@${item.username} · ⭐ Nivel ${item.nivel}",
                                color = Aq.Muted,
                                fontSize = 12.sp,
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("${item.xp} XP", color = Aq.Mint, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            "🪙 ${item.monedas} · 🏅 ${item.trofeos}",
                            color = Aq.GoldSoft,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }

        if (isStudent) {
            AqButton(
                text = "⚙️ Cambiar mi nombre o ajustes",
                onClick = { vm.navigate(Route.Settings) },
                modifier = Modifier.fillMaxWidth(),
                style = AqButtonStyle.Ghost,
            )
        }
    }
}
