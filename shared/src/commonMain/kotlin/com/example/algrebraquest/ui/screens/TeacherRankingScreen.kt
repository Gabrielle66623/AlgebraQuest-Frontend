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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.algrebraquest.data.AnswerLogEntry
import com.example.algrebraquest.data.ClassroomEntry
import com.example.algrebraquest.data.Role
import com.example.algrebraquest.data.StudentProgress
import com.example.algrebraquest.platform.BiometricMode
import com.example.algrebraquest.platform.BiometricStatus
import com.example.algrebraquest.ui.AppViewModel
import com.example.algrebraquest.ui.ScreenFrame
import com.example.algrebraquest.ui.UiState
import com.example.algrebraquest.ui.theme.Aq
import com.example.algrebraquest.ui.theme.AqButton
import com.example.algrebraquest.ui.theme.AqButtonStyle
import com.example.algrebraquest.ui.theme.AqCard
import com.example.algrebraquest.ui.theme.BackPill
import com.example.algrebraquest.ui.theme.LabelValueRow
import com.example.algrebraquest.ui.theme.Loading
import com.example.algrebraquest.ui.theme.MessageBanner
import com.example.algrebraquest.ui.theme.ProgressBar
import com.example.algrebraquest.ui.theme.ScreenHeader
import com.example.algrebraquest.ui.theme.SectionTitle
import com.example.algrebraquest.ui.theme.StatChip
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * R-03 · Panel de Supervisor (SupervisorScreen / TeacherRankingScreen.kt).
 * Muestra el ranking del aula filtrado por mundo (A-14: /api/ranking/aula),
 * el avance de alumnos por mundo y la bitácora de respuestas en tiempo real (A-10),
 * además del control biométrico del supervisor y cierre de sesión (R-12).
 */
@Composable
fun SupervisorScreen(state: UiState, vm: AppViewModel) = TeacherRankingScreen(state, vm)

@Composable
fun TeacherRankingScreen(state: UiState, vm: AppViewModel) {
    var selectedWorld by remember { mutableStateOf<String?>(null) }
    var activeTab by remember { mutableStateOf(0) } // 0 = Ranking Aula, 1 = Avance Alumnos, 2 = Bitácora
    var ranking by remember { mutableStateOf<List<ClassroomEntry>>(emptyList()) }
    var progress by remember { mutableStateOf<List<StudentProgress>>(emptyList()) }
    var logEntries by remember { mutableStateOf<List<AnswerLogEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun refresh(worldId: String? = selectedWorld) {
        loading = true
        errorMsg = null
        scope.launch {
            val r1 = vm.request { classroomRanking(worldId) }
            val r2 = vm.request { classroomProgress() }
            val r3 = vm.request { classroomLog(60) }
            r1.onSuccess { ranking = it }.onFailure { errorMsg = it.message }
            r2.onSuccess { progress = it }
            r3.onSuccess { logEntries = it }
            loading = false
        }
    }

    // Actualización periódica cada 10s igual que en teacherRanking.js
    LaunchedEffect(selectedWorld) {
        while (true) {
            refresh(selectedWorld)
            delay(10_000)
        }
    }

    val sessionUser = state.session?.username ?: "supervisor"
    val bioEnrolled = state.isBiometricEnrolledFor(sessionUser)
    val isAdminViewing = state.session?.role == Role.ADMINISTRADOR

    ScreenFrame(state = state, vm = vm, showHud = false) {
        if (isAdminViewing) {
            BackPill(onClick = { vm.back() }, label = "← Volver a Panel Administrador")
        }

        ScreenHeader(
            kicker = "PANEL DOCENTE · ${state.session?.displayName ?: "SUPERVISOR"}",
            title = "Evaluación y Ranking de Aula",
            subtitle = "Resultados por mundo, avance individual y bitácora de respuestas en vivo.",
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AqButton("↻ Actualizar", { refresh() }, Modifier.weight(1f), style = AqButtonStyle.Ghost)
            AqButton("⏻ Cerrar sesión", { vm.logout() }, Modifier.weight(1f), style = AqButtonStyle.Danger)
        }

        MessageBanner(errorMsg ?: state.error, state.notice)

        // ---------- Biometría para el rol SUPERVISOR ----------
        if (!isAdminViewing) {
            AqCard(Modifier.fillMaxWidth(), accent = Aq.Mint) {
                Text("🔐 Sensor Biométrico · Rol Supervisor", color = Aq.Mint, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (state.biometricStatus == BiometricStatus.AVAILABLE) {
                    Text(
                        if (bioEnrolled) "✓ Huella y reconocimiento facial activos para @$sessionUser."
                        else "Activa huella dactilar o reconocimiento facial para abrir el panel docente rápidamente.",
                        color = Aq.Muted,
                        fontSize = 12.sp,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AqButton("🫆 Huella", { vm.enrollBiometric(BiometricMode.HUELLA) }, Modifier.weight(1f), style = AqButtonStyle.Gold)
                        AqButton("😊 Rostro", { vm.enrollBiometric(BiometricMode.ROSTRO) }, Modifier.weight(1f), style = AqButtonStyle.Ghost)
                    }
                    if (bioEnrolled) {
                        AqButton(
                            "Desactivar biometría",
                            { vm.disableBiometric(sessionUser) },
                            Modifier.fillMaxWidth(),
                            style = AqButtonStyle.Danger,
                        )
                    }
                }
            }
        }

        // ---------- Resumen de métricas de aula ----------
        val totalAnswers = ranking.sumOf { it.respuestas }
        val leader = ranking.firstOrNull()?.nombre ?: "—"
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatChip("Jugadores", "${ranking.size}", Modifier.weight(1f), accent = Aq.Gold)
            StatChip("Respuestas", "$totalAnswers", Modifier.weight(1f), accent = Aq.Mint)
            StatChip("Líder actual", leader, Modifier.weight(1f), accent = Aq.Green)
        }

        // ---------- Filtro por Mundo ----------
        SectionTitle("Filtrar por Mundo")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChipItem(
                label = "🌎 Todos",
                selected = selectedWorld == null,
                onClick = { selectedWorld = null },
                modifier = Modifier.weight(1f),
            )
            state.worlds.forEach { w ->
                FilterChipItem(
                    label = "${w.icono} ${w.nombre.take(8)}",
                    selected = selectedWorld == w.id,
                    onClick = { selectedWorld = w.id },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // ---------- Selector de vista (Ranking / Avance / Bitácora) ----------
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("🏆 Ranking Aula", "📈 Avance Alumnos", "📋 Bitácora").forEachIndexed { index, title ->
                FilterChipItem(
                    label = title,
                    selected = activeTab == index,
                    onClick = { activeTab = index },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (loading && ranking.isEmpty() && progress.isEmpty()) {
            Loading()
        }

        when (activeTab) {
            0 -> {
                // Tab 0: Ranking del aula por mundo (teacherRanking.js)
                if (ranking.isEmpty()) {
                    AqCard(Modifier.fillMaxWidth()) {
                        Text("Aún no hay respuestas registradas para este filtro de mundo.", color = Aq.Muted, fontSize = 14.sp)
                    }
                } else {
                    val medals = listOf("🥇", "🥈", "🥉")
                    ranking.forEachIndexed { idx, row ->
                        val accent = if (idx < 3) Aq.Gold else null
                        AqCard(Modifier.fillMaxWidth(), accent = accent) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "${medals.getOrElse(idx) { "${idx + 1}." }} ${row.nombre}",
                                    color = Aq.Text,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    "${row.puntos} pts",
                                    color = Aq.Gold,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            LabelValueRow("Aciertos / Intentos", "${row.correctas} / ${row.respuestas}")
                            LabelValueRow("Precisión", "${row.precision}%")
                            val avgSec = if (row.tiempoPromedio > 0) "${(row.tiempoPromedio / 100.0).toInt() / 10.0} s" else "—"
                            LabelValueRow("Tiempo medio", avgSec)
                        }
                    }
                }
            }

            1 -> {
                // Tab 1: Avance de alumnos por mundo
                if (progress.isEmpty()) {
                    AqCard(Modifier.fillMaxWidth()) {
                        Text("No hay alumnos registrados.", color = Aq.Muted, fontSize = 14.sp)
                    }
                } else {
                    progress.forEach { st ->
                        val fraction = if (st.nivelesTotales > 0) st.nivelesCompletados.toFloat() / st.nivelesTotales else 0f
                        AqCard(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("🎓 ${st.nombre} (@${st.username})", color = Aq.Gold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("⭐ Nivel ${st.nivel} · ${st.xp} XP", color = Aq.Mint, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Text(
                                "Progreso total: ${st.nivelesCompletados} de ${st.nivelesTotales} niveles completados",
                                color = Aq.Text,
                                fontSize = 13.sp,
                            )
                            ProgressBar(fraction = fraction, color = Aq.Green)
                            state.worlds.forEach { w ->
                                val doneInWorld = st.porMundo[w.id] ?: 0
                                LabelValueRow("${w.icono} ${w.nombre}", "$doneInWorld / ${w.niveles.size} niveles")
                            }
                        }
                    }
                }
            }

            2 -> {
                // Tab 2: Bitácora de respuestas (A-10)
                val filteredLog = logEntries.filter { selectedWorld == null || it.mundoId == selectedWorld }
                if (filteredLog.isEmpty()) {
                    AqCard(Modifier.fillMaxWidth()) {
                        Text("Todavía no hay respuestas registradas en la bitácora.", color = Aq.Muted, fontSize = 14.sp)
                    }
                } else {
                    filteredLog.forEach { entry ->
                        AqCard(
                            Modifier.fillMaxWidth(),
                            accent = if (entry.correcta) Aq.Green else Aq.Red,
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "${if (entry.correcta) "✅" else "❌"} ${entry.nombre}",
                                    color = if (entry.correcta) Aq.Green else Aq.Red,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                )
                                Text(
                                    "${entry.mundoId} · Nivel ${entry.nivelId}",
                                    color = Aq.GoldSoft,
                                    fontSize = 12.sp,
                                )
                            }
                            Text(entry.pregunta, color = Aq.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            val sec = (entry.duracionMs / 100.0).toInt() / 10.0
                            Text(
                                if (entry.correcta) "+${entry.puntos} pts · Tiempo: $sec s"
                                else "Respuesta incorrecta · Tiempo: $sec s",
                                color = Aq.Muted,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Aq.Purple else Aq.Panel2)
            .border(1.dp, if (selected) Aq.Gold else Aq.Border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) Aq.Gold else Aq.Text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}
