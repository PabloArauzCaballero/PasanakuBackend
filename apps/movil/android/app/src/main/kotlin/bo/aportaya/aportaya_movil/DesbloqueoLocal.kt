package bo.aportaya.aportaya_movil

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import io.flutter.plugin.common.MethodChannel

/// Desbloqueo local con la huella **o el PIN/patrón del teléfono** (2026-10-17).
///
/// Antes, con la huella rota o sin enrolar, el diálogo nativo podía cortarse sin
/// ofrecer el PIN y el botón «Entrar con huella o PIN» no hacía nada. Y si un pedido
/// quedaba colgado (por ejemplo, el automático del arranque), los toques siguientes
/// se ignoraban. Ahora:
/// - cada pedido nuevo reemplaza al anterior (que responde `false` y se cancela);
/// - sin huella usable, o si la huella falla por el sensor o por bloqueo, se pide el
///   PIN/patrón del teléfono (BiometricPrompt en Android 11+, KeyguardManager antes).
/// Confirma presencia local; no autentica ante el servidor.
class DesbloqueoLocal(private val actividad: FragmentActivity) {

    private inner class Intento(private val resultado: MethodChannel.Result, val motivo: String) {
        private var respondido = false

        fun responder(valor: Boolean) {
            if (respondido) return
            respondido = true
            resultado.success(valor)
        }
    }

    private var intentoActual: Intento? = null
    private var promptActual: BiometricPrompt? = null

    fun disponible(): Boolean = biometriaLista() || credencialLista()

    fun confirmar(motivo: String, resultado: MethodChannel.Result) {
        intentoActual?.responder(false)
        promptActual?.cancelAuthentication()
        promptActual = null
        val intento = Intento(resultado, motivo)
        intentoActual = intento
        when {
            biometriaLista() -> conBiometria(intento)
            credencialLista() -> conCredencial(intento)
            else -> intento.responder(false)
        }
    }

    /// Respuesta de la pantalla de PIN del sistema en Android 10 o anterior.
    fun alVolverDeLaCredencial(codigo: Int, resultadoActividad: Int): Boolean {
        if (codigo != CODIGO_CREDENCIAL) return false
        intentoActual?.responder(resultadoActividad == Activity.RESULT_OK)
        return true
    }

    private fun biometriaLista(): Boolean =
        BiometricManager.from(actividad).canAuthenticate(BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS

    private fun credencialLista(): Boolean {
        val guardia = actividad.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager?
        return guardia?.isDeviceSecure == true
    }

    private fun conBiometria(intento: Intento) {
        val conPin = credencialLista()
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("AportaYa")
            .setSubtitle(intento.motivo)
            .apply {
                if (conPin) {
                    setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
                } else {
                    setAllowedAuthenticators(BIOMETRIC_WEAK)
                    setNegativeButtonText("Cancelar")
                }
            }
            .build()
        lanzar(intento, info, alFallarElSensor = conPin)
    }

    private fun conCredencial(intento: Intento) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle("AportaYa")
                .setSubtitle(intento.motivo)
                .setAllowedAuthenticators(DEVICE_CREDENTIAL)
                .build()
            lanzar(intento, info, alFallarElSensor = false)
            return
        }
        val guardia = actividad.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager?
        @Suppress("DEPRECATION")
        val pantalla = guardia?.createConfirmDeviceCredentialIntent("AportaYa", intento.motivo)
        if (pantalla == null) {
            intento.responder(false)
            return
        }
        @Suppress("DEPRECATION")
        actividad.startActivityForResult(pantalla, CODIGO_CREDENCIAL)
    }

    private fun lanzar(intento: Intento, info: BiometricPrompt.PromptInfo, alFallarElSensor: Boolean) {
        val prompt = BiometricPrompt(
            actividad,
            ContextCompat.getMainExecutor(actividad),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(resultado: BiometricPrompt.AuthenticationResult) {
                    intento.responder(true)
                }

                override fun onAuthenticationError(codigo: Int, mensaje: CharSequence) {
                    // Si ya hay otro pedido, este quedó reemplazado y ya respondió.
                    if (intento !== intentoActual) return
                    if (alFallarElSensor && codigo in ERRORES_DEL_SENSOR && credencialLista()) {
                        conCredencial(intento)
                    } else {
                        intento.responder(false)
                    }
                }

                override fun onAuthenticationFailed() {
                    // Un intento fallido no cierra el diálogo; se espera el resultado final.
                }
            },
        )
        promptActual = prompt
        prompt.authenticate(info)
    }

    private companion object {
        const val CODIGO_CREDENCIAL = 4321

        /// La huella no se pudo usar por el sensor o por bloqueo (no porque la persona
        /// canceló): ahí se pasa al PIN del teléfono.
        val ERRORES_DEL_SENSOR = setOf(
            BiometricPrompt.ERROR_HW_UNAVAILABLE,
            BiometricPrompt.ERROR_UNABLE_TO_PROCESS,
            BiometricPrompt.ERROR_NO_SPACE,
            BiometricPrompt.ERROR_LOCKOUT,
            BiometricPrompt.ERROR_VENDOR,
            BiometricPrompt.ERROR_LOCKOUT_PERMANENT,
            BiometricPrompt.ERROR_NO_BIOMETRICS,
            BiometricPrompt.ERROR_HW_NOT_PRESENT,
        )
    }
}
