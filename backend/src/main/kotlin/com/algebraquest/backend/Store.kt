package com.algebraquest.backend

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ApiException(val status: HttpStatusCode, message: String) : Exception(message)

data class Session(val id: String, val user: UserDto, val expiresAt: Long, val biometric: Boolean)

fun sha256(text: String): String =
    MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }

@Serializable
private data class Persisted(
    val players: Map<String, PlayerState> = emptyMap(),
    val log: List<AnswerLogEntry> = emptyList(),
    val devices: Map<String, List<String>> = emptyMap(),
)

/** Estado del juego: jugadores, bitácora de respuestas, dispositivos biométricos y sesiones. */
class GameStore(val users: List<StoredUser>, val worlds: List<Mundo>, private val file: File? = null) {
    companion object {
        const val SESSION_MS = 8L * 60 * 60 * 1000
        const val LIFE_COST_XP = 25
        val TROPHY_PRICES = mapOf("bronce" to 50, "plata" to 100, "oro" to 180, "campeon" to 300)
        private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }
    }

    private val lock = Any()
    private val players = LinkedHashMap<String, PlayerState>()
    private val log = ArrayList<AnswerLogEntry>()
    private val devices = HashMap<String, MutableSet<String>>() // username -> hashes de device tokens
    private val streaks = HashMap<String, Int>()
    private val sessions = ConcurrentHashMap<String, Session>()
    private val random = SecureRandom()

    init {
        file?.takeIf { it.exists() }?.let {
            runCatching { json.decodeFromString<Persisted>(it.readText()) }.getOrNull()?.let { p ->
                players.putAll(p.players); log.addAll(p.log)
                p.devices.forEach { (u, h) -> devices[u] = h.toMutableSet() }
            }
        }
    }

    private fun save() {
        val f = file ?: return
        f.parentFile?.mkdirs()
        f.writeText(
            json.encodeToString(
                Persisted.serializer(),
                Persisted(players.toMap(), log.toList(), devices.mapValues { it.value.toList() })
            )
        )
    }

    // ---------- Sesiones ----------
    fun findUser(username: String, password: String): StoredUser? =
        users.firstOrNull {
            it.username.equals(username.trim(), ignoreCase = true) &&
                MessageDigest.isEqual(it.password.toByteArray(), password.toByteArray())
        }

    fun openSession(user: StoredUser, biometric: Boolean): LoginResponse {
        val now = System.currentTimeMillis()
        sessions.entries.removeIf { it.value.expiresAt <= now }
        val token = UUID.randomUUID().toString()
        val dto = UserDto(user.username, user.displayName, user.role)
        sessions[token] = Session(sha256(token).take(12), dto, now + SESSION_MS, biometric)
        return LoginResponse(token, dto)
    }

    fun session(token: String?): Session? =
        token?.let(sessions::get)?.takeIf { it.expiresAt > System.currentTimeMillis() }

    fun closeSession(token: String) { sessions.remove(token) }

    fun activeSessions(): List<SessionInfo> {
        val now = System.currentTimeMillis()
        return sessions.values.filter { it.expiresAt > now }.map {
            SessionInfo(it.id, it.user.username, it.user.role, it.expiresAt, it.biometric)
        }.sortedBy { it.username }
    }

    fun revokeSession(id: String): Boolean = sessions.entries.removeIf { it.value.id == id }

    // ---------- Biometría (token de dispositivo; solo se guarda su hash) ----------
    fun enrollDevice(username: String): String = synchronized(lock) {
        val canonical = users.firstOrNull { it.username.equals(username, ignoreCase = true) }?.username ?: username
        val bytes = ByteArray(32).also(random::nextBytes)
        val token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        devices.getOrPut(canonical) { mutableSetOf() }.add(sha256(token))
        save()
        token
    }

    fun verifyDevice(username: String, token: String): StoredUser? = synchronized(lock) {
        val user = users.firstOrNull { it.username.equals(username.trim(), ignoreCase = true) } ?: return null
        val hash = sha256(token)
        val ok = devices[user.username]?.any { MessageDigest.isEqual(it.toByteArray(), hash.toByteArray()) } == true
        if (ok) user else null
    }

    fun revokeDevices(username: String) = synchronized(lock) {
        val canonical = users.firstOrNull { it.username.equals(username, ignoreCase = true) }?.username ?: username
        devices.remove(canonical)
        save()
    }

    // ---------- Contenido ----------
    private fun locate(worldId: String, levelId: Int): Pair<Mundo, Nivel> {
        val w = worlds.firstOrNull { it.id == worldId } ?: throw ApiException(HttpStatusCode.NotFound, "Mundo inexistente")
        val l = w.niveles.firstOrNull { it.id == levelId } ?: throw ApiException(HttpStatusCode.NotFound, "Nivel inexistente")
        return w to l
    }

    private fun levelDone(p: PlayerState, w: String, l: Int) = "${w}_$l" in p.completados
    private fun worldDone(p: PlayerState, w: Mundo) = w.niveles.isNotEmpty() && w.niveles.all { levelDone(p, w.id, it.id) }
    private fun worldUnlocked(p: PlayerState, w: Mundo): Boolean {
        if (w.desbloqueado) return true
        val req = worlds.firstOrNull { it.id == w.requiere } ?: return false
        return worldDone(p, req)
    }

    private fun assertUnlocked(p: PlayerState, w: Mundo, l: Nivel) {
        if (!worldUnlocked(p, w)) throw ApiException(HttpStatusCode.Forbidden, "Mundo bloqueado")
        if (l.id != 1 && !levelDone(p, w.id, l.id - 1)) throw ApiException(HttpStatusCode.Forbidden, "Nivel bloqueado")
    }

    fun check(worldId: String, levelId: Int, index: Int, option: String): Boolean {
        val (_, l) = locate(worldId, levelId)
        val q = l.preguntas.getOrNull(index) ?: throw ApiException(HttpStatusCode.BadRequest, "Pregunta inválida")
        return option == q.opciones[q.correcta]
    }

    // ---------- Jugador ----------
    private fun ensure(username: String): PlayerState {
        val u = users.firstOrNull { it.username.equals(username, ignoreCase = true) && it.role == "ALUMNO" }
            ?: throw ApiException(HttpStatusCode.Forbidden, "Solo los alumnos tienen perfil de juego")
        return players.getOrPut(u.username) { PlayerState(u.username, u.displayName) }
    }

    fun player(username: String): PlayerState = synchronized(lock) { ensure(username) }

    /** A-05: Registra un jugador nuevo o recupera su perfil activo. */
    fun loginOrCreatePlayer(username: String, displayName: String = ""): PlayerState = synchronized(lock) {
        val cleanUser = username.trim()
        val stored = users.firstOrNull { it.username.equals(cleanUser, ignoreCase = true) }
        if (stored != null && stored.role != "ALUMNO") {
            throw ApiException(HttpStatusCode.Forbidden, "Solo las cuentas con rol ALUMNO tienen perfil de jugador")
        }
        val key = stored?.username ?: cleanUser.ifEmpty { "alumno" }
        val defaultName = displayName.trim().ifEmpty { stored?.displayName ?: key }
        players.getOrPut(key) { PlayerState(key, defaultName) }.also { save() }
    }

    /** A-06: Sincroniza y guarda el progreso general del jugador (XP, nivel, vidas, monedas, trofeos y niveles completados). */
    fun savePlayer(username: String, req: SavePlayerRequest): PlayerState = synchronized(lock) {
        val current = ensure(username)
        val updated = current.copy(
            xp = req.xp?.coerceAtLeast(0) ?: current.xp,
            xpMax = req.xpMax?.coerceAtLeast(10) ?: current.xpMax,
            nivel = req.nivel?.coerceAtLeast(1) ?: current.nivel,
            vidas = req.vidas?.coerceIn(0, current.vidasMax) ?: current.vidas,
            monedas = req.monedas?.coerceAtLeast(0) ?: current.monedas,
            trofeos = req.trofeos ?: current.trofeos,
            completados = req.completados ?: current.completados,
        )
        players[current.username] = updated
        save()
        updated
    }

    fun answer(username: String, r: AnswerRequest): AnswerResponse = synchronized(lock) {
        val p = ensure(username)
        val (w, l) = locate(r.worldId, r.levelId)
        assertUnlocked(p, w, l)
        if (p.vidas <= 0) throw ApiException(HttpStatusCode.Conflict, "Sin vidas. Recupera una con 25 XP.")
        val q = l.preguntas.getOrNull(r.questionIndex) ?: throw ApiException(HttpStatusCode.BadRequest, "Pregunta inválida")
        val correct = r.option == q.opciones[q.correcta]
        var updated = p
        var points = 0
        var leveled = false
        if (correct) {
            val streak = (streaks[p.username] ?: 0) + 1
            streaks[p.username] = streak
            points = 1000 + maxOf(0, 500 - (r.durationMs / 1000).toInt() * 25) + minOf((streak - 1) * 100, 300)
            var xp = p.xp + q.xp
            var nivel = p.nivel
            var xpMax = p.xpMax
            while (xp >= xpMax) { xp -= xpMax; nivel++; xpMax = Math.round(xpMax * 1.15).toInt(); leveled = true }
            updated = p.copy(xp = xp, nivel = nivel, xpMax = xpMax, monedas = p.monedas + q.monedas)
        } else {
            streaks[p.username] = 0
            updated = p.copy(vidas = maxOf(0, p.vidas - 1))
        }
        players[p.username] = updated
        log.add(AnswerLogEntry(System.currentTimeMillis(), updated.displayName, w.id, l.id, q.enunciado, correct, points, r.durationMs))
        if (log.size > 2000) log.removeAt(0)
        save()
        AnswerResponse(
            correct, if (correct) q.xp else 0, if (correct) q.monedas else 0, points,
            leveled, updated.vidas <= 0, streaks[p.username] ?: 0, updated
        )
    }

    fun completeLevel(username: String, worldId: String, levelId: Int): PlayerState = synchronized(lock) {
        val p = ensure(username)
        val (w, l) = locate(worldId, levelId)
        assertUnlocked(p, w, l)
        val key = "${w.id}_${l.id}"
        if (key !in p.completados) players[p.username] = p.copy(completados = p.completados + key).also { save() }
        players.getValue(p.username)
    }

    fun restoreLife(username: String): PlayerState = synchronized(lock) {
        val p = ensure(username)
        if (p.vidas >= p.vidasMax || p.xp < LIFE_COST_XP)
            throw ApiException(HttpStatusCode.Conflict, "No tienes suficiente XP o ya tienes todas las vidas.")
        players[p.username] = p.copy(xp = p.xp - LIFE_COST_XP, vidas = p.vidas + 1)
        save()
        players.getValue(p.username)
    }

    fun buyTrophy(username: String, id: String): PlayerState = synchronized(lock) {
        val p = ensure(username)
        val price = TROPHY_PRICES[id] ?: throw ApiException(HttpStatusCode.NotFound, "Trofeo inexistente")
        if (id in p.trofeos) throw ApiException(HttpStatusCode.Conflict, "Ya tienes este trofeo")
        if (p.monedas < price) throw ApiException(HttpStatusCode.Conflict, "No tienes suficientes monedas")
        players[p.username] = p.copy(monedas = p.monedas - price, trofeos = p.trofeos + id)
        save()
        players.getValue(p.username)
    }

    fun rename(username: String, name: String): PlayerState = synchronized(lock) {
        val p = ensure(username)
        val clean = name.trim()
        if (clean.isEmpty() || clean.length > 20) throw ApiException(HttpStatusCode.BadRequest, "El nombre debe tener entre 1 y 20 caracteres")
        val oldName = p.displayName
        players[p.username] = p.copy(displayName = clean)
        for (i in log.indices) {
            if (log[i].nombre == oldName) {
                log[i] = log[i].copy(nombre = clean)
            }
        }
        save()
        players.getValue(p.username)
    }

    fun reset(username: String): PlayerState = synchronized(lock) {
        val p = ensure(username)
        val defaultName = users.firstOrNull { it.username == p.username }?.displayName ?: p.displayName
        players[p.username] = PlayerState(p.username, defaultName)
        streaks.remove(p.username)
        log.removeAll { it.nombre == p.displayName || it.nombre == defaultName }
        save()
        players.getValue(p.username)
    }

    // ---------- Consultas de ranking / aula / admin ----------
    private fun students() = users.filter { it.role == "ALUMNO" }.map { ensure(it.username) }

    fun ranking(): List<RankingEntry> = synchronized(lock) {
        students().sortedWith(
            compareByDescending<PlayerState> { it.nivel }
                .thenByDescending { it.xp }
                .thenByDescending { it.trofeos.size }
                .thenByDescending { it.monedas }
        ).map { RankingEntry(it.displayName, it.username, it.nivel, it.xp, it.monedas, it.trofeos.size) }
    }

    fun classroom(worldId: String?): List<ClassroomEntry> = synchronized(lock) {
        log.filter { worldId.isNullOrBlank() || it.mundoId == worldId }.groupBy { it.nombre }.map { (name, rows) ->
            val ok = rows.filter { it.correcta }
            ClassroomEntry(
                name, rows.sumOf { it.puntos }, ok.size, rows.size,
                Math.round(1000.0 * ok.size / rows.size) / 10.0,
                if (ok.isEmpty()) 0 else ok.sumOf { it.duracionMs } / ok.size
            )
        }.sortedWith(
            compareByDescending<ClassroomEntry> { it.puntos }
                .thenByDescending { it.correctas }
                .thenByDescending { it.precision }
                .thenBy { it.tiempoPromedio }
                .thenBy { it.nombre }
        ).take(50)
    }

    fun progress(): List<StudentProgress> = synchronized(lock) {
        val total = worlds.sumOf { it.niveles.size }
        students().map { p ->
            StudentProgress(
                p.displayName, p.username, p.nivel, p.xp, p.completados.size, total,
                worlds.associate { w -> w.id to w.niveles.count { levelDone(p, w.id, it.id) } }
            )
        }.sortedByDescending { it.nivelesCompletados }
    }

    fun answerLog(limit: Int): List<AnswerLogEntry> = synchronized(lock) { log.takeLast(limit).reversed() }

    fun overview(): AdminOverview = synchronized(lock) {
        val ps = students()
        val sessionsNow = activeSessions()
        AdminOverview(
            users.count { it.role == "ADMINISTRADOR" },
            users.count { it.role == "SUPERVISOR" },
            users.count { it.role == "ALUMNO" },
            sessionsNow.size,
            worlds.map { w ->
                WorldStatus(
                    w.id, w.nombre, w.icono, w.niveles.size, w.niveles.sumOf { it.preguntas.size },
                    log.filter { it.mundoId == w.id }.map { it.nombre }.distinct().size,
                    ps.count { worldDone(it, w) }
                )
            },
            sessionsNow,
        )
    }
}
