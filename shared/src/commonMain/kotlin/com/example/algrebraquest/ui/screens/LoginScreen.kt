package com.example.algrebraquest.ui.screens

import algrebraquest.shared.generated.resources.Res
import algrebraquest.shared.generated.resources.personaje
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.algrebraquest.data.Role
import com.example.algrebraquest.platform.BiometricMode
import com.example.algrebraquest.platform.BiometricStatus
import com.example.algrebraquest.ui.AppViewModel
import com.example.algrebraquest.ui.UiState
import com.example.algrebraquest.ui.theme.Aq
import com.example.algrebraquest.ui.theme.AqBackground
import com.example.algrebraquest.ui.theme.AqButton
import com.example.algrebraquest.ui.theme.AqButtonStyle
import com.example.algrebraquest.ui.theme.AqCard
import com.example.algrebraquest.ui.theme.AqTextField
import com.example.algrebraquest.ui.theme.MessageBanner
import org.jetbrains.compose.resources.painterResource

private data class RoleAccountPreset(
    val role: Role,
    val subtitle: String,
    val equation: String,
    val demoUsers: List<Pair<String, String>>,
)

private val ROLE_PRESETS = listOf(
    RoleAccountPreset(
        role = Role.ALUMNO,
        subtitle = "Resuelve misiones en los 4 mundos, gana XP, monedas y trofeos.",
        equation = "2x + 3 = 11",
        demoUsers = listOf(
            "alumno" to "Alumno123!",
            "ana" to "Ana12345!",
            "luis" to "Luis12345!",
        ),
    ),
    RoleAccountPreset(
        role = Role.SUPERVISOR,
        subtitle = "Evalúa el ranking de aula por mundo, avance de alumnos y bitácora en vivo.",
        equation = "∑ f(xᵢ) · Δx",
        demoUsers = listOf("supervisor" to "Supervisor123!"),
    ),
    RoleAccountPreset(
        role = Role.ADMINISTRADOR,
        subtitle = "Controla cuentas, estado de los 4 mundos, sesiones activas y reinicios.",
        equation = "det(A − λI) = 0",
        demoUsers = listOf("admin" to "Admin123!"),
    ),
)

/**
 * R-01 · Pantalla principal de Inicio de Sesión rediseñada.
 * Soporta los 3 roles (ALUMNO, SUPERVISOR, ADMINISTRADOR), inicio con usuario/contraseña,
 * inicio con sensor biométrico (huella dactilar y reconocimiento facial) por rol y ajuste de servidor Ktor.
 */
@Composable
fun LoginScreen(state: UiState, vm: AppViewModel) {
    var selectedRole by remember { mutableStateOf(Role.ALUMNO) }
    var username by remember { mutableStateOf(state.biometricUser ?: "alumno") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showServer by remember { mutableStateOf(false) }
    var serverUrl by remember(state.serverUrl) { mutableStateOf(state.serverUrl) }

    LaunchedEffect(Unit) {
        vm.refreshBiometric()
        vm.checkServerHealth()
    }

    val currentPreset = ROLE_PRESETS.first { it.role == selectedRole }
    val roleBiometricCred = state.credentialForRole(selectedRole) ?: state.biometricCredentials.firstOrNull()
    val sensorAvailable = state.biometricStatus == BiometricStatus.AVAILABLE

    AqBackground {
        Column(
            Modifier
                .fillMaxSize()
                .safeContentPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ---------- Cabecera Hero principal ----------
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Aq.Panel, Aq.Panel2, Color(0xFF1E1450))
                        )
                    )
                    .border(1.5.dp, Aq.Gold.copy(alpha = 0.55f), RoundedCornerShape(22.dp))
                    .padding(18.dp),
            ) {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "PORTAL DE ACCESO",
                            color = Aq.Mint,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                        )
                        val healthColor = when (state.serverHealthy) {
                            true -> Aq.Green
                            false -> Aq.Red
                            null -> Aq.Gold
                        }
                        val healthText = when (state.serverHealthy) {
                            true -> "● Backend activo"
                            false -> "● Backend offline"
                            null -> "● Verificando..."
                        }
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Aq.Bg.copy(alpha = 0.7f))
                                .border(1.dp, healthColor.copy(alpha = 0.6f), RoundedCornerShape(50))
                                .clickable { vm.checkServerHealth() }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(healthText, color = healthColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Image(
                            painterResource(Res.drawable.personaje),
                            contentDescription = "Personaje de Álgebra Quest",
                            modifier = Modifier
                                .size(92.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .border(2.dp, Aq.Gold, RoundedCornerShape(22.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("ÁLGEBRA", color = Aq.Text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                            Text("QUEST", color = Aq.Gold, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 36.sp)
                            Text(
                                "Aprende Álgebra Universitaria jugando",
                                color = Aq.Muted,
                                fontSize = 13.sp,
                            )
                            Text(
                                currentPreset.equation,
                                color = Aq.Mint,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            // ---------- Selector de Rol (Alumno / Supervisor / Administrador) ----------
            AqCard(Modifier.fillMaxWidth(), accent = Aq.Gold) {
                Text(
                    "Perfil de acceso",
                    color = Aq.GoldSoft,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Role.entries.forEach { role ->
                        val active = selectedRole == role
                        val roleCred = state.credentialForRole(role)
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (active) Aq.Purple else Aq.Panel2)
                                .border(
                                    width = if (active) 2.dp else 1.dp,
                                    color = if (active) Aq.Gold else Aq.Border,
                                    shape = RoundedCornerShape(14.dp),
                                )
                                .clickable {
                                    selectedRole = role
                                    val preset = ROLE_PRESETS.first { it.role == role }
                                    username = roleCred?.username ?: preset.demoUsers.first().first
                                    password = ""
                                    vm.clearMessages()
                                }
                                .padding(vertical = 10.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(role.badge, fontSize = 20.sp)
                            Text(
                                role.title,
                                color = if (active) Color.White else Aq.Text,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            )
                            if (roleCred != null) {
                                Text(
                                    "Biometría activa",
                                    color = Aq.Green,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
                Text(currentPreset.subtitle, color = Aq.Muted, fontSize = 12.sp)

                // Chips de autocompletado rápido para pruebas de cada rol
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Cuentas:", color = Aq.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    currentPreset.demoUsers.forEach { (demoUser, demoPass) ->
                        val isSelectedUser = username.equals(demoUser, ignoreCase = true)
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (isSelectedUser) Aq.Gold.copy(alpha = 0.2f) else Aq.Panel2)
                                .border(
                                    1.dp,
                                    if (isSelectedUser) Aq.Gold else Aq.Border,
                                    RoundedCornerShape(50),
                                )
                                .clickable {
                                    username = demoUser
                                    password = demoPass
                                    vm.clearMessages()
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                        ) {
                            Text(
                                demoUser,
                                color = if (isSelectedUser) Aq.Gold else Aq.Text,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            // ---------- Formulario de Usuario y Contraseña ----------
            AqCard(Modifier.fillMaxWidth(), accent = Aq.Purple) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Credenciales · ${selectedRole.title}",
                        color = Aq.Gold,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(selectedRole.badge, fontSize = 20.sp)
                }

                AqTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = "Usuario",
                    enabled = !state.loading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )
                AqTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Contraseña",
                    enabled = !state.loading,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailing = {
                        Text(
                            if (showPassword) "Ocultar" else "Mostrar",
                            modifier = Modifier
                                .clickable { showPassword = !showPassword }
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                            color = Aq.GoldSoft,
                            fontSize = 12.sp,
                        )
                    },
                )

                MessageBanner(state.error, state.notice)

                AqButton(
                    text = "Entrar como ${selectedRole.title} →",
                    onClick = {
                        vm.login(username, password)
                        password = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.loading,
                    loading = state.loading,
                )
            }

            // ---------- Panel de Sensor Biométrico (Huella Dactilar y Reconocimiento Facial) ----------
            AqCard(Modifier.fillMaxWidth(), accent = Aq.Mint) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Acceso biométrico",
                            color = Aq.Mint,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Inicia sesión con huella dactilar o reconocimiento facial en cualquiera de los 3 roles.",
                            color = Aq.Muted,
                            fontSize = 12.sp,
                        )
                    }
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Aq.Panel2)
                            .border(1.dp, Aq.Mint, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(selectedRole.badge, fontSize = 18.sp)
                    }
                }

                if (state.biometricCredentials.isNotEmpty()) {
                    Text(
                        "Cuentas con biometría registrada en este dispositivo:",
                        color = Aq.GoldSoft,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.biometricCredentials.forEach { cred ->
                            val isCurrentTarget = cred.username.equals(username, ignoreCase = true) ||
                                cred.role.equals(selectedRole.name, ignoreCase = true)
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isCurrentTarget) Aq.Panel2 else Aq.Bg.copy(alpha = 0.5f))
                                    .border(
                                        1.dp,
                                        if (isCurrentTarget) Aq.Mint else Aq.Border,
                                        RoundedCornerShape(10.dp),
                                    )
                                    .clickable {
                                        username = cred.username
                                        selectedRole = Role.parse(cred.role)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(
                                        "${Role.parse(cred.role).badge} ${cred.displayName} (${cred.username})",
                                        color = Aq.Text,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        "Rol: ${cred.role} · Sensor listo",
                                        color = Aq.Mint,
                                        fontSize = 11.sp,
                                    )
                                }
                                Text(
                                    "Usar →",
                                    color = Aq.Gold,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }

                // Dos botones dedicados: Huella Dactilar y Reconocimiento Facial
                val targetUser = state.biometricCredentials
                    .firstOrNull { it.username.equals(username, ignoreCase = true) }?.username
                    ?: roleBiometricCred?.username

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AqButton(
                        text = "Huella dactilar",
                        onClick = {
                            vm.biometricLogin(
                                mode = BiometricMode.HUELLA,
                                targetUsername = targetUser,
                                targetRole = selectedRole,
                            )
                        },
                        modifier = Modifier.weight(1f),
                        style = AqButtonStyle.Gold,
                        enabled = !state.loading && sensorAvailable,
                    )
                    AqButton(
                        text = "Reconocimiento facial",
                        onClick = {
                            vm.biometricLogin(
                                mode = BiometricMode.ROSTRO,
                                targetUsername = targetUser,
                                targetRole = selectedRole,
                            )
                        },
                        modifier = Modifier.weight(1f),
                        style = AqButtonStyle.Ghost,
                        enabled = !state.loading && sensorAvailable,
                    )
                }

                val statusNote = when {
                    !sensorAvailable && state.biometricStatus == BiometricStatus.NOT_ENROLLED ->
                        "Registra al menos una huella o rostro en Ajustes de Android para habilitar el lector."
                    !sensorAvailable ->
                        "El sensor biométrico no está disponible en este dispositivo."
                    roleBiometricCred != null ->
                        "Sensor listo para entrar como ${roleBiometricCred.displayName} (${roleBiometricCred.role})."
                    else ->
                        "Inicia sesión una vez con contraseña en el rol elegido para vincular tu huella y rostro."
                }
                Text(statusNote, color = Aq.Muted, fontSize = 12.sp)
            }

            // ---------- Configuración del Servidor Ktor ----------
            Text(
                if (showServer) "▾ Ocultar configuración de servidor (${state.serverUrl})"
                else "▸ Configurar servidor Ktor (${state.serverUrl})",
                color = Aq.Muted,
                fontSize = 13.sp,
                modifier = Modifier
                    .clickable { showServer = !showServer }
                    .padding(6.dp),
            )
            if (showServer) {
                AqCard(Modifier.fillMaxWidth()) {
                    Text(
                        "Dirección del backend Ktor (A-01 /health).\n• Emulador Android: http://10.0.2.2:8080\n• Dispositivo físico en red local: http://TU-IP-LOCAL:8080",
                        color = Aq.Muted,
                        fontSize = 12.sp,
                    )
                    AqTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        label = "URL del servidor Ktor",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        AqButton(
                            text = "Guardar URL",
                            onClick = { vm.setServerUrl(serverUrl) },
                            modifier = Modifier.weight(1f),
                            style = AqButtonStyle.Gold,
                        )
                        AqButton(
                            text = "Probar /health",
                            onClick = { vm.checkServerHealth() },
                            modifier = Modifier.weight(1f),
                            style = AqButtonStyle.Ghost,
                        )
                    }
                }
            }
        }
    }
}
