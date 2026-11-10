package com.example.algrebraquest.domain

import com.example.algrebraquest.data.Mundo
import com.example.algrebraquest.data.Nivel
import com.example.algrebraquest.data.PlayerState

/** Reglas del catálogo de mundos (equivalente a Player.mundoDesbloqueado, etc. del juego web). */
object WorldsCatalog {
    const val LIFE_COST_XP = 25

    fun world(worlds: List<Mundo>, id: String) = worlds.firstOrNull { it.id == id }
    fun level(worlds: List<Mundo>, worldId: String, levelId: Int): Pair<Mundo, Nivel>? {
        val w = world(worlds, worldId) ?: return null
        val l = w.niveles.firstOrNull { it.id == levelId } ?: return null
        return w to l
    }

    fun levelDone(p: PlayerState, worldId: String, levelId: Int) = "${worldId}_$levelId" in p.completados
    fun completedCount(p: PlayerState, w: Mundo) = w.niveles.count { levelDone(p, w.id, it.id) }
    fun worldDone(p: PlayerState, w: Mundo) = w.niveles.isNotEmpty() && completedCount(p, w) == w.niveles.size

    fun worldUnlocked(p: PlayerState, w: Mundo, worlds: List<Mundo>): Boolean {
        if (w.desbloqueado) return true
        val required = world(worlds, w.requiere ?: return false) ?: return false
        return worldDone(p, required)
    }

    fun levelUnlocked(p: PlayerState, w: Mundo, l: Nivel) = l.id == 1 || levelDone(p, w.id, l.id - 1)

    /** Siguiente nivel pendiente de un mundo desbloqueado (o null si todo está completado/bloqueado). */
    fun nextMission(p: PlayerState, worlds: List<Mundo>): Pair<Mundo, Nivel>? {
        for (w in worlds) {
            if (!worldUnlocked(p, w, worlds)) continue
            val l = w.niveles.firstOrNull { levelUnlocked(p, w, it) && !levelDone(p, w.id, it.id) }
            if (l != null) return w to l
        }
        return worlds.firstOrNull { worldUnlocked(p, it, worlds) }?.let { w -> w.niveles.firstOrNull()?.let { w to it } }
    }

    fun totalLevels(worlds: List<Mundo>) = worlds.sumOf { it.niveles.size }
}

data class Trophy(val id: String, val icono: String, val nombre: String, val descripcion: String, val precio: Int)

val TROPHIES = listOf(
    Trophy("bronce", "🥉", "Trofeo de bronce", "La primera pieza de tu colección.", 50),
    Trophy("plata", "🥈", "Trofeo de plata", "Una medalla para una mente constante.", 100),
    Trophy("oro", "🥇", "Trofeo de oro", "Un premio para quienes dominan los retos.", 180),
    Trophy("campeon", "🏆", "Copa de campeón", "El gran premio de Álgebra Quest.", 300),
)

data class Achievement(val icono: String, val titulo: String, val descripcion: String, val logrado: Boolean)

fun achievementsFor(p: PlayerState, worlds: List<Mundo>): List<Achievement> {
    val done = p.completados.size
    val total = WorldsCatalog.totalLevels(worlds)
    val factorizacion = worlds.firstOrNull { it.id == "factorizacion" }
    return listOf(
        Achievement("🥇", "Primer Paso", "Completa tu primer nivel", done >= 1),
        Achievement("🔥", "Racha de 3", "Completa 3 niveles", done >= 3),
        Achievement("🌱", "Maestro de la Factorización", "Completa todo el mundo Factorización",
            factorizacion != null && WorldsCatalog.worldDone(p, factorizacion)),
        Achievement("💰", "Coleccionista", "Junta 100 monedas", p.monedas >= 100),
        Achievement("⭐", "Nivel de Jugador 3", "Alcanza el nivel 3 de jugador", p.nivel >= 3),
        Achievement("🏆", "Completista", "Completa todos los niveles disponibles", total > 0 && done >= total),
    )
}
