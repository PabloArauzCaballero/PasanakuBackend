package bo.aportaya.aportaya_movil

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
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
/// 2026-10-19: en un Samsung con PIN y rostro el botón seguía sin hacer nada. Dos
/// caminos en los que el pedido se perdía sin respuesta:
/// - `BiometricPrompt.authenticate` se ignora en silencio si la actividad ya guardó su
///   estado (pantalla apagándose, volviendo de la pantalla de bloqueo): ahora se
///   espera a que la actividad vuelva al frente y recién ahí se abre;
/// - un error del sistema se contestaba igual que «la persona canceló»: ahora vuelve
///   como error con su código, y la app lo muestra.
/// Confirma presencia local; no autentica ante el servidor.
class DesbloqueoLocal(private val actividad: FragmentActivity) {

    private inner class Intento(private val resultado: MethodChannel.Result, val motivo: String) {
        private var respondido = false

        fun responder(valor: Boolean) {
            if (respondido) return
            respondido = true
            resultado.success(valor)
        }

        fun fallar(codigo: Int, mensaje: CharSequence) {
            if (respondido) return
            respondido = true
            resultado.error("DESBLOQUEO", "$codigo: $mensaje", null)
        }
    }

    private val principal = Handler(Looper.getMainLooper())

    private var intentoActual: Intento? = null
    private var promptActual: BiometricPrompt? = null

    fun disponible(): Boolean = biometriaLista() || credencialLista()

    fun confirmar(motivo: String, resultado: MethodChannel.Result) {
        intentoActual?.responder(false)
        val habiaUnDialogo = promptActual != null
        promptActual?.cancelAuthentication()
        promptActual = null
        val intento = Intento(resultado, motivo)
        intentoActual = intento
        val abrir = Runnable { cuandoEsteAlFrente(intento) { abrir(intento) } }
        // El diálogo anterior tarda un instante en irse: abrir el nuevo en el mismo
        // instante puede perderse. Se abre en la próxima vuelta del hilo principal.
        if (habiaUnDialogo) principal.post(abrir) else abrir.run()
    }

    private fun abrir(intento: Intento) {
        if (intento !== intentoActual) return
        when {
            biometriaLista() -> conBiometria(intento)
            credencialLista() -> conCredencial(intento)
            else -> intento.fallar(-1, "Sin huella ni PIN configurados en el teléfono")
        }
    }

    /// `authenticate` con la actividad detenida no hace nada y no avisa: se espera a
    /// que vuelva al frente (por ejemplo, después de la pantalla de bloqueo).
    private fun cuandoEsteAlFrente(intento: Intento, accion: () -> Unit) {
        val ciclo = actividad.lifecycle
        if (ciclo.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
            !actividad.supportFragmentManager.isStateSaved
        ) {
            accion()
            return
        }
        ciclo.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                ciclo.removeObserver(this)
                if (intento === intentoActual) principal.post { accion() }
            }
        })
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
            intento.fallar(-3, "El teléfono no ofreció la pantalla de PIN")
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
                    } else if (codigo in CANCELACIONES) {
                        intento.responder(false)
                    } else {
                        intento.fallar(codigo, mensaje)
                    }
                }

                override fun onAuthenticationFailed() {
                    // Un intento fallido no cierra el diálogo; se espera el resultado final.
                }
            },
        )
        promptActual = prompt
        try {
            prompt.authenticate(info)
        } catch (e: RuntimeException) {
            intento.fallar(-2, e.message ?: e.javaClass.simpleName)
        }
    }

    private companion object {
        const val CODIGO_CREDENCIAL = 4321

        /// La huella no se pudo usar por el sensor o por bloqueo (no porque la persona
        /// canceló): ahí se pasa al PIN del teléfono.
        /// La persona (o el sistema al apagar la pantalla) cerró el diálogo: no es un
        /// fallo que haya que mostrar.
        val CANCELACIONES = setOf(
            BiometricPrompt.ERROR_USER_CANCELED,
            BiometricPrompt.ERROR_NEGATIVE_BUTTON,
            BiometricPrompt.ERROR_CANCELED,
            BiometricPrompt.ERROR_TIMEOUT,
        )

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
