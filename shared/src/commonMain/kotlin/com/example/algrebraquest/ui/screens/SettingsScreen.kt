package com.example.algrebraquest.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.algrebraquest.data.Role
import com.example.algrebraquest.domain.Route
import com.example.algrebraquest.domain.emptyPlayer
import com.example.algrebraquest.platform.BiometricMode
import com.example.algrebraquest.platform.BiometricStatus
import com.example.algrebraquest.ui.AppViewModel
import com.example.algrebraquest.ui.ScreenFrame
import com.example.algrebraquest.ui.UiState
import com.example.algrebraquest.ui.theme.Aq
import com.example.algrebraquest.ui.theme.AqButton
import com.example.algrebraquest.ui.theme.AqButtonStyle
import com.example.algrebraquest.ui.theme.AqCard
import com.example.algrebraquest.ui.theme.AqTextField
import com.example.algrebraquest.ui.theme.BackPill
import com.example.algrebraquest.ui.theme.MessageBanner
import com.example.algrebraquest.ui.theme.ScreenHeader
import kotlinx.coroutines.launch

/**
 * R-11 · Configuración y Ajustes (SettingsScreen.kt).
 * Basada en js/views/settings.js:
 * - Cambio de nombre visible en perfil y ranking (A-07: /api/jugadores/renombrar).
 * - Configuración del sensor biométrico (huella dactilar y reconocimiento facial) para la cuenta activa.
 * - Reinicio de progreso con confirmación (A-08: /api/jugadores/reset).
 * - Configuración de servidor y cierre de sesión (R-12: /auth/logout).
 */
@Composable
fun SettingsScreen(state: UiState, vm: AppViewModel) {
    val player = state.player ?: emptyPlayer(
        state.session?.username ?: "alumno",
        state.session?.displayName ?: "Estudiante",
    )
    val isStudent = state.session?.role == Role.ALUMNO
    val sessionUser = state.session?.username ?: player.username
    val bioEnrolled = state.isBiometricEnrolledFor(sessionUser)

    var newName by remember(player.displayName) { mutableStateOf(player.displayName) }
    var serverUrl by remember(state.serverUrl) { mutableStateOf(state.serverUrl) }
    var confirmReset by remember { mutableStateOf(false) }
    var localNotice by remember { mutableStateOf<String?>(null) }
    var localError by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    ScreenFrame(state = state, vm = vm, showHud = isStudent) {
        BackPill(onClick = { vm.back() }, label = "← Volver")

        ScreenHeader(
            kicker = "PERFIL · BIOMETRÍA · SERVIDOR",
            title = "⚙️ Configuración",
            subtitle = "Administra tu nombre visible, sensor biométrico y progreso de cuenta.",
        )

        MessageBanner(localError ?: state.error, localNotice ?: state.notice)

        // ---------- 1. Cambiar nombre del jugador (A-07: /api/jugadores/renombrar) ----------
        if (isStudent) {
            AqCard(Modifier.fillMaxWidth(), accent = Aq.Gold) {
                Text("👤 Nombre visible del jugador", color = Aq.Gold, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Este nombre aparece en la barra superior (HUD), en el ranking general y en el ranking de aula.",
                    color = Aq.Muted,
                    fontSize = 13.sp,
                )
                AqTextField(
                    value = newName,
                    onValueChange = { if (it.length <= 20) newName = it },
                    label = "Nombre (máx. 20 caracteres)",
                    enabled = !busy,
                )
                AqButton(
                    text = "Guardar nombre",
                    onClick = {
                        val trimmed = newName.trim()
                        if (trimmed.isEmpty()) {
                            localError = "Escribe un nombre válido."
                            return@AqButton
                        }
                        busy = true
                        localError = null
                        localNotice = null
                        scope.launch {
                            vm.rename(trimmed)
                                .onSuccess {
                                    busy = false
                                    localNotice = "Nombre actualizado a ${it.displayName}."
                                }
                                .onFailure {
                                    busy = false
                                    localError = it.message
                                }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    style = AqButtonStyle.Gold,
                    enabled = !busy && newName.trim().isNotEmpty() && newName.trim() != player.displayName,
                    loading = busy,
                )
            }
        }

        // ---------- 2. Sensor Biométrico (Huella Dactilar y Reconocimiento Facial) ----------
        AqCard(Modifier.fillMaxWidth(), accent = Aq.Mint) {
            Text(
                "🫆😊 Sensor Biométrico (${state.session?.role?.title ?: "Cuenta"})",
                color = Aq.Mint,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
            if (state.biometricStatus == BiometricStatus.AVAILABLE) {
                Text(
                    if (bioEnrolled) "✓ Huella dactilar y reconocimiento facial activos para @$sessionUser."
                    else "Vincula este dispositivo para iniciar sesión con huella dactilar o reconocimiento facial.",
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
                        text = "Desactivar acceso biométrico de @$sessionUser",
                        onClick = { vm.disableBiometric(sessionUser) },
                        modifier = Modifier.fillMaxWidth(),
                        style = AqButtonStyle.Danger,
                    )
                }
            } else {
                Text(
                    "El sensor biométrico no está disponible o no hay huellas/rostro registrados en los ajustes del sistema.",
                    color = Aq.Muted,
                    fontSize = 13.sp,
                )
            }
        }

        // ---------- 3. Reiniciar Progreso del Jugador (A-08: /api/jugadores/reset) ----------
        if (isStudent) {
            AqCard(Modifier.fillMaxWidth(), accent = Aq.Red) {
                Text("🗑 Reiniciar progreso", color = Aq.Red, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Esto reiniciará tu XP, monedas, nivel, vidas, trofeos y niveles completados a sus valores iniciales (tu usuario y nombre se conservan).",
                    color = Aq.Muted,
                    fontSize = 13.sp,
                )
                if (!confirmReset) {
                    AqButton(
                        text = "🗑 Reiniciar todo mi progreso",
                        onClick = { confirmReset = true },
                        modifier = Modifier.fillMaxWidth(),
                        style = AqButtonStyle.Danger,
                    )
                } else {
                    Text(
                        "¿Seguro que quieres borrar todo tu avance?",
                        color = Aq.Gold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AqButton(
                            text = "Sí, reiniciar",
                            onClick = {
                                busy = true
                                confirmReset = false
                                scope.launch {
                                    vm.resetProgress()
                                        .onSuccess {
                                            busy = false
                                            localNotice = "Progreso reiniciado correctamente."
                                        }
                                        .onFailure {
                                            busy = false
                                            localError = it.message
                                        }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            style = AqButtonStyle.Danger,
                        )
                        AqButton(
                            text = "Cancelar",
                            onClick = { confirmReset = false },
                            modifier = Modifier.weight(1f),
                            style = AqButtonStyle.Ghost,
                        )
                    }
                }
            }
        }

        // ---------- 4. Dirección del Servidor Backend ----------
        AqCard(Modifier.fillMaxWidth()) {
            Text("🌐 Servidor Backend Ktor", color = Aq.GoldSoft, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            AqTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = "URL del servidor",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )
            AqButton(
                text = "Actualizar URL del servidor",
                onClick = { vm.setServerUrl(serverUrl) },
                modifier = Modifier.fillMaxWidth(),
                style = AqButtonStyle.Ghost,
            )
        }

        // ---------- 5. Cerrar Sesión (R-12: /auth/logout) ----------
        AqCard(Modifier.fillMaxWidth()) {
            Text("¿Deseas cambiar de cuenta o rol en este dispositivo?", color = Aq.Muted, fontSize = 13.sp)
            AqButton(
                text = "🔄 Cerrar sesión y volver al Login",
                onClick = { vm.logout() },
                modifier = Modifier.fillMaxWidth(),
                style = AqButtonStyle.Primary,
            )
        }
    }
}
