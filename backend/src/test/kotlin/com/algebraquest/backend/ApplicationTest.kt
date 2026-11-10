package com.algebraquest.backend

import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlin.test.*

class ApplicationTest {
    private fun ApplicationTestBuilder.api() = createClient { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }

    private suspend fun io.ktor.client.HttpClient.login(u: String, p: String): LoginResponse =
        post("/auth/login") { contentType(ContentType.Application.Json); setBody(LoginRequest(u, p)) }.body()

    @Test fun rolesAndBadCredentials() = testApplication {
        application { module(null) }
        val c = api()
        assertEquals("ADMINISTRADOR", c.login("admin", "Admin123!").user.role)
        assertEquals("SUPERVISOR", c.login("supervisor", "Supervisor123!").user.role)
        assertEquals("ALUMNO", c.login("alumno", "Alumno123!").user.role)
        val bad = c.post("/auth/login") { contentType(ContentType.Application.Json); setBody(LoginRequest("alumno", "x")) }
        assertEquals(HttpStatusCode.Unauthorized, bad.status)
    }

    @Test fun rolesAreEnforced() = testApplication {
        application { module(null) }
        val c = api()
        val alumno = c.login("alumno", "Alumno123!")
        assertEquals(HttpStatusCode.Forbidden, c.get("/admin/overview") { bearerAuth(alumno.token) }.status)
        assertEquals(HttpStatusCode.Unauthorized, c.get("/player").status)
        val admin = c.login("admin", "Admin123!")
        assertEquals(HttpStatusCode.OK, c.get("/admin/overview") { bearerAuth(admin.token) }.status)
    }

    @Test fun inventoryApiEndpointsFlow() = testApplication {
        application { module(null) }
        val c = api()
        val t = c.login("alumno", "Alumno123!").token

        // A-05: /api/jugadores/login
        val playerLogin: PlayerResponse = c.post("/api/jugadores/login") {
            bearerAuth(t)
            contentType(ContentType.Application.Json)
            setBody(PlayerLoginRequest("alumno", "Estudiante"))
        }.body()
        assertEquals("alumno", playerLogin.player.username)

        // A-10: /api/juego/registrar-respuesta
        val ans: AnswerResponse = c.post("/api/juego/registrar-respuesta") {
            bearerAuth(t)
            contentType(ContentType.Application.Json)
            setBody(AnswerRequest("factorizacion", 1, 0, "10(2x+3)", 2500))
        }.body()
        assertTrue(ans.correct)

        // A-09: /api/juego/completar-nivel
        val completed: PlayerResponse = c.post("/api/juego/completar-nivel") {
            bearerAuth(t)
            contentType(ContentType.Application.Json)
            setBody(LevelCompleteRequest("factorizacion", 1))
        }.body()
        assertTrue("factorizacion_1" in completed.player.completados)

        // A-06: /api/jugadores/guardar
        val saved: PlayerResponse = c.post("/api/jugadores/guardar") {
            bearerAuth(t)
            contentType(ContentType.Application.Json)
            setBody(SavePlayerRequest(monedas = 120, xp = 50))
        }.body()
        assertEquals(120, saved.player.monedas)

        // A-12: /api/tienda/comprar-trofeo
        val bought: PlayerResponse = c.post("/api/tienda/comprar-trofeo") {
            bearerAuth(t)
            contentType(ContentType.Application.Json)
            setBody(BuyTrophyRequest("bronce"))
        }.body()
        assertTrue("bronce" in bought.player.trofeos)

        // A-13: /api/ranking
        val ranking: List<RankingEntry> = c.get("/api/ranking") { bearerAuth(t) }.body()
        assertTrue(ranking.isNotEmpty())

        // A-14: /api/ranking/aula con rol SUPERVISOR
        val supToken = c.login("supervisor", "Supervisor123!").token
        val aula: List<ClassroomEntry> = c.get("/api/ranking/aula?world=factorizacion") { bearerAuth(supToken) }.body()
        assertTrue(aula.isNotEmpty())
    }

    @Test fun biometricEnrollLoginAndRevokeAllRoles() = testApplication {
        application { module(null) }
        val c = api()
        for ((user, pass, role) in listOf(
            Triple("admin", "Admin123!", "ADMINISTRADOR"),
            Triple("supervisor", "Supervisor123!", "SUPERVISOR"),
            Triple("ana", "Ana12345!", "ALUMNO")
        )) {
            val t = c.login(user, pass).token
            val device: BiometricEnrollResponse = c.post("/auth/biometric/enroll") { bearerAuth(t) }.body()
            val ok: LoginResponse = c.post("/auth/biometric/login") {
                contentType(ContentType.Application.Json)
                setBody(BiometricLoginRequest(user, device.deviceToken))
            }.body()
            assertEquals(role, ok.user.role)
            c.delete("/auth/biometric") { bearerAuth(t) }
            val revoked = c.post("/auth/biometric/login") {
                contentType(ContentType.Application.Json)
                setBody(BiometricLoginRequest(user, device.deviceToken))
            }
            assertEquals(HttpStatusCode.Unauthorized, revoked.status)
        }
    }

    @Test fun logoutInvalidatesToken() = testApplication {
        application { module(null) }
        val c = api()
        val t = c.login("alumno", "Alumno123!").token
        c.post("/auth/logout") { bearerAuth(t) }
        assertEquals(HttpStatusCode.Unauthorized, c.get("/auth/me") { bearerAuth(t) }.status)
    }
}
