package com.algebraquest.backend

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Rutas de ranking general y de aula del inventario (Tabla 2: A-13 y A-14) y alias equivalentes.
 * - A-13: GET /api/ranking
 * - A-14: GET /api/ranking/aula
 */
fun Route.rankingRoutes(store: GameStore) {
    // A-13: Lista el ranking general de jugadores ordenado por XP, nivel y trofeos obtenidos
    get("/api/ranking") {
        call.requireAuth(store) ?: return@get
        call.respond(store.ranking())
    }

    // A-14: Lista el desempeño y avance de los alumnos filtrado por mundo para evaluación docente
    get("/api/ranking/aula") {
        call.requireAuth(store, ROLE_SUPERVISOR, ROLE_ADMIN) ?: return@get
        val world = call.request.queryParameters["world"] ?: call.request.queryParameters["mundo"]
        call.respond(store.classroom(world))
    }

    // Alias compatibles y vistas complementarias para Supervisor y Administrador
    get("/ranking") {
        call.requireAuth(store) ?: return@get
        call.respond(store.ranking())
    }
    get("/classroom/ranking") {
        call.requireAuth(store, ROLE_SUPERVISOR, ROLE_ADMIN) ?: return@get
        val world = call.request.queryParameters["world"] ?: call.request.queryParameters["mundo"]
        call.respond(store.classroom(world))
    }
    get("/classroom/progress") {
        call.requireAuth(store, ROLE_SUPERVISOR, ROLE_ADMIN) ?: return@get
        call.respond(store.progress())
    }
    get("/classroom/log") {
        call.requireAuth(store, ROLE_SUPERVISOR, ROLE_ADMIN) ?: return@get
        call.respond(store.answerLog((call.request.queryParameters["limit"]?.toIntOrNull() ?: 50).coerceIn(1, 200)))
    }
}
