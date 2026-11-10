package com.example.algrebraquest.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.algrebraquest.domain.Route
import com.example.algrebraquest.ui.AppViewModel
import com.example.algrebraquest.ui.ScreenFrame
import com.example.algrebraquest.ui.UiState
import com.example.algrebraquest.ui.theme.Aq
import com.example.algrebraquest.ui.theme.AqCard
import com.example.algrebraquest.ui.theme.AqTextField
import com.example.algrebraquest.ui.theme.BackPill
import com.example.algrebraquest.ui.theme.ScreenHeader

/**
 * R-09 · Biblioteca de Tutoriales (TutorialsListScreen.kt).
 * Basada en js/views/tutorials.js: lista los 20 temas de los 4 mundos con buscador y filtros por mundo.
 */
@Composable
fun TutorialsListScreen(state: UiState, vm: AppViewModel) {
    var query by remember { mutableStateOf("") }
    var worldFilter by remember { mutableStateOf("todos") }

    val allTopics = remember(state.worlds) {
        state.worlds.flatMap { mundo ->
            mundo.niveles.map { nivel -> mundo to nivel }
        }
    }

    val filteredTopics = allTopics.filter { (mundo, nivel) ->
        val matchesWorld = worldFilter == "todos" || mundo.id == worldFilter
        val matchesQuery = query.isBlank() ||
            mundo.nombre.contains(query.trim(), ignoreCase = true) ||
            nivel.titulo.contains(query.trim(), ignoreCase = true) ||
            nivel.teoria.definicion.contains(query.trim(), ignoreCase = true)
        matchesWorld && matchesQuery
    }

    ScreenFrame(state = state, vm = vm, showHud = true) {
        BackPill(onClick = { vm.popTo(Route.Menu) }, label = "← Menú")

        ScreenHeader(
            kicker = "ARCHIVO DE CONOCIMIENTO",
            title = "Temas para repasar",
            subtitle = "Elige una lección, revisa la guía paso a paso, mira el video y prueba una pregunta.",
        )

        AqTextField(
            value = query,
            onValueChange = { query = it },
            label = "⌕ Buscar un tema o mundo",
        )

        // Filtros por mundo
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            WorldFilterPill(
                label = "Todos",
                active = worldFilter == "todos",
                onClick = { worldFilter = "todos" },
                modifier = Modifier.weight(1f),
            )
            state.worlds.forEach { mundo ->
                WorldFilterPill(
                    label = "${mundo.icono} ${mundo.nombre.take(7)}",
                    active = worldFilter == mundo.id,
                    onClick = { worldFilter = mundo.id },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (filteredTopics.isEmpty()) {
            AqCard(Modifier.fillMaxWidth()) {
                Text("No hay temas con ese nombre.", color = Aq.Muted, fontSize = 14.sp)
            }
        } else {
            filteredTopics.forEach { (mundo, nivel) ->
                val topicColor = Aq.parse(mundo.color, Aq.Gold)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Aq.Panel)
                        .border(1.dp, topicColor.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .clickable { vm.navigate(Route.Tutorial(mundo.id, nivel.id)) }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Aq.Panel2)
                            .border(1.5.dp, topicColor, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(nivel.icono, fontSize = 24.sp)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "${mundo.icono} ${mundo.nombre} · Nivel ${nivel.id}",
                            color = topicColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            nivel.titulo,
                            color = Aq.Text,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            nivel.teoria.ejemplo,
                            color = Aq.Muted,
                            fontSize = 13.sp,
                        )
                    }
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Aq.Purple),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("▶", color = Aq.Gold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun WorldFilterPill(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) Aq.Purple else Aq.Panel2)
            .border(1.dp, if (active) Aq.Gold else Aq.Border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (active) Aq.Gold else Aq.Text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}
