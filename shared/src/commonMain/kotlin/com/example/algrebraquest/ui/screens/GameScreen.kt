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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.algrebraquest.data.AnswerRequest
import com.example.algrebraquest.data.Mundo
import com.example.algrebraquest.domain.GameEngine
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
import com.example.algrebraquest.ui.theme.ProgressBar
import com.example.algrebraquest.ui.theme.ScreenHeader
import com.example.algrebraquest.ui.theme.StatChip
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * R-08 · Pantalla de Partida / Juego (GameScreen.kt).
 * Basada en js/views/game.js y GameEngine.kt:
 * - Soporta reactivos de opción múltiple ("opcion") y de arrastrar/colocar ("arrastrar").
 * - Cronómetro en vivo, contador de racha, validación contra el backend (A-10: /api/juego/registrar-respuesta),
 *   pantalla de nivel completado (A-09: /api/juego/completar-nivel) con desbloqueo de mundos y pantalla sin vidas (A-11).
 */
@Composable
fun GameScreen(worldId: String, levelId: Int, state: UiState, vm: AppViewModel) {
    val pair = WorldsCatalog.level(state.worlds, worldId, levelId)
    val player = state.player ?: emptyPlayer(
        state.session?.username ?: "alumno",
        state.session?.displayName ?: "Estudiante",
    )
    val scope = rememberCoroutineScope()

    var attemptKey by remember(worldId, levelId) { mutableStateOf(0) }
    val engine = remember(worldId, levelId, attemptKey) {
        pair?.let { (w, l) -> GameEngine(w, l) }
    }

    var currentStepState by remember(engine) { mutableStateOf(0) }
    var shuffledOptions by remember(engine, currentStepState) {
        mutableStateOf(engine?.shuffledOptionsForCurrent().orEmpty())
    }
    var selectedOption by remember(engine, currentStepState) { mutableStateOf<String?>(null) }
    var lastWrongOption by remember(engine, currentStepState) { mutableStateOf<String?>(null) }
    var correctOptionConfirmed by remember(engine, currentStepState) { mutableStateOf<String?>(null) }
    var statusMessage by remember(engine, currentStepState) {
        mutableStateOf("Elige una respuesta para comenzar.")
    }
    var statusTone by remember(engine, currentStepState) { mutableStateOf("neutral") } // neutral, ok, error
    var submitting by remember { mutableStateOf(false) }
    var elapsedDeciseconds by remember(engine, currentStepState) { mutableStateOf(0L) }
    var timerRunning by remember(engine, currentStepState) { mutableStateOf(true) }

    var levelFinished by remember(engine) { mutableStateOf(false) }
    var outOfLives by remember(engine) { mutableStateOf(player.vidas <= 0) }
    var newlyUnlockedWorlds by remember(engine) { mutableStateOf<List<Mundo>>(emptyList()) }
    var recoverMessage by remember { mutableStateOf<String?>(null) }

    // Reloj en vivo (cada 200ms como en game.js)
    LaunchedEffect(engine, currentStepState, timerRunning, levelFinished, outOfLives) {
        while (timerRunning && !levelFinished && !outOfLives) {
            delay(200)
            elapsedDeciseconds += 2
        }
    }

    ScreenFrame(state = state, vm = vm, showHud = true) {
        if (pair == null || engine == null) {
            BackPill(onClick = { vm.popTo(Route.Worlds) }, label = "← Volver a Mundos")
            AqCard(Modifier.fillMaxWidth()) {
                Text("No se encontró el nivel solicitado.", color = Aq.Red)
            }
            return@ScreenFrame
        }

        val (mundo, nivel) = pair

        // ---------------- Caso 1: Sin vidas (_sinVidas en game.js) ----------------
        if (outOfLives) {
            BackPill(onClick = { vm.popTo(Route.LevelSelect(mundo.id)) }, label = "← Volver a niveles")
            AqCard(Modifier.fillMaxWidth(), accent = Aq.Red) {
                Text("💔 Te quedaste sin vidas", color = Aq.Gold, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("${nivel.titulo} · ${mundo.nombre}", color = Aq.Muted, fontSize = 15.sp)
                Text(
                    "Canjea ${WorldsCatalog.LIFE_COST_XP} XP por una vida para continuar tu misión.",
                    color = Aq.Text,
                    fontSize = 15.sp,
                )
                if (recoverMessage != null) {
                    Text(recoverMessage!!, color = Aq.Red, fontSize = 13.sp)
                }
                AqButton(
                    text = "❤️ Recuperar vida · ${WorldsCatalog.LIFE_COST_XP} XP",
                    onClick = {
                        submitting = true
                        recoverMessage = null
                        scope.launch {
                            vm.restoreLife()
                                .onSuccess {
                                    submitting = false
                                    outOfLives = false
                                    attemptKey++
                                }
                                .onFailure {
                                    submitting = false
                                    recoverMessage = it.message
                                }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    style = AqButtonStyle.Gold,
                    enabled = !submitting && player.xp >= WorldsCatalog.LIFE_COST_XP,
                    loading = submitting,
                )
                AqButton(
                    text = "🛒 Abrir tienda de trofeos",
                    onClick = { vm.navigate(Route.Store) },
                    modifier = Modifier.fillMaxWidth(),
                    style = AqButtonStyle.Ghost,
                )
            }
            return@ScreenFrame
        }

        // ---------------- Caso 2: Nivel completado (_nivelCompletado en game.js) ----------------
        if (levelFinished) {
            BackPill(onClick = { vm.popTo(Route.LevelSelect(mundo.id)) }, label = "← Volver a niveles")
            val nextLevel = mundo.niveles.firstOrNull { it.id == nivel.id + 1 }

            AqCard(Modifier.fillMaxWidth(), accent = Aq.Gold) {
                Text("🏆 ¡Nivel completado!", color = Aq.Gold, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("${nivel.titulo} — ${mundo.nombre}", color = Aq.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatChip(
                        label = "Aciertos",
                        value = "✅ ${engine.aciertos} / ${engine.totalQuestions}",
                        modifier = Modifier.weight(1f),
                        accent = Aq.Green,
                    )
                    StatChip(
                        label = "Puntos de aula",
                        value = "⚡ ${engine.puntosQuiz}",
                        modifier = Modifier.weight(1f),
                        accent = Aq.Gold,
                    )
                }

                if (newlyUnlockedWorlds.isNotEmpty()) {
                    AqCard(Modifier.fillMaxWidth(), accent = Aq.Green) {
                        Text(
                            "🌟 ¡Mundo completado! Desbloqueaste:",
                            color = Aq.Green,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        )
                        newlyUnlockedWorlds.forEach { w ->
                            Text("${w.icono} ${w.nombre}", color = Aq.Gold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }

                AqButton(
                    text = "🔁 Repetir nivel",
                    onClick = {
                        levelFinished = false
                        attemptKey++
                    },
                    modifier = Modifier.fillMaxWidth(),
                    style = AqButtonStyle.Primary,
                )

                if (nextLevel != null) {
                    AqButton(
                        text = "➡ Siguiente nivel (${nextLevel.titulo})",
                        onClick = { vm.replace(Route.LevelIntro(mundo.id, nextLevel.id)) },
                        modifier = Modifier.fillMaxWidth(),
                        style = AqButtonStyle.Gold,
                    )
                }

                if (newlyUnlockedWorlds.isNotEmpty()) {
                    AqButton(
                        text = "🌎 Ver nuevos mundos",
                        onClick = { vm.popTo(Route.Worlds) },
                        modifier = Modifier.fillMaxWidth(),
                        style = AqButtonStyle.Ghost,
                    )
                }
            }
            return@ScreenFrame
        }

        // ---------------- Caso 3: Pregunta activa (_renderPregunta en game.js) ----------------
        val question = engine.currentQuestion ?: return@ScreenFrame
        val secondsStr = "${elapsedDeciseconds / 10}.${elapsedDeciseconds % 10} s"

        BackPill(onClick = { vm.popTo(Route.LevelSelect(mundo.id)) }, label = "← Salir del nivel")

        ScreenHeader(
            kicker = "${mundo.icono} ${mundo.nombre.uppercase()} · PREGUNTA ${currentStepState + 1} DE ${engine.totalQuestions}",
            title = nivel.titulo,
        )

        ProgressBar(fraction = engine.progressFraction, color = Aq.Gold)

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("⏱ $secondsStr", color = Aq.GoldSoft, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("🔥 Racha ${engine.racha}", color = Aq.Mint, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("+${question.xp} XP · +${question.monedas} 🪙", color = Aq.Muted, fontSize = 13.sp)
        }

        AqCard(Modifier.fillMaxWidth(), accent = Aq.Purple) {
            Text(
                text = question.enunciado,
                color = Aq.Gold,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )

            // Si es de tipo "arrastrar", mostramos la zona receptora interactiva (como #zonaRespuesta en game.js)
            if (question.esArrastrar) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selectedOption != null) Aq.Panel2 else Aq.Bg)
                        .border(
                            width = 2.dp,
                            color = if (selectedOption != null) Aq.Gold else Aq.Mint.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(14.dp),
                        )
                        .clickable(enabled = selectedOption != null && correctOptionConfirmed == null) {
                            selectedOption = null
                        }
                        .padding(vertical = 18.dp, horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = selectedOption ?: "⬇ Arrastra o toca una pieza para colocarla aquí",
                        color = if (selectedOption != null) Aq.Gold else Aq.Muted,
                        fontSize = 16.sp,
                        fontWeight = if (selectedOption != null) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // Opciones / Piezas mezcladas
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                shuffledOptions.forEach { option ->
                    val isSelected = selectedOption == option
                    val isConfirmedCorrect = correctOptionConfirmed == option
                    val isLastWrong = lastWrongOption == option

                    val borderColor = when {
                        isConfirmedCorrect -> Aq.Green
                        isLastWrong -> Aq.Red
                        isSelected -> Aq.Gold
                        else -> Aq.Border
                    }
                    val bgColor = when {
                        isConfirmedCorrect -> Aq.Green.copy(alpha = 0.22f)
                        isLastWrong -> Aq.Red.copy(alpha = 0.20f)
                        isSelected -> Aq.Purple.copy(alpha = 0.45f)
                        else -> Aq.Panel2
                    }

                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(bgColor)
                            .border(2.dp, borderColor, RoundedCornerShape(14.dp))
                            .clickable(enabled = !submitting && correctOptionConfirmed == null) {
                                selectedOption = option
                                if (option == lastWrongOption) {
                                    statusMessage = "Esa opción ya la intentaste. Elige otra para seguir."
                                    statusTone = "error"
                                } else {
                                    statusMessage = if (question.esArrastrar) {
                                        "Respuesta colocada en la zona. Comprueba tu idea."
                                    } else {
                                        "¡Buena elección! Pulsa comprobar cuando estés listo."
                                    }
                                    statusTone = "neutral"
                                }
                            }
                            .padding(vertical = 14.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = option,
                            color = Aq.Text,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            val canSubmit = !submitting &&
                correctOptionConfirmed == null &&
                selectedOption != null &&
                selectedOption != lastWrongOption

            AqButton(
                text = if (correctOptionConfirmed != null) "¡Respuesta correcta!" else "Comprobar respuesta",
                onClick = {
                    val chosen = selectedOption ?: return@AqButton
                    submitting = true
                    val durationMs = elapsedDeciseconds * 100L
                    scope.launch {
                        vm.answer(
                            AnswerRequest(
                                worldId = mundo.id,
                                levelId = nivel.id,
                                questionIndex = engine.currentQuestionOriginalIndex,
                                option = chosen,
                                durationMs = durationMs,
                            )
                        ).onSuccess { res ->
                            submitting = false
                            engine.recordResult(res.correct, res.points, res.streak)
                            if (res.correct) {
                                timerRunning = false
                                correctOptionConfirmed = chosen
                                val streakSuffix = if (engine.racha > 1) " · Racha x${engine.racha}" else ""
                                val levelUpSuffix = if (res.leveledUp) " ¡Subiste de nivel!" else ""
                                statusMessage = "¡Correcto! +${res.xpGained} XP y +${res.coinsGained} monedas. +${res.points} pts$streakSuffix$levelUpSuffix"
                                statusTone = "ok"
                                delay(850)
                                if (engine.nextQuestion()) {
                                    currentStepState = engine.currentStep
                                } else {
                                    // Completar nivel en el servidor (A-09) y detectar mundos recién desbloqueados
                                    val lockedBefore = state.worlds.filter {
                                        it.requiere == mundo.id && !WorldsCatalog.worldUnlocked(res.player, it, state.worlds)
                                    }
                                    vm.completeLevel(mundo.id, nivel.id).onSuccess { updatedPlayer ->
                                        newlyUnlockedWorlds = lockedBefore.filter {
                                            WorldsCatalog.worldUnlocked(updatedPlayer, it, state.worlds)
                                        }
                                        vm.syncPlayer()
                                    }
                                    levelFinished = true
                                }
                            } else {
                                lastWrongOption = chosen
                                if (res.outOfLives) {
                                    timerRunning = false
                                    statusMessage = "Se acabaron las vidas. Usa la pista y recupérate para continuar."
                                    statusTone = "error"
                                    delay(850)
                                    outOfLives = true
                                } else {
                                    statusMessage = "Casi. −1 vida. Pista: ${nivel.teoria.definicion}"
                                    statusTone = "error"
                                }
                            }
                        }.onFailure { err ->
                            submitting = false
                            statusMessage = err.message ?: "No se pudo validar la respuesta"
                            statusTone = "error"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                style = AqButtonStyle.Gold,
                enabled = canSubmit,
                loading = submitting,
            )

            val messageColor = when (statusTone) {
                "ok" -> Aq.Green
                "error" -> Aq.Red
                else -> Aq.Muted
            }
            Text(
                text = statusMessage,
                color = messageColor,
                fontSize = 14.sp,
                fontWeight = if (statusTone == "neutral") FontWeight.Normal else FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
