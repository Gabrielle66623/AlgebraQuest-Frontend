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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.algrebraquest.ui.theme.ProgressBar
import com.example.algrebraquest.ui.theme.ScreenHeader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class TutorialStep(val label: String, val text: String, val hint: String)

/**
 * R-09 · Tutorial interactivo por tema (TutorialScreen.kt).
 * Basado en js/views/tutorial.js:
 * - Reproductor de 3 pasos (OBJETIVO, IDEA CLAVE, EJEMPLO GUIADO) con reproducción automática y manual.
 * - Enlace / modal de video de apoyo de álgebra (videoUrl).
 * - Notas teóricas y reto rápido de práctica validado con /content/check.
 */
@Composable
fun TutorialScreen(worldId: String, levelId: Int, state: UiState, vm: AppViewModel) {
    val pair = WorldsCatalog.level(state.worlds, worldId, levelId)
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

    ScreenFrame(state = state, vm = vm, showHud = true) {
        if (pair == null) {
            BackPill(onClick = { vm.popTo(Route.TutorialsList) }, label = "← Biblioteca")
            AqCard(Modifier.fillMaxWidth()) {
                Text("No se encontró el tutorial solicitado.", color = Aq.Red)
            }
            return@ScreenFrame
        }

        val (mundo, nivel) = pair
        val topicColor = Aq.parse(mundo.color, Aq.Mint)

        val steps = remember(mundo.id, nivel.id) {
            listOf(
                TutorialStep(
                    "OBJETIVO",
                    "Hoy repasamos ${nivel.titulo}. Observa la regla y luego aplícala en un ejemplo.",
                    "Empieza con la idea general.",
                ),
                TutorialStep(
                    "IDEA CLAVE",
                    nivel.teoria.definicion,
                    "Quédate con esta regla antes de resolver.",
                ),
                TutorialStep(
                    "EJEMPLO GUIADO",
                    nivel.teoria.ejemplo,
                    "Ahora prueba una pregunta del tema.",
                ),
            )
        }

        var currentStep by remember(mundo.id, nivel.id) { mutableStateOf(0) }
        var playing by remember(mundo.id, nivel.id) { mutableStateOf(false) }
        var showVideoPanel by remember(mundo.id, nivel.id) { mutableStateOf(false) }

        val firstQuestion = nivel.preguntas.firstOrNull()
        val shuffledReviewOptions = remember(mundo.id, nivel.id) {
            firstQuestion?.opciones?.shuffled().orEmpty()
        }
        var selectedReviewOption by remember(mundo.id, nivel.id) { mutableStateOf<String?>(null) }
        var lastWrongReviewOption by remember(mundo.id, nivel.id) { mutableStateOf<String?>(null) }
        var reviewSolved by remember(mundo.id, nivel.id) { mutableStateOf(false) }
        var reviewFeedback by remember(mundo.id, nivel.id) { mutableStateOf<Pair<Boolean, String>?>(null) }

        LaunchedEffect(playing, currentStep) {
            if (playing) {
                delay(4200)
                if (currentStep < steps.lastIndex) {
                    currentStep++
                } else {
                    playing = false
                }
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackPill(onClick = { vm.back() }, label = "← Biblioteca")
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Aq.Panel2)
                    .border(1.dp, topicColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text("${mundo.icono} ${mundo.nombre}", color = topicColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        ScreenHeader(
            kicker = "LECCIÓN ${nivel.id} · REPASO GUIADO",
            title = "${nivel.icono} ${nivel.titulo}",
        )

        // ---------- Reproductor interactivo de repaso (lesson-player en tutorial.js) ----------
        val step = steps[currentStep]
        AqCard(Modifier.fillMaxWidth(), accent = topicColor) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("ÁLGEBRA QUEST · PIZARRA", color = Aq.Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("0${currentStep + 1} / 0${steps.size}", color = Aq.Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Aq.Bg)
                    .border(1.dp, Aq.Border, RoundedCornerShape(14.dp))
                    .padding(18.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(step.label, color = Aq.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    Text(step.text, color = Aq.Text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, lineHeight = 25.sp)
                }
            }

            ProgressBar(fraction = (currentStep + 1).toFloat() / steps.size.toFloat(), color = topicColor)

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AqButton(
                    text = "←",
                    onClick = { currentStep = (currentStep - 1 + steps.size) % steps.size },
                    style = AqButtonStyle.Ghost,
                )
                AqButton(
                    text = if (playing) "Ⅱ Pausar" else "▶ Reproducir",
                    onClick = { playing = !playing },
                    style = AqButtonStyle.Gold,
                )
                AqButton(
                    text = "→",
                    onClick = { currentStep = (currentStep + 1) % steps.size },
                    style = AqButtonStyle.Ghost,
                )
            }
            Text(step.hint, color = Aq.Muted, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())

            val videoUrl = nivel.videoUrl ?: "https://www.youtube.com/watch?v=JVym35FdT5A"
            AqButton(
                text = "▶ Ver video de apoyo del tema ↗",
                onClick = { showVideoPanel = !showVideoPanel },
                modifier = Modifier.fillMaxWidth(),
                style = AqButtonStyle.Primary,
            )

            if (showVideoPanel) {
                AqCard(Modifier.fillMaxWidth(), accent = Aq.Gold) {
                    Text("🎬 VIDEO DE APOYO · ${nivel.titulo}", color = Aq.Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        "Enlace de clase recomendado: $videoUrl",
                        color = Aq.Muted,
                        fontSize = 12.sp,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AqButton(
                            text = "Abrir en YouTube ↗",
                            onClick = { runCatching { uriHandler.openUri(videoUrl) } },
                            modifier = Modifier.weight(1f),
                            style = AqButtonStyle.Gold,
                        )
                        AqButton(
                            text = "Cerrar",
                            onClick = { showVideoPanel = false },
                            modifier = Modifier.weight(1f),
                            style = AqButtonStyle.Ghost,
                        )
                    }
                }
            }
        }

        // ---------- Notas del tema (lesson-notes en tutorial.js) ----------
        AqCard(Modifier.fillMaxWidth()) {
            Text("REGLA DEL NIVEL", color = Aq.Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(nivel.teoria.definicion, color = Aq.Text, fontSize = 14.sp)
            Text("EJEMPLO", color = Aq.Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(nivel.teoria.ejemplo, color = Aq.GoldSoft, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        // ---------- Reto rápido de práctica sin perder vidas (review-quiz en tutorial.js) ----------
        if (firstQuestion != null) {
            AqCard(Modifier.fillMaxWidth(), accent = Aq.Purple) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("RETO RÁPIDO", color = Aq.Mint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("+ práctica sin perder vidas", color = Aq.GoldSoft, fontSize = 12.sp)
                }
                Text(firstQuestion.enunciado, color = Aq.Gold, fontSize = 18.sp, fontWeight = FontWeight.Bold)

                shuffledReviewOptions.forEach { opt ->
                    val isSelected = selectedReviewOption == opt
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Aq.Purple.copy(alpha = 0.45f) else Aq.Panel2)
                            .border(
                                1.5.dp,
                                if (isSelected) Aq.Gold else Aq.Border,
                                RoundedCornerShape(12.dp),
                            )
                            .clickable(enabled = !reviewSolved) {
                                selectedReviewOption = opt
                                reviewFeedback = null
                            }
                            .padding(12.dp),
                    ) {
                        Text(opt, color = Aq.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                AqButton(
                    text = if (reviewSolved) "¡Respuesta correcta!" else "Comprobar respuesta",
                    onClick = {
                        val chosen = selectedReviewOption ?: return@AqButton
                        scope.launch {
                            vm.request { check(mundo.id, nivel.id, 0, chosen) }
                                .onSuccess { ok ->
                                    if (ok) {
                                        reviewSolved = true
                                        reviewFeedback = true to "¡Lo tienes! El reto del nivel te dará XP y monedas."
                                    } else {
                                        lastWrongReviewOption = chosen
                                        reviewFeedback = false to "Buen intento. Revisa el ejemplo y elige otra respuesta."
                                    }
                                }
                                .onFailure {
                                    // Validación de respaldo con la primera opción si no hay red
                                    val ok = chosen == firstQuestion.opciones.firstOrNull()
                                    reviewSolved = ok
                                    reviewFeedback = if (ok) {
                                        true to "¡Lo tienes! El reto del nivel te dará XP y monedas."
                                    } else {
                                        false to "Buen intento. Revisa el ejemplo y elige otra respuesta."
                                    }
                                }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    style = AqButtonStyle.Gold,
                    enabled = !reviewSolved && selectedReviewOption != null && selectedReviewOption != lastWrongReviewOption,
                )

                reviewFeedback?.let { (ok, msg) ->
                    Text(
                        text = if (ok) "✅ $msg" else "⚠ $msg",
                        color = if (ok) Aq.Green else Aq.Red,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        // ---------- Pie de navegación ----------
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AqButton(
                text = "← Elegir otro tema",
                onClick = { vm.popTo(Route.TutorialsList) },
                modifier = Modifier.weight(1f),
                style = AqButtonStyle.Ghost,
            )
            AqButton(
                text = "Ir al reto del nivel →",
                onClick = { vm.navigate(Route.LevelIntro(mundo.id, nivel.id)) },
                modifier = Modifier.weight(1f),
                style = AqButtonStyle.Gold,
            )
        }
    }
}
