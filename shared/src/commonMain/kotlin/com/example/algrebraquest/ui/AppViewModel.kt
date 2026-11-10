package com.example.algrebraquest.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.algrebraquest.data.AnswerRequest
import com.example.algrebraquest.data.AnswerResponse
import com.example.algrebraquest.data.ApiClient
import com.example.algrebraquest.data.ApiFailure
import com.example.algrebraquest.data.AuthSession
import com.example.algrebraquest.data.LoginResponse
import com.example.algrebraquest.data.Mundo
import com.example.algrebraquest.data.PlayerState
import com.example.algrebraquest.data.Role
import com.example.algrebraquest.data.SavePlayerRequest
import com.example.algrebraquest.domain.Route
import com.example.algrebraquest.platform.BiometricCredential
import com.example.algrebraquest.platform.BiometricMode
import com.example.algrebraquest.platform.BiometricResult
import com.example.algrebraquest.platform.BiometricStatus
import com.example.algrebraquest.platform.PlatformServices
import com.example.algrebraquest.platform.platformServices
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val loading: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val session: AuthSession? = null,
    val player: PlayerState? = null,
    val worlds: List<Mundo> = emptyList(),
    val backStack: List<Route> = listOf(Route.Login),
    val biometricStatus: BiometricStatus = BiometricStatus.UNAVAILABLE,
    val hasFingerprint: Boolean = false,
    val hasFace: Boolean = false,
    /** Usuario predeterminado cuya huella/rostro está vinculada a este dispositivo. */
    val biometricUser: String? = null,
    /** Lista de cuentas vinculadas con biometría (soporta los 3 roles: ADMINISTRADOR, SUPERVISOR, ALUMNO). */
    val biometricCredentials: List<BiometricCredential> = emptyList(),
    /** Mostrar el diálogo para activar la biometría tras iniciar sesión con contraseña. */
    val offerBiometric: Boolean = false,
    val serverUrl: String = "",
    val serverHealthy: Boolean? = null,
) {
    val route: Route get() = backStack.last()

    fun isBiometricEnrolledFor(username: String): Boolean =
        biometricCredentials.any { it.username.equals(username, ignoreCase = true) }

    fun credentialForRole(role: Role): BiometricCredential? =
        biometricCredentials.firstOrNull { it.role.equals(role.name, ignoreCase = true) }
}

fun rootRouteFor(role: Role): Route = when (role) {
    Role.ADMINISTRADOR -> Route.Administrator
    Role.SUPERVISOR -> Route.Supervisor
    Role.ALUMNO -> Route.Home
}

class AppViewModel(private val services: PlatformServices = platformServices()) : ViewModel() {
    private var serverUrl: String = services.secure.serverUrl ?: services.defaultServerUrl
    private val api = ApiClient { serverUrl }

    private val _state = MutableStateFlow(UiState(serverUrl = serverUrl))
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        refreshBiometric()
        checkServerHealth()
    }

    // ---------------- Mensajes / servidor (A-01) ----------------
    fun clearMessages() = _state.update { it.copy(error = null, notice = null) }
    private fun fail(message: String?) = _state.update { it.copy(loading = false, error = message ?: "Ocurrió un error") }

    fun checkServerHealth() {
        viewModelScope.launch {
            val ok = api.health()
            _state.update { it.copy(serverHealthy = ok) }
        }
    }

    fun setServerUrl(url: String) {
        val clean = url.trim().trimEnd('/')
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            fail("La dirección del servidor debe empezar con http:// o https://")
            return
        }
        serverUrl = clean
        services.secure.serverUrl = clean
        _state.update { it.copy(serverUrl = clean, error = null, notice = "Servidor actualizado a $clean") }
        checkServerHealth()
    }

    // ---------------- Inicio de sesión y Biometría (A-02, A-03, A-04, A-05) ----------------
    fun refreshBiometric() = _state.update {
        val creds = services.secure.loadAllBiometricCredentials()
        it.copy(
            biometricStatus = services.biometric.status(),
            hasFingerprint = services.biometric.hasFingerprint(),
            hasFace = services.biometric.hasFace(),
            biometricUser = creds.firstOrNull()?.username,
            biometricCredentials = creds,
        )
    }

    fun login(username: String, password: String) {
        if (_state.value.loading) return
        if (username.isBlank() || password.isBlank()) {
            fail("Completa el usuario y la contraseña")
            return
        }
        _state.update { it.copy(loading = true, error = null, notice = null) }
        viewModelScope.launch {
            try {
                startSession(api.login(username.trim(), password), viaBiometric = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                api.token = null
                fail(e.message)
            }
        }
    }

    fun biometricLogin(
        mode: BiometricMode = BiometricMode.AMBOS,
        targetUsername: String? = null,
        targetRole: Role? = null,
    ) {
        if (_state.value.loading) return
        val allCreds = services.secure.loadAllBiometricCredentials()
        val credential = when {
            !targetUsername.isNullOrBlank() -> allCreds.firstOrNull { it.username.equals(targetUsername.trim(), ignoreCase = true) }
            targetRole != null -> allCreds.firstOrNull { it.role.equals(targetRole.name, ignoreCase = true) }
            else -> allCreds.firstOrNull()
        }
        if (credential == null) {
            val roleLabel = targetRole?.title ?: targetUsername ?: "tu cuenta"
            fail("Aún no has vinculado ${mode.label.lowercase()} para $roleLabel. Inicia sesión con tu contraseña primero y activa el sensor biométrico.")
            return
        }
        val title = when (mode) {
            BiometricMode.HUELLA -> "Acceso con Huella Dactilar"
            BiometricMode.ROSTRO -> "Acceso con Reconocimiento Facial"
            BiometricMode.AMBOS -> "Acceso Biométrico (${credential.role})"
        }
        val subtitle = "Entrar como ${credential.displayName} (${credential.username} · ${credential.role})"
        viewModelScope.launch {
            when (val result = services.biometric.authenticate(title, subtitle, mode)) {
                BiometricResult.Success -> {
                    _state.update { it.copy(loading = true, error = null, notice = null) }
                    try {
                        val response = api.biometricLogin(credential.username, credential.deviceToken, mode.name)
                        startSession(response, viaBiometric = true)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: ApiFailure) {
                        api.token = null
                        if (e.isUnauthorized) {
                            services.secure.removeBiometricCredential(credential.username)
                            refreshBiometric()
                            fail("La credencial biométrica de ${credential.username} expiró o fue revocada. Entra con tu contraseña y vuelve a activarla.")
                        } else {
                            fail(e.message)
                        }
                    } catch (e: Exception) {
                        api.token = null
                        fail(e.message)
                    }
                }
                BiometricResult.Cancelled -> Unit
                is BiometricResult.Error -> fail(result.message)
            }
        }
    }

    private suspend fun startSession(response: LoginResponse, viaBiometric: Boolean) {
        api.token = response.token
        val verifiedUser = runCatching { api.me() }.getOrDefault(response.user)
        val role = Role.parse(verifiedUser.role)
        val worlds = api.worlds()
        val player = if (role == Role.ALUMNO) {
            api.playerLogin(verifiedUser.username, verifiedUser.displayName)
        } else null
        val creds = services.secure.loadAllBiometricCredentials()
        val alreadyEnrolled = creds.any { it.username.equals(verifiedUser.username, ignoreCase = true) }
        val offer = !viaBiometric && services.biometric.status() == BiometricStatus.AVAILABLE && !alreadyEnrolled
        _state.update {
            it.copy(
                loading = false,
                error = null,
                notice = if (viaBiometric) "Sesión iniciada con biometría (${role.title})" else null,
                session = AuthSession(response.token, verifiedUser.username, verifiedUser.displayName, role, viaBiometric),
                player = player,
                worlds = worlds,
                backStack = listOf(rootRouteFor(role)),
                biometricStatus = services.biometric.status(),
                hasFingerprint = services.biometric.hasFingerprint(),
                hasFace = services.biometric.hasFace(),
                biometricUser = creds.firstOrNull()?.username,
                biometricCredentials = creds,
                offerBiometric = offer,
                serverHealthy = true,
            )
        }
    }

    fun dismissBiometricOffer() = _state.update { it.copy(offerBiometric = false) }

    fun enrollBiometric(mode: BiometricMode = BiometricMode.AMBOS) {
        val session = _state.value.session ?: return
        _state.update { it.copy(offerBiometric = false, error = null, notice = null) }
        val title = when (mode) {
            BiometricMode.HUELLA -> "Registrar Huella Dactilar"
            BiometricMode.ROSTRO -> "Registrar Reconocimiento Facial"
            BiometricMode.AMBOS -> "Activar Sensor Biométrico"
        }
        val subtitle = "Vincular ${session.displayName} (${session.role.title}) en este dispositivo"
        viewModelScope.launch {
            when (val result = services.biometric.authenticate(title, subtitle, mode)) {
                BiometricResult.Success -> request { biometricEnroll() }.onSuccess { enrolled ->
                    services.secure.saveBiometricCredential(
                        BiometricCredential(
                            username = session.username,
                            deviceToken = enrolled.deviceToken,
                            role = session.role.name,
                            displayName = session.displayName,
                            mode = mode.name,
                        )
                    )
                    refreshBiometric()
                    _state.update { s ->
                        s.copy(notice = "${mode.icon} ${mode.label} activado para ${session.displayName} (${session.role.title})")
                    }
                }.onFailure { fail(it.message) }
                BiometricResult.Cancelled -> Unit
                is BiometricResult.Error -> fail(result.message)
            }
        }
    }

    fun disableBiometric(username: String? = null) {
        val target = username ?: _state.value.session?.username ?: _state.value.biometricUser
        viewModelScope.launch {
            if (_state.value.session != null) {
                request { biometricRevoke() }
            }
            if (target != null) {
                services.secure.removeBiometricCredential(target)
            } else {
                services.secure.clearBiometricCredential()
            }
            refreshBiometric()
            _state.update { it.copy(notice = "Acceso biométrico desactivado", error = null) }
        }
    }

    fun logout() {
        if (_state.value.session == null) return
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            try { api.logout() } catch (e: CancellationException) { throw e } catch (_: Exception) {}
            clearSession(null, "Sesión cerrada correctamente")
        }
    }

    private fun clearSession(error: String?, notice: String?) {
        api.token = null
        val creds = services.secure.loadAllBiometricCredentials()
        _state.value = UiState(
            error = error,
            notice = notice,
            serverUrl = serverUrl,
            serverHealthy = _state.value.serverHealthy,
            biometricStatus = services.biometric.status(),
            hasFingerprint = services.biometric.hasFingerprint(),
            hasFace = services.biometric.hasFace(),
            biometricUser = creds.firstOrNull()?.username,
            biometricCredentials = creds,
        )
    }

    // ---------------- Navegación ----------------
    fun navigate(route: Route) = _state.update { it.copy(backStack = it.backStack + route, error = null, notice = null) }
    fun replace(route: Route) = _state.update { it.copy(backStack = it.backStack.dropLast(1) + route, error = null, notice = null) }

    /** Vuelve a una ruta anterior de la pila (o la raíz del rol si no estaba). */
    fun popTo(route: Route) = _state.update { s ->
        val index = s.backStack.indexOfLast { it == route }
        if (index >= 0) s.copy(backStack = s.backStack.take(index + 1), error = null, notice = null)
        else s.copy(backStack = listOf(s.session?.let { rootRouteFor(it.role) } ?: Route.Login))
    }

    /** @return true si consumió el evento de "atrás". */
    fun back(): Boolean {
        if (_state.value.backStack.size <= 1) return false
        _state.update { it.copy(backStack = it.backStack.dropLast(1), error = null, notice = null) }
        return true
    }

    // ---------------- Acceso a la API con manejo de sesión expirada ----------------
    suspend fun <T> request(block: suspend ApiClient.() -> T): Result<T> = try {
        Result.success(api.block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: ApiFailure) {
        if (e.isUnauthorized && _state.value.session != null) {
            clearSession("Tu sesión expiró o fue cerrada. Inicia sesión de nuevo.", null)
        }
        Result.failure(e)
    } catch (e: Exception) {
        Result.failure(ApiFailure(0, e.message ?: "Error inesperado"))
    }

    fun updatePlayer(player: PlayerState) = _state.update { it.copy(player = player) }

    fun refreshPlayer() {
        if (_state.value.session?.role != Role.ALUMNO) return
        viewModelScope.launch { request { player() }.onSuccess(::updatePlayer) }
    }

    /** A-06: Guarda y sincroniza el estado del jugador en el backend. */
    fun syncPlayer() {
        val p = _state.value.player ?: return
        if (_state.value.session?.role != Role.ALUMNO) return
        viewModelScope.launch {
            request {
                savePlayer(
                    SavePlayerRequest(
                        xp = p.xp,
                        xpMax = p.xpMax,
                        nivel = p.nivel,
                        vidas = p.vidas,
                        monedas = p.monedas,
                        trofeos = p.trofeos,
                        completados = p.completados,
                    )
                )
            }.onSuccess(::updatePlayer)
        }
    }

    suspend fun answer(body: AnswerRequest): Result<AnswerResponse> =
        request { answer(body) }.onSuccess { updatePlayer(it.player) }

    suspend fun completeLevel(worldId: String, levelId: Int): Result<PlayerState> =
        request { completeLevel(worldId, levelId) }.onSuccess(::updatePlayer)

    suspend fun restoreLife(): Result<PlayerState> = request { restoreLife() }.onSuccess(::updatePlayer)
    suspend fun buyTrophy(id: String): Result<PlayerState> = request { buyTrophy(id) }.onSuccess(::updatePlayer)
    suspend fun rename(name: String): Result<PlayerState> = request { rename(name) }.onSuccess(::updatePlayer)
    suspend fun resetProgress(targetUsername: String? = null): Result<PlayerState> =
        request { resetProgress(targetUsername) }.onSuccess {
            if (targetUsername == null || targetUsername.equals(_state.value.session?.username, ignoreCase = true)) {
                updatePlayer(it)
            }
        }

    override fun onCleared() {
        api.close()
        super.onCleared()
    }
}
