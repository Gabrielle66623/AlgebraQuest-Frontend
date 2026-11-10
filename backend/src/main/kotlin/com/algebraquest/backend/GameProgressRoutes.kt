package com.algebraquest.backend

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Rutas de progreso de juego y tienda del inventario (Tabla 2: A-09 a A-12) y alias equivalentes.
 * - A-09: POST /api/juego/completar-nivel
 * - A-10: POST /api/juego/registrar-respuesta
 * - A-11: POST /api/juego/restaurar-vida
 * - A-12: POST /api/tienda/comprar-trofeo
 */
fun Route.gameProgressRoutes(store: GameStore) {
    // A-09: Registra un nivel superado en los 4 mundos, desbloquea el siguiente y suma XP/monedas
    post("/api/juego/completar-nivel") {
        val s = call.requireAuth(store, ROLE_ALUMNO) ?: return@post
        val r = call.receive<LevelCompleteRequest>()
        call.respond(PlayerResponse(store.completeLevel(s.user.username, r.worldId, r.levelId)))
    }

    // A-10: Guarda en bitácora cada respuesta del alumno (reactivo, acierto o error)
    post("/api/juego/registrar-respuesta") {
        val s = call.requireAuth(store, ROLE_ALUMNO) ?: return@post
        call.respond(store.answer(s.user.username, call.receive()))
    }

    // A-11: Recarga vidas del jugador y actualiza su estado en la barra superior (HUD)
    post("/api/juego/restaurar-vida") {
        val s = call.requireAuth(store, ROLE_ALUMNO) ?: return@post
        call.respond(PlayerResponse(store.restoreLife(s.user.username)))
    }

    // A-12: Descuenta monedas del jugador y registra la compra de uno de los 4 trofeos
    post("/api/tienda/comprar-trofeo") {
        val s = call.requireAuth(store, ROLE_ALUMNO) ?: return@post
        val req = call.receive<BuyTrophyRequest>()
        call.respond(PlayerResponse(store.buyTrophy(s.user.username, req.trophyId)))
    }

    // Alias compatibles con cliente KMP existente
    post("/player/answer") {
        val s = call.requireAuth(store, ROLE_ALUMNO) ?: return@post
        call.respond(store.answer(s.user.username, call.receive()))
    }
    post("/player/level-complete") {
        val s = call.requireAuth(store, ROLE_ALUMNO) ?: return@post
        val r = call.receive<LevelCompleteRequest>()
        call.respond(PlayerResponse(store.completeLevel(s.user.username, r.worldId, r.levelId)))
    }
    post("/player/restore-life") {
        val s = call.requireAuth(store, ROLE_ALUMNO) ?: return@post
        call.respond(PlayerResponse(store.restoreLife(s.user.username)))
    }
    post("/store/trophies/{id}/buy") {
        val s = call.requireAuth(store, ROLE_ALUMNO) ?: return@post
        call.respond(PlayerResponse(store.buyTrophy(s.user.username, call.parameters["id"].orEmpty())))
    }
}
