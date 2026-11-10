package com.example.algrebraquest.platform

import androidx.compose.runtime.Composable
import kotlinx.serialization.Serializable

/** Estado del sensor biométrico (huella / reconocimiento facial) del dispositivo. */
enum class BiometricStatus { AVAILABLE, NOT_ENROLLED, UNSUPPORTED, UNAVAILABLE }

/** Modalidad biométrica seleccionada por el usuario. */
enum class BiometricMode(val label: String, val icon: String) {
    HUELLA("Huella dactilar", "🫆"),
    ROSTRO("Reconocimiento facial", "😊"),
    AMBOS("Huella o rostro", "🔐"),
}

sealed interface BiometricResult {
    data object Success : BiometricResult
    data object Cancelled : BiometricResult
    data class Error(val message: String) : BiometricResult
}

interface BiometricGate {
    fun status(): BiometricStatus
    fun hasFingerprint(): Boolean = status() == BiometricStatus.AVAILABLE
    fun hasFace(): Boolean = status() == BiometricStatus.AVAILABLE
    suspend fun authenticate(
        title: String,
        subtitle: String,
        mode: BiometricMode = BiometricMode.AMBOS,
    ): BiometricResult
}

/** Credencial biométrica del dispositivo: usuario + rol + token de dispositivo (nunca la contraseña). */
@Serializable
data class BiometricCredential(
    val username: String,
    val deviceToken: String,
    val role: String = "ALUMNO",
    val displayName: String = username,
    val mode: String = BiometricMode.AMBOS.name,
)

interface SecureStore {
    fun saveBiometricCredential(credential: BiometricCredential)
    fun loadBiometricCredential(): BiometricCredential?
    fun loadAllBiometricCredentials(): List<BiometricCredential> =
        listOfNotNull(loadBiometricCredential())
    fun loadBiometricCredentialForRole(role: String): BiometricCredential? =
        loadAllBiometricCredentials().firstOrNull { it.role.equals(role, ignoreCase = true) }
    fun loadBiometricCredentialForUser(username: String): BiometricCredential? =
        loadAllBiometricCredentials().firstOrNull { it.username.equals(username, ignoreCase = true) }
    fun removeBiometricCredential(username: String) {
        if (loadBiometricCredential()?.username.equals(username, ignoreCase = true)) {
            clearBiometricCredential()
        }
    }
    fun clearBiometricCredential()
    var serverUrl: String?
}

class PlatformServices(
    val biometric: BiometricGate,
    val secure: SecureStore,
    val defaultServerUrl: String,
)

expect fun platformServices(): PlatformServices

/** Botón "atrás" del sistema. */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
