package com.algebraquest.backend

import kotlinx.serialization.Serializable

// ---------- Contenido (worlds.json) ----------
@Serializable data class Teoria(val definicion: String, val ejemplo: String)
@Serializable data class Pregunta(
    val tipo: String, val enunciado: String, val opciones: List<String>,
    val correcta: Int, val xp: Int, val monedas: Int,
)
@Serializable data class Nivel(
    val id: Int, val titulo: String, val icono: String, val videoUrl: String? = null,
    val teoria: Teoria, val preguntas: List<Pregunta>,
)
@Serializable data class Mundo(
    val id: String, val nombre: String, val icono: String, val color: String, val descripcion: String,
    val desbloqueado: Boolean = false, val requiere: String? = null, val niveles: List<Nivel>,
)

// Versión pública: nunca incluye la respuesta correcta.
@Serializable data class PreguntaPublica(
    val tipo: String, val enunciado: String, val opciones: List<String>, val xp: Int, val monedas: Int,
)
@Serializable data class NivelPublico(
    val id: Int, val titulo: String, val icono: String, val videoUrl: String? = null,
    val teoria: Teoria, val preguntas: List<PreguntaPublica>,
)
@Serializable data class MundoPublico(
    val id: String, val nombre: String, val icono: String, val color: String, val descripcion: String,
    val desbloqueado: Boolean, val requiere: String? = null, val niveles: List<NivelPublico>,
)

fun Mundo.toPublic() = MundoPublico(
    id, nombre, icono, color, descripcion, desbloqueado, requiere,
    niveles.map { n ->
        NivelPublico(
            n.id, n.titulo, n.icono, n.videoUrl, n.teoria,
            n.preguntas.map { PreguntaPublica(it.tipo, it.enunciado, it.opciones, it.xp, it.monedas) }
        )
    }
)

// ---------- Cuentas y sesiones ----------
@Serializable data class StoredUser(val username: String, val password: String, val displayName: String, val role: String)
@Serializable data class LoginRequest(val username: String, val password: String)
@Serializable data class UserDto(val username: String, val displayName: String, val role: String)
@Serializable data class LoginResponse(val token: String, val user: UserDto)
@Serializable data class ApiError(val message: String)

@Serializable data class BiometricEnrollRequest(val biometricType: String = "FINGERPRINT_OR_FACE")
@Serializable data class BiometricEnrollResponse(val deviceToken: String, val username: String = "", val role: String = "")
@Serializable data class BiometricLoginRequest(val username: String, val deviceToken: String, val biometricType: String = "FINGERPRINT_OR_FACE")

// ---------- Jugador ----------
@Serializable data class PlayerState(
    val username: String, val displayName: String,
    val nivel: Int = 1, val xp: Int = 0, val xpMax: Int = 100,
    val monedas: Int = 0, val vidas: Int = 3, val vidasMax: Int = 3,
    val trofeos: List<String> = emptyList(),
    val completados: List<String> = emptyList(), // "mundoId_nivelId"
)
@Serializable data class PlayerLoginRequest(val username: String = "", val displayName: String = "")
@Serializable data class SavePlayerRequest(
    val xp: Int? = null,
    val xpMax: Int? = null,
    val nivel: Int? = null,
    val vidas: Int? = null,
    val monedas: Int? = null,
    val trofeos: List<String>? = null,
    val completados: List<String>? = null,
)
@Serializable data class ResetPlayerRequest(val username: String? = null)
@Serializable data class BuyTrophyRequest(val trophyId: String)
@Serializable data class AnswerRequest(
    val worldId: String, val levelId: Int, val questionIndex: Int, val option: String, val durationMs: Long = 0,
)
@Serializable data class AnswerResponse(
    val correct: Boolean, val xpGained: Int, val coinsGained: Int, val points: Int,
    val leveledUp: Boolean, val outOfLives: Boolean, val streak: Int, val player: PlayerState,
)
@Serializable data class CheckRequest(val worldId: String, val levelId: Int, val questionIndex: Int, val option: String)
@Serializable data class CheckResponse(val correct: Boolean)
@Serializable data class LevelCompleteRequest(val worldId: String, val levelId: Int)
@Serializable data class RenameRequest(val displayName: String)
@Serializable data class PlayerResponse(val player: PlayerState)

// ---------- Ranking / aula / admin ----------
@Serializable data class RankingEntry(
    val nombre: String,
    val username: String,
    val nivel: Int,
    val xp: Int,
    val monedas: Int,
    val trofeos: Int = 0,
)
@Serializable data class ClassroomEntry(
    val nombre: String, val puntos: Int, val correctas: Int, val respuestas: Int,
    val precision: Double, val tiempoPromedio: Long,
)
@Serializable data class StudentProgress(
    val nombre: String, val username: String, val nivel: Int, val xp: Int,
    val nivelesCompletados: Int, val nivelesTotales: Int, val porMundo: Map<String, Int>,
)
@Serializable data class AnswerLogEntry(
    val timestamp: Long, val nombre: String, val mundoId: String, val nivelId: Int,
    val pregunta: String, val correcta: Boolean, val puntos: Int, val duracionMs: Long,
)
@Serializable data class WorldStatus(
    val id: String, val nombre: String, val icono: String, val niveles: Int, val preguntas: Int,
    val jugadoresActivos: Int, val mundoCompletadoPor: Int,
)
@Serializable data class SessionInfo(val id: String, val username: String, val role: String, val expiresAt: Long, val biometric: Boolean)
@Serializable data class AdminOverview(
    val administradores: Int, val supervisores: Int, val alumnos: Int,
    val sesionesActivas: Int, val mundos: List<WorldStatus>, val sesiones: List<SessionInfo>,
)
