package com.example.algrebraquest.domain

import androidx.compose.runtime.Immutable
import com.example.algrebraquest.data.PlayerState

/** Rutas de navegación de la app (R-01 … R-12). */
@Immutable
sealed interface Route {
    data object Login : Route                                   // R-01
    data object Administrator : Route                           // R-02
    data object Supervisor : Route                              // R-03
    data object Home : Route                                    // R-04 Panel de Alumno
    data object Menu : Route                                    // R-05
    data object Worlds : Route                                  // R-06
    data class LevelSelect(val worldId: String) : Route         // R-07
    data class LevelIntro(val worldId: String, val levelId: Int) : Route
    data class Game(val worldId: String, val levelId: Int) : Route   // R-08
    data object TutorialsList : Route                           // R-09
    data class Tutorial(val worldId: String, val levelId: Int) : Route
    data object Store : Route                                   // R-10
    data object Achievements : Route
    data object Ranking : Route                                 // R-11
    data object Settings : Route
}

/** Puntos de referencia de un jugador sin datos (antes de cargar el perfil). */
fun emptyPlayer(username: String, displayName: String) = PlayerState(username, displayName)
