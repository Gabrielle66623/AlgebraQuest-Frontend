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
import com.example.algrebraquest.data.AdminOverview
import com.example.algrebraquest.data.StudentProgress
import com.example.algrebraquest.domain.Route
import com.example.algrebraquest.platform.BiometricMode
import com.example.algrebraquest.platform.BiometricStatus
import com.example.algrebraquest.ui.AppViewModel
import com.example.algrebraquest.ui.ScreenFrame
import com.example.algrebraquest.ui.UiState
import com.example.algrebraquest.ui.theme.Aq
import com.example.algrebraquest.ui.theme.AqButton
import com.example.algrebraquest.ui.theme.AqButtonStyle
import com.example.algrebraquest.ui.theme.AqCard
import com.example.algrebraquest.ui.theme.LabelValueRow
import com.example.algrebraquest.ui.theme.Loading
import com.example.algrebraquest.ui.theme.MessageBanner
import com.example.algrebraquest.ui.theme.ScreenHeader
import com.example.algrebraquest.ui.theme.SectionTitle
import com.example.algrebraquest.ui.theme.StatChip
import kotlinx.coroutines.launch

/**
 * R-02 · Panel de Administrador (AdministradorScreen).
 * Muestra resumen de cuentas, estado de los 4 mundos, control de sesiones activas,
 * reinicio de progreso de alumnos (A-08), vinculación biométrica del administrador y cierre de sesión (R-12).
 */
@Composable
fun AdministradorScreen(state: UiState, vm: AppViewModel) {
    var overview by remember { mutableStateOf<AdminOverview?>(null) }
    var students by remember { mutableStateOf<List<StudentProgress>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var localMessage by remember { mutableStateOf<String?>(null) }
    var localError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        loading = true
        localError = null
        scope.launch {
            val ov = vm.request { adminOverview() }
            val pr = vm.request { classroomProgress() }
            ov.onSuccess { overview = it }.onFailure { localError = it.message }
            pr.onSuccess { students = it }
            loading = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    val sessionUser = state.session?.username ?: "admin"
    val bioEnrolled = state.isBiometricEnrolledFor(sessionUser)

    ScreenFrame(state = state, vm = vm, showHud = false) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ScreenHeader(
                kicker = "PANEL ADMINISTRATIVO · ${state.session?.displayName ?: "ADMINISTRADOR"}",
                title = "Control General",
                subtitle = "Resumen de cuentas, estado de los 4 mundos y control de sesiones.",
                modifier = Modifier.weight(1f),
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AqButton("↻ Actualizar", { refresh() }, Modifier.weight(1f), style = AqButtonStyle.Ghost)
            AqButton("📊 Ver Aula", { vm.navigate(Route.Supervisor) }, Modifier.weight(1f), style = AqButtonStyle.Gold)
            AqButton("⏻ Salir", { vm.logout() }, Modifier.weight(1f), style = AqButtonStyle.Danger)
        }

        MessageBanner(localError ?: state.error, localMessage ?: state.notice)

        // ---------- Tarjeta de Biometría para el rol ADMINISTRADOR ----------
        AqCard(Modifier.fillMaxWidth(), accent = Aq.Mint) {
            Text("🔐 Sensor Biométrico · Rol Administrador", color = Aq.Mint, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            if (state.biometricStatus == BiometricStatus.AVAILABLE) {
                Text(
                    if (bioEnrolled) "✓ Huella dactilar y reconocimiento facial activos para @$sessionUser."
                    else "Vincula tu huella dactilar o reconocimiento facial para entrar al panel sin escribir contraseña.",
                    color = Aq.Muted,
                    fontSize = 13.sp,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AqButton(
                        text = "🫆 Vincular Huella",
                        onClick = { vm.enrollBiometric(BiometricMode.HUELLA) },
                        modifier = Modifier.weight(1f),
                        style = AqButtonStyle.Gold,
                    )
                    AqButton(
                        text = "😊 Vincular Rostro",
                        onClick = { vm.enrollBiometric(BiometricMode.ROSTRO) },
                        modifier = Modifier.weight(1f),
                        style = AqButtonStyle.Ghost,
                    )
                }
                if (bioEnrolled) {
                    AqButton(
                        text = "Desactivar biometría de Administrador",
                        onClick = { vm.disableBiometric(sessionUser) },
                        modifier = Modifier.fillMaxWidth(),
                        style = AqButtonStyle.Danger,
                    )
                }
            } else {
                Text("El sensor biométrico no está disponible o no tiene datos registrados en el dispositivo.", color = Aq.Muted, fontSize = 12.sp)
            }
        }

        if (loading && overview == null) {
            Loading()
        }

        overview?.let { data ->
            // ---------- 1. Resumen de Cuentas ----------
            SectionTitle("Resumen de Cuentas y Sesiones")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip("Admins", "${data.administradores}", Modifier.weight(1f), accent = Aq.Gold)
                StatChip("Supervisores", "${data.supervisores}", Modifier.weight(1f), accent = Aq.Mint)
                StatChip("Alumnos", "${data.alumnos}", Modifier.weight(1f), accent = Aq.Green)
                StatChip("Sesiones", "${data.sesionesActivas}", Modifier.weight(1f), accent = Aq.Glow)
            }

            // ---------- 2. Estado de los 4 Mundos ----------
            SectionTitle("Estado de los 4 Mundos")
            data.mundos.forEach { w ->
                AqCard(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${w.icono} ${w.nombre}", color = Aq.Gold, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text("${w.niveles} niveles · ${w.preguntas} reactivos", color = Aq.Muted, fontSize = 12.sp)
                    }
                    LabelValueRow("Alumnos con actividad en este mundo", "${w.jugadoresActivos}")
                    LabelValueRow("Alumnos que completaron el mundo al 100%", "${w.mundoCompletadoPor}")
                }
            }

            // ---------- 3. Control de Sesiones Activas ----------
            SectionTitle("Control de Sesiones Activas (${data.sesiones.size})")
            if (data.sesiones.isEmpty()) {
                AqCard(Modifier.fillMaxWidth()) {
                    Text("No hay sesiones activas en este momento.", color = Aq.Muted, fontSize = 14.sp)
                }
            } else {
                data.sesiones.forEach { ses ->
                    AqCard(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "👤 ${ses.username} (${ses.role})",
                                    color = Aq.Text,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                )
                                Text(
                                    "ID: ${ses.id} · ${if (ses.biometric) "🫆 Biométrica" else "🔑 Contraseña"}",
                                    color = Aq.Muted,
                                    fontSize = 12.sp,
                                )
                            }
                            AqButton(
                                text = "Revocar",
                                onClick = {
                                    scope.launch {
                                        vm.request { revokeSession(ses.id) }
                                            .onSuccess {
                                                localMessage = "Sesión ${ses.id} revocada"
                                                refresh()
                                            }
                                            .onFailure { localError = it.message }
                                    }
                                },
                                style = AqButtonStyle.Danger,
                            )
                        }
                    }
                }
            }
        }

        // ---------- 4. Gestión y Reinicio de Alumnos (A-08) ----------
        SectionTitle("Gestión de Alumnos (Reinicio de Progreso)")
        students.forEach { st ->
            AqCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("🎓 ${st.nombre} (@${st.username})", color = Aq.Text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            "Nivel ${st.nivel} · ${st.xp} XP · ${st.nivelesCompletados}/${st.nivelesTotales} niveles superados",
                            color = Aq.Muted,
                            fontSize = 12.sp,
                        )
                    }
                    AqButton(
                        text = "🗑 Reiniciar",
                        onClick = {
                            scope.launch {
                                vm.resetProgress(st.username)
                                    .onSuccess {
                                        localMessage = "Progreso de ${st.nombre} reiniciado a valores iniciales."
                                        refresh()
                                    }
                                    .onFailure { localError = it.message }
                            }
                        },
                        style = AqButtonStyle.Ghost,
                    )
                }
            }
        }
    }
}
