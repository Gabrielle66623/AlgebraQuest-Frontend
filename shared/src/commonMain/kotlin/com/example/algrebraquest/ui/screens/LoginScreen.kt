package com.example.algrebraquest.ui.screens

import algrebraquest.shared.generated.resources.Res
import algrebraquest.shared.generated.resources.blogger
import algrebraquest.shared.generated.resources.fondo1
import algrebraquest.shared.generated.resources.graduado
import algrebraquest.shared.generated.resources.logo1
import algrebraquest.shared.generated.resources.profesor
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
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
import com.example.algrebraquest.ui.theme.AqButton
import com.example.algrebraquest.ui.theme.AqButtonStyle
import com.example.algrebraquest.ui.theme.AqTextField
import com.example.algrebraquest.ui.theme.MessageBanner
import org.jetbrains.compose.resources.painterResource

private enum class AuthTab(val title: String) { LOGIN("Iniciar sesión"), REGISTER("Crear cuenta") }
private enum class LoginView { WELCOME, ROLES, LOGIN }

private val USERNAME_REGEX = Regex("^[A-Za-z0-9_.-]{3,20}$")
private const val MIN_PASSWORD = 8

private fun usernameError(v: String): String? = when {
    v.isBlank() -> "El usuario es obligatorio"
    !USERNAME_REGEX.matches(v.trim()) -> "3 a 20 caracteres: letras, números, punto, guion o guion bajo"
    else -> null
}

private fun displayNameError(v: String): String? =
    if (v.trim().length > 20) "Máximo 20 caracteres" else null

private fun passwordError(v: String): String? = when {
    v.isEmpty() -> "La contraseña es obligatoria"
    v.length < MIN_PASSWORD -> "Mínimo $MIN_PASSWORD caracteres (${v.length}/$MIN_PASSWORD)"
    else -> null
}

private fun confirmError(password: String, confirm: String): String? = when {
    confirm.isEmpty() -> "Confirma tu contraseña"
    confirm != password -> "Las contraseñas no coinciden"
    else -> null
}

// Paleta exacta del HTML (algebraquest_solo_interfaz.html)
private val BodyBg = Brush.radialGradient(
    colors = listOf(Color(0xFF2B0714), Color(0xFF14040A), Color(0xFF0A0205)),
)
private val BarBg = Brush.linearGradient(
    colors = listOf(Color(0xFF21030C), Color(0xFF4D081F), Color(0xFF750E2F)),
)
private val BarBorder = Color(0x94EBBF66) // rgba(235, 191, 102, 0.58)
private val TitlePrimary = Color(0xFFFFF8EE)
private val TitleAccent = Color(0xFFF6CF7D)
private val UptColor = Color(0xFFFFF4DE)
private val BullBg = Brush.linearGradient(
    colors = listOf(Color(0xFFFFF5E4), Color(0xFFF5CA94), Color(0xFFDF8B7B)),
)
private val ShieldBg = Brush.linearGradient(
    colors = listOf(Color(0xFFA3133D), Color(0xFF730A28), Color(0xFF440416)),
)
private val PatternFilter = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            1.02f, 0f, 0f, 0f, -43.52f,
            0f, 1.02f, 0f, 0f, -43.52f,
            0f, 0f, 1.02f, 0f, -43.52f,
            0f, 0f, 0f, 1f, 0f,
        ),
    ),
)
private val RoleBg = Brush.linearGradient(
    colors = listOf(Color(0xFF5A0821), Color(0xFF8C1135), Color(0xFF470518)),
)
private val RoleCardBg = Color(0x1FFFF5E4) // rgba(255, 245, 228, 0.12)
private val RoleCardBorder = Color(0x61F6CF7D) // rgba(246, 207, 125, 0.38)
private val RoleIconBg = Brush.linearGradient(
    colors = listOf(Color(0xFFFFF7EA), Color(0xFFF7D9AF)),
)
private val RoleLabel = Color(0xFFFFF7EB)
private val LoginOverlayBg = Brush.verticalGradient(
    colors = listOf(Color(0xFF1A030B), Color(0xFF3E0718), Color(0xFF160309)),
)

@Composable
fun LoginScreen(state: UiState, vm: AppViewModel) {
    var view by remember { mutableStateOf(LoginView.WELCOME) }
    var selectedRole by remember { mutableStateOf(Role.ALUMNO) }
    var tab by remember { mutableStateOf(AuthTab.LOGIN) }

    LaunchedEffect(Unit) {
        vm.refreshBiometric()
        vm.checkServerHealth()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(BodyBg),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().safeContentPadding()) {
            val wide = maxWidth >= 600.dp
            val frameHeight = if (wide) maxHeight.coerceAtMost(820.dp) else maxHeight
            val shape = if (wide) RoundedCornerShape(26.dp) else RoundedCornerShape(0.dp)

            Box(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (wide) 24.dp else 0.dp, vertical = if (wide) 16.dp else 0.dp),
                contentAlignment = Alignment.Center,
            ) {
                val panelModifier = Modifier
                    .then(if (wide) Modifier.widthIn(max = 410.dp).heightIn(max = frameHeight) else Modifier)
                    .fillMaxSize()
                    .clip(shape)
                    .then(
                        if (wide) {
                            Modifier.border(1.dp, TitleAccent.copy(alpha = 0.30f), shape)
                        } else {
                            Modifier
                        },
                    )

                AnimatedContent(
                    targetState = view,
                    modifier = panelModifier,
                    transitionSpec = {
                        val duration = 280
                        if (targetState == LoginView.LOGIN) {
                            (fadeIn(tween(duration)) + slideInVertically(tween(duration)) { it / 14 }) togetherWith
                                    (fadeOut(tween(duration)) + slideOutVertically(tween(duration)) { -it / 14 })
                        } else {
                            (fadeIn(tween(duration)) + slideInHorizontally(tween(duration)) { it / 18 }) togetherWith
                                    (fadeOut(tween(duration)) + slideOutHorizontally(tween(duration)) { -it / 18 })
                        }
                    },
                    label = "login-screen-transition",
                ) { targetView ->
                    when (targetView) {
                        LoginView.WELCOME -> WelcomeScreen(
                            onContinue = { view = LoginView.ROLES },
                            modifier = Modifier.fillMaxSize(),
                        )
                        LoginView.ROLES -> RoleSelectionScreen(
                            onBack = { view = LoginView.WELCOME },
                            onRoleSelected = { role ->
                                selectedRole = role
                                tab = AuthTab.LOGIN
                                vm.clearMessages()
                                view = LoginView.LOGIN
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                        LoginView.LOGIN -> WineLoginScreen(
                            state = state,
                            vm = vm,
                            role = selectedRole,
                            tab = tab,
                            onTab = { tab = it; vm.clearMessages() },
                            onBack = { view = LoginView.ROLES },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AqHtmlHeader(
    backText: String? = null,
    onBack: (() -> Unit)? = null,
) {
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(BarBg)
                .padding(top = 24.dp, bottom = 18.dp, start = 18.dp, end = 18.dp),
        ) {
            if (backText != null && onBack != null) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0x7A0F0207))
                        .border(1.dp, TitleAccent.copy(alpha = 0.35f), RoundedCornerShape(50))
                        .clickable(onClick = onBack)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        backText,
                        color = UptColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Column(
                Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Alma Daniela \n Bienvenidos a",
                    style = TextStyle(
                        color = TitlePrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.48f),
                            offset = Offset(0f, 3f),
                            blurRadius = 12f,
                        ),
                    ),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Bienvenidos a",
                    style = TextStyle(
                        color = TitlePrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.48f),
                            offset = Offset(0f, 3f),
                            blurRadius = 12f,
                        ),
                    ),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "AlgebraQuest",
                    style = TextStyle(
                        color = TitleAccent,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.8).sp,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.55f),
                            offset = Offset(0f, 4f),
                            blurRadius = 16f,
                        ),
                    ),
                )
            }
        }
        // Borde inferior dorado de 2px (.aq-header border-bottom)
        Box(Modifier.fillMaxWidth().height(2.dp).background(BarBorder))
    }
}

@Composable
private fun WelcomeScreen(onContinue: () -> Unit, modifier: Modifier) {
    val patternPainter = painterResource(Res.drawable.fondo1)
    val transition = rememberInfiniteTransition(label = "math-pattern-drift")
    val patternOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 215f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 28_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "math-pattern-offset",
    )

    Column(
        modifier.background(Color(0xFF24040E)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AqHtmlHeader()

        // Escenario central (.aq-artwork)
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(BullBg)
                .clipToBounds()
                .clickable(onClick = onContinue),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize().clipToBounds()) {
                clipRect {
                    val tileSize = 215.dp.toPx()
                    val offset = patternOffset.dp.toPx()
                    var top = -offset
                    while (top < size.height) {
                        var left = -offset
                        while (left < size.width) {
                            translate(left = left, top = top) {
                                with(patternPainter) {
                                    draw(
                                        size = Size(tileSize, tileSize),
                                        alpha = 0.96f,
                                        colorFilter = PatternFilter,
                                    )
                                }
                            }
                            left += tileSize
                        }
                        top += tileSize
                    }
                }
            }

            // Brillo radial sutil (.bull-vignette)
            Box(
                Modifier.fillMaxSize().drawWithCache {
                    val vignette = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.22f),
                            Color.Transparent,
                            Color(0x242D0512),
                        ),
                        center = Offset(size.width * 0.58f, size.height * 0.52f),
                        radius = size.maxDimension * 0.75f,
                    )
                    onDrawBehind {
                        drawRect(vignette)
                    }
                },
            )

            // Escudo vino con la máscara del toro UPT (.wine-shield)
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithCache {
                        val glow = Brush.radialGradient(
                            colors = listOf(
                                Color(0x4DFF5682),
                                Color.Transparent,
                            ),
                            center = Offset(size.width * 0.22f, size.height * 0.24f),
                            radius = size.maxDimension * 0.58f,
                        )
                        onDrawBehind {
                            drawRect(ShieldBg)
                            drawRect(glow)
                        }
                    },
            ) {
                Image(
                    painterResource(Res.drawable.logo1),
                    contentDescription = "Silueta del toro de AlgebraQuest",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { blendMode = BlendMode.DstIn },
                    contentScale = ContentScale.FillBounds,
                )
            }
        }

        // Barra inferior (.aq-footer)
        Column(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().height(2.dp).background(BarBorder))
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(BarBg)
                    .padding(top = 18.dp, bottom = 22.dp, start = 18.dp, end = 18.dp),
                contentAlignment = Alignment.Center,

            ) {
                Text(
                    text = "KEVIN GABRIEL\nUPT",
                    style = TextStyle(
                        color = UptColor,
                        fontSize = 36.sp,
                        lineHeight = 44.sp, // Controla el espacio entre las dos líneas
                        fontWeight = FontWeight.Black,
                        letterSpacing = 5.sp,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.52f),
                            offset = Offset(0f, 3f),
                            blurRadius = 14f,
                        ),
                    ),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun RoleSelectionScreen(
    onBack: () -> Unit,
    onRoleSelected: (Role) -> Unit,
    modifier: Modifier,
) {
    Column(modifier.background(RoleBg)) {
        AqHtmlHeader(backText = "← Inicio", onBack = onBack)

        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            val compactHeight = maxHeight < 420.dp
            RoleOptions(
                modifier = if (compactHeight) {
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 22.dp, vertical = 16.dp)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .height(maxHeight)
                        .padding(horizontal = 22.dp, vertical = 16.dp)
                },
                onRoleSelected = onRoleSelected,
                spread = !compactHeight,
            )
        }
    }
}

@Composable
private fun RoleOptions(
    modifier: Modifier,
    onRoleSelected: (Role) -> Unit,
    spread: Boolean,
) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "Selecciona tu rol",
            style = TextStyle(
                color = TitleAccent,
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.42f),
                    offset = Offset(0f, 2f),
                    blurRadius = 10f,
                ),
            ),
            modifier = Modifier.padding(bottom = 4.dp),
        )

        listOf(Role.ALUMNO, Role.SUPERVISOR, Role.ADMINISTRADOR).forEach { role ->
            val roleIllustration = when (role) {
                Role.ALUMNO -> Res.drawable.graduado
                Role.SUPERVISOR -> Res.drawable.profesor
                Role.ADMINISTRADOR -> Res.drawable.blogger
            }
            val roleLabelText = when (role) {
                Role.ALUMNO -> "Alumno"
                Role.SUPERVISOR -> "Profesor"
                Role.ADMINISTRADOR -> "Administrador"
            }
            val cardHeight = if (spread) Modifier.weight(1f) else Modifier

            Column(
                Modifier
                    .fillMaxWidth()
                    .then(cardHeight)
                    .clip(RoundedCornerShape(20.dp))
                    .background(RoleCardBg)
                    .border(1.5.dp, RoleCardBorder, RoundedCornerShape(20.dp))
                    .clickable { onRoleSelected(role) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
            ) {
                Box(
                    Modifier
                        .size(82.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(RoleIconBg)
                        .padding(9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painterResource(roleIllustration),
                        contentDescription = "Icono de $roleLabelText",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                }
                Text(
                    roleLabelText,
                    style = TextStyle(
                        color = RoleLabel,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.45f),
                            offset = Offset(0f, 2f),
                            blurRadius = 8f,
                        ),
                    ),
                )
            }
        }
    }
}

@Composable
private fun WineLoginScreen(
    state: UiState,
    vm: AppViewModel,
    role: Role,
    tab: AuthTab,
    onTab: (AuthTab) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier,
) {
    var username by remember(role) {
        mutableStateOf(
            state.biometricCredentials.firstOrNull { it.role.equals(role.name, ignoreCase = true) }?.username
                ?: state.biometricUser
                ?: "",
        )
    }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    Column(
        modifier
            .background(LoginOverlayBg)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AqHtmlHeader(backText = "← Roles", onBack = onBack)

        Column(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "${role.badge}  ${role.title}",
                color = TitleAccent,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            HealthPill(state, vm)

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x730A0205))
                    .border(1.dp, RoleCardBorder, RoundedCornerShape(14.dp))
                    .padding(4.dp),
            ) {
                AuthTab.entries.forEach { item ->
                    val active = item == tab
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (active) Color(0xFF8C1135) else Color.Transparent)
                            .clickable(enabled = !state.loading) { onTab(item) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            item.title,
                            color = if (active) Color.White else TitleAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            if (tab == AuthTab.LOGIN) {
                AqTextField(
                    value = username,
                    onValueChange = { username = it; if (state.error != null) vm.clearMessages() },
                    label = "Usuario",
                    enabled = !state.loading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )
                AqTextField(
                    value = password,
                    onValueChange = { password = it; if (state.error != null) vm.clearMessages() },
                    label = "Contraseña",
                    enabled = !state.loading,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailing = { PasswordToggle(showPassword) { showPassword = !showPassword } },
                )

                MessageBanner(state.error, state.notice)

                AqButton(
                    text = if (state.loading) "Validando con el servidor…" else "Entrar como ${role.title}",
                    onClick = { vm.login(username.trim(), password) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = username.isNotBlank() && password.isNotEmpty() && !state.loading,
                    loading = state.loading,
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AqButton(
                        text = "Huella",
                        onClick = { vm.biometricLogin(BiometricMode.HUELLA, targetRole = role) },
                        modifier = Modifier.weight(1f),
                        style = AqButtonStyle.Gold,
                        enabled = !state.loading && state.biometricStatus == BiometricStatus.AVAILABLE,
                    )
                    AqButton(
                        text = "Rostro",
                        onClick = { vm.biometricLogin(BiometricMode.ROSTRO, targetRole = role) },
                        modifier = Modifier.weight(1f),
                        style = AqButtonStyle.Ghost,
                        enabled = !state.loading && state.biometricStatus == BiometricStatus.AVAILABLE,
                    )
                }
            } else {
                RegisterForm(state, vm, onGoLogin = { onTab(AuthTab.LOGIN) })
            }
        }
    }
}

@Composable
private fun HealthPill(state: UiState, vm: AppViewModel) {
    val color = when (state.serverHealthy) {
        true -> Aq.Green
        false -> Aq.Red
        null -> Aq.Gold
    }
    val text = when (state.serverHealthy) {
        true -> "● Servidor en línea"
        false -> "● Sin conexión · toca para reintentar"
        null -> "● Conectando…"
    }
    Box(
        Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(Aq.Bg.copy(alpha = 0.7f))
                .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(50))
                .clickable { vm.checkServerHealth() }
                .padding(horizontal = 12.dp, vertical = 5.dp),
        ) {
            Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun PasswordToggle(visible: Boolean, onToggle: () -> Unit) {
    Text(
        if (visible) "Ocultar" else "Mostrar",
        modifier = Modifier
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        color = Aq.GoldSoft,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun RegisterForm(state: UiState, vm: AppViewModel, onGoLogin: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    var usernameTouched by remember { mutableStateOf(false) }
    var passwordTouched by remember { mutableStateOf(false) }
    var confirmTouched by remember { mutableStateOf(false) }

    val userErr = if (usernameTouched) usernameError(username) else null
    val nameErr = displayNameError(displayName)
    val passErr = if (passwordTouched) passwordError(password) else null
    val confirmErr = if (confirmTouched) confirmError(password, confirm) else null
    val valid = usernameError(username) == null && nameErr == null &&
            passwordError(password) == null && confirmError(password, confirm) == null

    Text("Crea tu cuenta de alumno", color = TitleAccent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    Text(
        "Las cuentas nuevas son siempre de alumno. Supervisores y administradores los crea la institución.",
        color = Aq.Muted,
        fontSize = 12.sp,
    )

    AqTextField(
        value = username,
        onValueChange = { username = it; usernameTouched = true; if (state.error != null) vm.clearMessages() },
        label = "Usuario",
        enabled = !state.loading,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        error = userErr,
    )
    AqTextField(
        value = displayName,
        onValueChange = { displayName = it },
        label = "Nombre visible (opcional)",
        enabled = !state.loading,
        error = nameErr,
    )
    AqTextField(
        value = password,
        onValueChange = { password = it; passwordTouched = true; if (state.error != null) vm.clearMessages() },
        label = "Contraseña",
        enabled = !state.loading,
        visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailing = { PasswordToggle(show) { show = !show } },
        error = passErr,
    )
    AqTextField(
        value = confirm,
        onValueChange = { confirm = it; confirmTouched = true; if (state.error != null) vm.clearMessages() },
        label = "Confirmar contraseña",
        enabled = !state.loading,
        visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        error = confirmErr,
    )

    MessageBanner(state.error, state.notice)

    AqButton(
        text = if (state.loading) "Creando cuenta…" else "Crear cuenta y entrar →",
        onClick = { vm.register(username.trim(), displayName.trim(), password) },
        modifier = Modifier.fillMaxWidth(),
        enabled = valid && !state.loading,
        loading = state.loading,
    )

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("¿Ya tienes cuenta? ", color = Aq.Muted, fontSize = 13.sp)
        Text(
            "Inicia sesión",
            color = TitleAccent,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(enabled = !state.loading, onClick = onGoLogin).padding(4.dp),
        )
    }
}