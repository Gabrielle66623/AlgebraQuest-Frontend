package com.example.algrebraquest.platform

import androidx.compose.runtime.Composable

private class IosBiometricGate : BiometricGate {
    override fun status() = BiometricStatus.UNSUPPORTED
    override suspend fun authenticate(
        title: String,
        subtitle: String,
        mode: BiometricMode,
    ): BiometricResult = BiometricResult.Error("Biometría no disponible en iOS todavía.")
}

private class MemorySecureStore : SecureStore {
    private val credentials = mutableListOf<BiometricCredential>()
    override fun saveBiometricCredential(credential: BiometricCredential) {
        credentials.removeAll { it.username.equals(credential.username, ignoreCase = true) }
        credentials.add(0, credential)
    }
    override fun loadBiometricCredential() = credentials.firstOrNull()
    override fun loadAllBiometricCredentials(): List<BiometricCredential> = credentials.toList()
    override fun removeBiometricCredential(username: String) {
        credentials.removeAll { it.username.equals(username, ignoreCase = true) }
    }
    override fun clearBiometricCredential() { credentials.clear() }
    override var serverUrl: String? = null
}

actual fun platformServices(): PlatformServices =
    PlatformServices(IosBiometricGate(), MemorySecureStore(), "http://localhost:8080")

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS no tiene botón atrás de sistema.
}
