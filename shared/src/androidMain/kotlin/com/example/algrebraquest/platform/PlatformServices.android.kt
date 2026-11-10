package com.example.algrebraquest.platform

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.coroutines.resume

/**
 * Referencias de Android que necesita el código compartido. La actividad principal las
 * inicializa en onCreate y las libera en onDestroy.
 */
object AndroidHost {
    var appContext: Context? = null
    var activity: FragmentActivity? = null

    fun attach(activity: FragmentActivity) {
        this.activity = activity
        this.appContext = activity.applicationContext
    }

    fun detach(activity: Activity) {
        if (this.activity === activity) this.activity = null
    }
}

private const val AUTHENTICATORS = BIOMETRIC_STRONG or BIOMETRIC_WEAK

private class AndroidBiometricGate : BiometricGate {
    override fun status(): BiometricStatus {
        val context = AndroidHost.appContext ?: return BiometricStatus.UNAVAILABLE
        return when (BiometricManager.from(context).canAuthenticate(AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.UNSUPPORTED
            else -> BiometricStatus.UNAVAILABLE
        }
    }

    override fun hasFingerprint(): Boolean {
        val context = AndroidHost.appContext ?: return false
        val pm = context.packageManager
        return pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT) || status() == BiometricStatus.AVAILABLE
    }

    override fun hasFace(): Boolean {
        val context = AndroidHost.appContext ?: return false
        val pm = context.packageManager
        val faceFeature = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            pm.hasSystemFeature(PackageManager.FEATURE_FACE)
        } else false
        return faceFeature || status() == BiometricStatus.AVAILABLE
    }

    override suspend fun authenticate(
        title: String,
        subtitle: String,
        mode: BiometricMode,
    ): BiometricResult {
        val activity = AndroidHost.activity
            ?: return BiometricResult.Error("No hay una pantalla activa para mostrar la verificación biométrica.")
        if (status() != BiometricStatus.AVAILABLE) {
            return BiometricResult.Error("Este dispositivo no tiene huella dactilar ni reconocimiento facial configurados.")
        }
        val modeHint = when (mode) {
            BiometricMode.HUELLA -> "Coloca tu dedo en el lector de huella dactilar"
            BiometricMode.ROSTRO -> "Mira a la cámara frontal para el reconocimiento facial"
            BiometricMode.AMBOS -> "Verifica tu identidad con huella dactilar o reconocimiento facial"
        }
        return suspendCancellableCoroutine { continuation ->
            val executor = ContextCompat.getMainExecutor(activity)
            val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (continuation.isActive) continuation.resume(BiometricResult.Success)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    val cancelled = errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_CANCELED
                    if (continuation.isActive) {
                        continuation.resume(
                            if (cancelled) BiometricResult.Cancelled
                            else BiometricResult.Error(errString.toString())
                        )
                    }
                }
            })
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setDescription(modeHint)
                .setAllowedAuthenticators(AUTHENTICATORS)
                .setNegativeButtonText("Usar contraseña")
                .setConfirmationRequired(false)
                .build()
            prompt.authenticate(info)
            continuation.invokeOnCancellation { prompt.cancelAuthentication() }
        }
    }
}

/** Guarda los tokens de dispositivo cifrados con AES-GCM por usuario y rol; la clave vive en Android Keystore. */
private class AndroidSecureStore(context: Context) : SecureStore {
    private val prefs: SharedPreferences = context.getSharedPreferences("aq_secure", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private fun encrypt(plainText: String): Pair<String, String> {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encrypted, Base64.NO_WRAP) to Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
    }

    private fun decrypt(cipherB64: String, ivB64: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(ivB64, Base64.NO_WRAP)))
        }
        return String(cipher.doFinal(Base64.decode(cipherB64, Base64.NO_WRAP)), Charsets.UTF_8)
    }

    override fun saveBiometricCredential(credential: BiometricCredential) {
        val current = loadAllBiometricCredentials()
            .filterNot { it.username.equals(credential.username, ignoreCase = true) }
            .toMutableList()
        current.add(0, credential) // El más reciente queda primero como predeterminado
        val payload = json.encodeToString(current)
        val (encList, ivList) = encrypt(payload)
        val (encSingle, ivSingle) = encrypt(credential.deviceToken)
        prefs.edit()
            .putString(PREF_USER, credential.username)
            .putString(PREF_ROLE, credential.role)
            .putString(PREF_DISPLAY, credential.displayName)
            .putString(PREF_MODE, credential.mode)
            .putString(PREF_TOKEN, encSingle)
            .putString(PREF_IV, ivSingle)
            .putString(PREF_LIST_DATA, encList)
            .putString(PREF_LIST_IV, ivList)
            .apply()
    }

    override fun loadAllBiometricCredentials(): List<BiometricCredential> {
        val listData = prefs.getString(PREF_LIST_DATA, null)
        val listIv = prefs.getString(PREF_LIST_IV, null)
        if (listData != null && listIv != null) {
            val decoded = runCatching {
                json.decodeFromString<List<BiometricCredential>>(decrypt(listData, listIv))
            }.getOrNull()
            if (!decoded.isNullOrEmpty()) return decoded
        }
        return listOfNotNull(loadSingleFallback())
    }

    private fun loadSingleFallback(): BiometricCredential? {
        val user = prefs.getString(PREF_USER, null) ?: return null
        val token = prefs.getString(PREF_TOKEN, null) ?: return null
        val iv = prefs.getString(PREF_IV, null) ?: return null
        val role = prefs.getString(PREF_ROLE, "ALUMNO") ?: "ALUMNO"
        val display = prefs.getString(PREF_DISPLAY, user) ?: user
        val mode = prefs.getString(PREF_MODE, BiometricMode.AMBOS.name) ?: BiometricMode.AMBOS.name
        return runCatching {
            BiometricCredential(user, decrypt(token, iv), role, display, mode)
        }.getOrElse { null }
    }

    override fun loadBiometricCredential(): BiometricCredential? =
        loadAllBiometricCredentials().firstOrNull()

    override fun removeBiometricCredential(username: String) {
        val remaining = loadAllBiometricCredentials()
            .filterNot { it.username.equals(username, ignoreCase = true) }
        if (remaining.isEmpty()) {
            clearBiometricCredential()
        } else {
            val payload = json.encodeToString(remaining)
            val (encList, ivList) = encrypt(payload)
            val first = remaining.first()
            val (encSingle, ivSingle) = encrypt(first.deviceToken)
            prefs.edit()
                .putString(PREF_USER, first.username)
                .putString(PREF_ROLE, first.role)
                .putString(PREF_DISPLAY, first.displayName)
                .putString(PREF_MODE, first.mode)
                .putString(PREF_TOKEN, encSingle)
                .putString(PREF_IV, ivSingle)
                .putString(PREF_LIST_DATA, encList)
                .putString(PREF_LIST_IV, ivList)
                .apply()
        }
    }

    override fun clearBiometricCredential() {
        prefs.edit()
            .remove(PREF_USER)
            .remove(PREF_ROLE)
            .remove(PREF_DISPLAY)
            .remove(PREF_MODE)
            .remove(PREF_TOKEN)
            .remove(PREF_IV)
            .remove(PREF_LIST_DATA)
            .remove(PREF_LIST_IV)
            .apply()
    }

    override var serverUrl: String?
        get() = prefs.getString(PREF_SERVER, null)
        set(value) { prefs.edit().putString(PREF_SERVER, value).apply() }

    private companion object {
        const val KEY_ALIAS = "algebraquest_biometric_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val PREF_USER = "bio_user"
        const val PREF_ROLE = "bio_role"
        const val PREF_DISPLAY = "bio_display"
        const val PREF_MODE = "bio_mode"
        const val PREF_TOKEN = "bio_token"
        const val PREF_IV = "bio_iv"
        const val PREF_LIST_DATA = "bio_list_data"
        const val PREF_LIST_IV = "bio_list_iv"
        const val PREF_SERVER = "server_url"
    }
}

actual fun platformServices(): PlatformServices {
    val context = checkNotNull(AndroidHost.appContext) { "AndroidHost.attach(activity) debe llamarse en onCreate" }
    // 10.0.2.2 es el equipo anfitrión visto desde el emulador de Android.
    return PlatformServices(AndroidBiometricGate(), AndroidSecureStore(context), "http://10.0.2.2:8080")
}

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
