package com.algebraquest.backend

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.io.File

const val ROLE_ADMIN = "ADMINISTRADOR"
const val ROLE_SUPERVISOR = "SUPERVISOR"
const val ROLE_ALUMNO = "ALUMNO"

fun main() {
    embeddedServer(
        Netty,
        port = System.getenv("PORT")?.toIntOrNull() ?: 8080,
        host = System.getenv("HOST") ?: "0.0.0.0",
    ) { module() }.start(wait = true)
}

internal fun ApplicationCall.bearer(): String? =
    request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ")?.trim()?.takeIf { it.isNotEmpty() }

/** Valida el token y el rol; si falla ya respondió 401/403 y devuelve null. */
internal suspend fun ApplicationCall.requireAuth(store: GameStore, vararg roles: String): Session? {
    val s = store.session(bearer())
    if (s == null) {
        respond(HttpStatusCode.Unauthorized, ApiError("Sesión inválida o expirada"))
        return null
    }
    if (roles.isNotEmpty() && s.user.role !in roles) {
        respond(HttpStatusCode.Forbidden, ApiError("Tu rol no tiene acceso a este recurso"))
        return null
    }
    return s
}

fun Application.module(dataFile: File? = File(System.getenv("DATA_FILE") ?: "data/store.json")) {
    val json = Json { ignoreUnknownKeys = true }
    fun resource(name: String) = checkNotNull(javaClass.classLoader.getResourceAsStream(name)) { "No se encontró $name" }
        .bufferedReader(Charsets.UTF_8).use { it.readText() }

    val users = json.decodeFromString<List<StoredUser>>(resource("users.json"))
    require(users.map { it.username }.distinct().size == users.size) { "Usuarios duplicados" }
    require(users.all { it.role in setOf(ROLE_ADMIN, ROLE_SUPERVISOR, ROLE_ALUMNO) }) { "Rol inválido" }
    val worlds = json.decodeFromString<List<Mundo>>(resource("worlds.json"))
    val store = GameStore(users, worlds, dataFile)

    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; encodeDefaults = true }) }
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
    }
    install(StatusPages) {
        exception<ApiException> { call, e -> call.respond(e.status, ApiError(e.message ?: "Error")) }
        exception<io.ktor.server.plugins.BadRequestException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ApiError("Solicitud JSON inválida"))
        }
    }

    routing {
        // A-01: Verifica que el servidor Ktor (Netty en el puerto 8080) esté activo
        get("/health") { call.respond(mapOf("status" to "ok", "service" to "AlgebraQuest Backend")) }

        // ---------------- Autenticación (A-02, A-03, A-04 + Biometría por rol) ----------------
        post("/auth/login") {
            val r = call.receive<LoginRequest>()
            val match = store.findUser(r.username, r.password)
            if (match == null) call.respond(HttpStatusCode.Unauthorized, ApiError("Usuario o contraseña incorrectos"))
            else call.respond(store.openSession(match, biometric = false))
        }
        get("/auth/me") { call.requireAuth(store)?.let { call.respond(it.user) } }
        post("/auth/logout") {
            call.bearer()?.let(store::closeSession)
            call.respond(HttpStatusCode.NoContent)
        }

        // Biometría (huella dactilar y reconocimiento facial) para ADMINISTRADOR, SUPERVISOR y ALUMNO
        post("/auth/biometric/enroll") {
            val s = call.requireAuth(store) ?: return@post
            val token = store.enrollDevice(s.user.username)
            call.respond(BiometricEnrollResponse(token, s.user.username, s.user.role))
        }
        post("/auth/biometric/login") {
            val r = call.receive<BiometricLoginRequest>()
            val user = store.verifyDevice(r.username.trim(), r.deviceToken)
            if (user == null) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiError("Biometría no registrada o revocada para ${r.username}. Inicia sesión con tu contraseña.")
                )
            } else {
                call.respond(store.openSession(user, biometric = true))
            }
        }
        delete("/auth/biometric") {
            val s = call.requireAuth(store) ?: return@delete
            store.revokeDevices(s.user.username)
            call.respond(HttpStatusCode.NoContent)
        }

        // ---------------- Contenido ----------------
        get("/content/worlds") {
            call.requireAuth(store) ?: return@get
            call.respond(worlds.map { it.toPublic() })
        }
        post("/content/check") {
            call.requireAuth(store) ?: return@post
            val r = call.receive<CheckRequest>()
            call.respond(CheckResponse(store.check(r.worldId, r.levelId, r.questionIndex, r.option)))
        }

        // ---------------- Rutas modulares del inventario (A-05 a A-14) ----------------
        playerRoutes(store)
        gameProgressRoutes(store)
        rankingRoutes(store)

        // ---------------- Panel de Administrador ----------------
        get("/admin/overview") {
            call.requireAuth(store, ROLE_ADMIN) ?: return@get
            call.respond(store.overview())
        }
        delete("/admin/sessions/{id}") {
            call.requireAuth(store, ROLE_ADMIN) ?: return@delete
            if (store.revokeSession(call.parameters["id"].orEmpty())) call.respond(HttpStatusCode.NoContent)
            else call.respond(HttpStatusCode.NotFound, ApiError("Sesión inexistente"))
        }
    }
}
