package com.algebraquest.backend

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Rutas de jugadores del inventario (Tabla 2: A-05 a A-08) y rutas equivalentes (/player/*).
 * - A-05: POST /api/jugadores/login
 * - A-06: POST /api/jugadores/guardar
 * - A-07: POST /api/jugadores/renombrar
 * - A-08: POST /api/jugadores/reset
 */
fun Route.playerRoutes(store: GameStore) {
    // A-05: Registra un jugador nuevo o recupera su perfil con vidas, XP, nivel y monedas
    post("/api/jugadores/login") {
        val s = call.requireAuth(store, ROLE_ALUMNO, ROLE_ADMIN) ?: return@post
        val body = runCatching { call.receive<PlayerLoginRequest>() }.getOrNull()
        val targetUser = body?.username?.takeIf { it.isNotBlank() } ?: s.user.username
        val targetDisplay = body?.displayName?.takeIf { it.isNotBlank() } ?: s.user.displayName
        call.respond(PlayerResponse(store.loginOrCreatePlayer(targetUser, targetDisplay)))
    }

    // A-06: Sincroniza y guarda el progreso general del jugador (XP, nivel, vidas y monedas)
    post("/api/jugadores/guardar") {
        val s = call.requireAuth(store, ROLE_ALUMNO) ?: return@post
        val req = call.receive<SavePlayerRequest>()
        call.respond(PlayerResponse(store.savePlayer(s.user.username, req)))
    }

    // A-07: Actualiza el nombre visible del jugador en su perfil y en el ranking
    post("/api/jugadores/renombrar") {
        val s = call.requireAuth(store, ROLE_ALUMNO) ?: return@post
        val req = call.receive<RenameRequest>()
        call.respond(PlayerResponse(store.rename(s.user.username, req.displayName)))
    }

    // A-08: Reinicia a sus valores iniciales el progreso, vidas, monedas y trofeos del jugador (Ajustes y Administrador)
    post("/api/jugadores/reset") {
        val s = call.requireAuth(store, ROLE_ALUMNO, ROLE_ADMIN) ?: return@post
        val body = runCatching { call.receive<ResetPlayerRequest>() }.getOrNull()
        val target = if (s.user.role == ROLE_ADMIN && !body?.username.isNullOrBlank()) {
            body!!.username!!.trim()
        } else {
            s.user.username
        }
        call.respond(PlayerResponse(store.reset(target)))
    }

    // Alias compatibles con cliente KMP existente
    get("/player") {
        val s = call.requireAuth(store, ROLE_ALUMNO) ?: return@get
        call.respond(PlayerResponse(store.player(s.user.username)))
    }
    post("/player/rename") {
        val s = call.requireAuth(store, ROLE_ALUMNO) ?: return@post
        call.respond(PlayerResponse(store.rename(s.user.username, call.receive<RenameRequest>().displayName)))
    }
    post("/player/reset") {
        val s = call.requireAuth(store, ROLE_ALUMNO, ROLE_ADMIN) ?: return@post
        val body = runCatching { call.receive<ResetPlayerRequest>() }.getOrNull()
        val target = if (s.user.role == ROLE_ADMIN && !body?.username.isNullOrBlank()) {
            body!!.username!!.trim()
        } else {
            s.user.username
        }
        call.respond(PlayerResponse(store.reset(target)))
    }
}
