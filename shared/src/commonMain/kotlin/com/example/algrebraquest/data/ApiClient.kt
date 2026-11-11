package com.example.algrebraquest.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

/** URL de producción en Railway */
const val RAILWAY_BASE_URL = "https://algebraquest-backend-production.up.railway.app"

/** Error de red (status = 0) o respuesta no exitosa del backend Ktor. */
class ApiFailure(val status: Int, message: String) : Exception(message) {
    val isUnauthorized get() = status == 401
}

/** Cliente HTTP del backend AlgebraQuest (rutas A-01 a A-14 del inventario). */
class ApiClient(
    private val baseUrl: () -> String = { RAILWAY_BASE_URL }
) {
    private val http = HttpClient {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; encodeDefaults = true }) }
        install(HttpTimeout) { requestTimeoutMillis = 15_000; connectTimeoutMillis = 8_000 }
    }
    var token: String? = null

    /**
     * Si otra parte de la app envía por defecto una IP local vieja (10.0.2.2 o localhost),
     * redirige automáticamente a la URL de Railway.
     */
    private fun resolveBaseUrl(): String {
        val raw = baseUrl().trim().trimEnd('/')
        return if (raw.isBlank() || raw.contains("10.0.2.2") || raw.contains("localhost") || raw.contains("127.0.0.1")) {
            RAILWAY_BASE_URL
        } else if (!raw.startsWith("http://") && !raw.startsWith("https://")) {
            "https://$raw"
        } else {
            raw
        }
    }

    private suspend fun call(
        method: HttpMethod, path: String, auth: Boolean = true, block: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse {
        val activeBaseUrl = resolveBaseUrl()
        val url = activeBaseUrl + path
        val response = try {
            http.request(url) {
                this.method = method
                if (auth) token?.let { bearerAuth(it) }
                block()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw ApiFailure(
                0,
                "No se pudo conectar con el servidor ($activeBaseUrl). Comprueba tu conexión a internet o que el servidor en Railway esté activo. Detalle: ${e::class.simpleName}: ${e.message}"
            )
        }
        if (!response.status.isSuccess()) {
            val message = runCatching { response.body<ApiError>().message }.getOrNull()
                ?: "Error del servidor (${response.status.value})"
            throw ApiFailure(response.status.value, message)
        }
        return response
    }

    private inline fun <reified T> HttpRequestBuilder.jsonBody(value: T) {
        contentType(ContentType.Application.Json)
        setBody(value)
    }

    // ---------- A-01: Salud del servidor ----------
    suspend fun health(): Boolean =
        runCatching { call(HttpMethod.Get, "/health", auth = false).body<HealthResponse>().status == "ok" }.getOrDefault(false)

    // ---------- A-02, A-03, A-04: Autenticación ----------
    suspend fun login(username: String, password: String): LoginResponse =
        call(HttpMethod.Post, "/auth/login", auth = false) { jsonBody(LoginRequest(username, password)) }.body()

    suspend fun me(): UserDto = call(HttpMethod.Get, "/auth/me").body()

    suspend fun logout() { call(HttpMethod.Post, "/auth/logout") }

    suspend fun biometricEnroll(): BiometricEnrollResponse =
        call(HttpMethod.Post, "/auth/biometric/enroll").body()

    suspend fun biometricLogin(username: String, deviceToken: String, biometricType: String = "FINGERPRINT_OR_FACE"): LoginResponse =
        call(HttpMethod.Post, "/auth/biometric/login", auth = false) {
            jsonBody(BiometricLoginRequest(username, deviceToken, biometricType))
        }.body()

    suspend fun biometricRevoke() { call(HttpMethod.Delete, "/auth/biometric") }

    // ---------- Contenido ----------
    suspend fun worlds(): List<Mundo> = call(HttpMethod.Get, "/content/worlds").body()

    suspend fun check(worldId: String, levelId: Int, index: Int, option: String): Boolean =
        call(HttpMethod.Post, "/content/check") {
            jsonBody(CheckRequest(worldId, levelId, index, option))
        }.body<CheckResponse>().correct

    // ---------- A-05 a A-08: Jugador (PlayerRoutes.kt) ----------
    suspend fun playerLogin(username: String = "", displayName: String = ""): PlayerState =
        call(HttpMethod.Post, "/api/jugadores/login") {
            jsonBody(PlayerLoginRequest(username, displayName))
        }.body<PlayerResponse>().player

    suspend fun player(): PlayerState = playerLogin()

    suspend fun savePlayer(request: SavePlayerRequest): PlayerState =
        call(HttpMethod.Post, "/api/jugadores/guardar") { jsonBody(request) }.body<PlayerResponse>().player

    suspend fun rename(name: String): PlayerState =
        call(HttpMethod.Post, "/api/jugadores/renombrar") { jsonBody(RenameRequest(name)) }.body<PlayerResponse>().player

    suspend fun resetProgress(targetUsername: String? = null): PlayerState =
        call(HttpMethod.Post, "/api/jugadores/reset") {
            jsonBody(ResetPlayerRequest(targetUsername))
        }.body<PlayerResponse>().player

    // ---------- A-09 a A-12: Juego y Tienda (GameProgressRoutes.kt) ----------
    suspend fun answer(request: AnswerRequest): AnswerResponse =
        call(HttpMethod.Post, "/api/juego/registrar-respuesta") { jsonBody(request) }.body()

    suspend fun completeLevel(worldId: String, levelId: Int): PlayerState =
        call(HttpMethod.Post, "/api/juego/completar-nivel") {
            jsonBody(LevelCompleteRequest(worldId, levelId))
        }.body<PlayerResponse>().player

    suspend fun restoreLife(): PlayerState =
        call(HttpMethod.Post, "/api/juego/restaurar-vida").body<PlayerResponse>().player

    suspend fun buyTrophy(id: String): PlayerState =
        call(HttpMethod.Post, "/api/tienda/comprar-trofeo") {
            jsonBody(BuyTrophyRequest(id))
        }.body<PlayerResponse>().player

    // ---------- A-13 y A-14: Ranking general y de aula (RankingRoutes.kt) ----------
    suspend fun ranking(): List<RankingEntry> = call(HttpMethod.Get, "/api/ranking").body()

    suspend fun classroomRanking(worldId: String?): List<ClassroomEntry> =
        call(HttpMethod.Get, "/api/ranking/aula") {
            if (!worldId.isNullOrBlank()) parameter("world", worldId)
        }.body()

    suspend fun classroomProgress(): List<StudentProgress> = call(HttpMethod.Get, "/classroom/progress").body()

    suspend fun classroomLog(limit: Int = 50): List<AnswerLogEntry> =
        call(HttpMethod.Get, "/classroom/log") { parameter("limit", limit) }.body()

    suspend fun adminOverview(): AdminOverview = call(HttpMethod.Get, "/admin/overview").body()

    suspend fun revokeSession(id: String) { call(HttpMethod.Delete, "/admin/sessions/$id") }

    fun close() = http.close()
}