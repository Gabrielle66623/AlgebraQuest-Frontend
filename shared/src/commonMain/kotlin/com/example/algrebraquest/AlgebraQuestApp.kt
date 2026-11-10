package com.example.algrebraquest

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.algrebraquest.domain.Route
import com.example.algrebraquest.platform.BiometricMode
import com.example.algrebraquest.platform.PlatformBackHandler
import com.example.algrebraquest.ui.AppViewModel
import com.example.algrebraquest.ui.screens.AchievementsScreen
import com.example.algrebraquest.ui.screens.AdministradorScreen
import com.example.algrebraquest.ui.screens.AlumnoScreen
import com.example.algrebraquest.ui.screens.GameScreen
import com.example.algrebraquest.ui.screens.LevelIntroScreen
import com.example.algrebraquest.ui.screens.LevelSelectScreen
import com.example.algrebraquest.ui.screens.LoginScreen
import com.example.algrebraquest.ui.screens.MenuScreen
import com.example.algrebraquest.ui.screens.RankingScreen
import com.example.algrebraquest.ui.screens.SettingsScreen
import com.example.algrebraquest.ui.screens.StoreScreen
import com.example.algrebraquest.ui.screens.SupervisorScreen
import com.example.algrebraquest.ui.screens.TutorialScreen
import com.example.algrebraquest.ui.screens.TutorialsListScreen
import com.example.algrebraquest.ui.screens.WorldsScreen
import com.example.algrebraquest.ui.theme.Aq
import com.example.algrebraquest.ui.theme.AqButton
import com.example.algrebraquest.ui.theme.AqButtonStyle
import com.example.algrebraquest.ui.theme.AqTheme

/**
 * Punto de entrada de navegación de AlgebraQuest (referenciado como AlgebraQuestApp.kt en Tabla 1 del inventario).
 * Elige la pantalla según la ruta activa (R-01 a R-12) y el rol que devuelve el backend Ktor,
 * y presenta el diálogo para vincular huella dactilar o reconocimiento facial al iniciar sesión.
 */
@Composable
fun AlgebraQuestApp(vm: AppViewModel = viewModel { AppViewModel() }) {
    val state by vm.state.collectAsState()

    PlatformBackHandler(enabled = state.backStack.size > 1) {
        vm.back()
    }

    AqTheme {
        Box(Modifier.fillMaxSize()) {
            when (val route = if (state.session == null) Route.Login else state.route) {
                Route.Login -> LoginScreen(state, vm)
                Route.Administrator -> AdministradorScreen(state, vm)
                Route.Supervisor -> SupervisorScreen(state, vm)
                Route.Home -> AlumnoScreen(state, vm)
                Route.Menu -> MenuScreen(state, vm)
                Route.Worlds -> WorldsScreen(state, vm)
                is Route.LevelSelect -> LevelSelectScreen(route.worldId, state, vm)
                is Route.LevelIntro -> LevelIntroScreen(route.worldId, route.levelId, state, vm)
                is Route.Game -> GameScreen(route.worldId, route.levelId, state, vm)
                Route.TutorialsList -> TutorialsListScreen(state, vm)
                is Route.Tutorial -> TutorialScreen(route.worldId, route.levelId, state, vm)
                Route.Store -> StoreScreen(state, vm)
                Route.Achievements -> AchievementsScreen(state, vm)
                Route.Ranking -> RankingScreen(state, vm)
                Route.Settings -> SettingsScreen(state, vm)
            }

            // Modal para activar Huella Dactilar o Reconocimiento Facial tras iniciar sesión con contraseña
            if (state.offerBiometric && state.session != null) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.72f))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Aq.Panel)
                            .border(2.dp, Aq.Gold, RoundedCornerShape(20.dp))
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            "🫆😊 Activar inicio de sesión biométrico",
                            color = Aq.Gold,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "¿Deseas vincular tu huella dactilar o reconocimiento facial para la cuenta ${state.session?.displayName} (${state.session?.role?.title}) en este dispositivo?",
                            color = Aq.Text,
                            fontSize = 14.sp,
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AqButton(
                                text = "🫆 Usar Huella",
                                onClick = { vm.enrollBiometric(BiometricMode.HUELLA) },
                                modifier = Modifier.weight(1f),
                                style = AqButtonStyle.Gold,
                            )
                            AqButton(
                                text = "😊 Usar Rostro",
                                onClick = { vm.enrollBiometric(BiometricMode.ROSTRO) },
                                modifier = Modifier.weight(1f),
                                style = AqButtonStyle.Primary,
                            )
                        }
                        AqButton(
                            text = "Ahora no",
                            onClick = { vm.dismissBiometricOffer() },
                            modifier = Modifier.fillMaxWidth(),
                            style = AqButtonStyle.Ghost,
                        )
                    }
                }
            }
        }
    }
}
