package bo.aportaya.aportaya_movil

import android.view.WindowManager
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import io.flutter.embedding.android.FlutterFragmentActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

/// Los tres canales nativos de los puertos del shell (carril F2 · `movil-flutter`):
/// biometría, protección de pantalla y cámara. `Haptica`, `AlmacenSeguro` y
/// `Conectividad` no necesitan código nativo propio — `HapticFeedback` y
/// `flutter_secure_storage`/`connectivity_plus` ya resuelven todo del lado Dart.
/// `AvisosPush` no tiene receptor nativo todavía: es el hueco declarado del informe
/// del carril (pide el SDK de Firebase, decisión que no es de este carril).
///
/// `FlutterFragmentActivity`, no `FlutterActivity`: `BiometricPrompt` exige una
/// `FragmentActivity` para alojar su propio `DialogFragment` interno — con
/// `FlutterActivity` (que extiende `Activity` a secas) `BiometricPrompt(this, …)` ni
/// compila. Hallazgo real, encontrado al compilar de verdad para Android.
class MainActivity : FlutterFragmentActivity() {
    private val canalBiometria = "bo.aportaya/biometria"
    private val canalProteccionPantalla = "bo.aportaya/proteccion_pantalla"
    private val canalCamara = "bo.aportaya/camara"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, canalBiometria)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "disponible" -> {
                        val gestor = BiometricManager.from(this)
                        val estado = gestor.canAuthenticate(
                            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                                BiometricManager.Authenticators.DEVICE_CREDENTIAL,
                        )
                        result.success(estado == BiometricManager.BIOMETRIC_SUCCESS)
                    }
                    "confirmar" -> {
                        val motivo = call.argument<String>("motivo") ?: "Confirmá la operación"
                        val ejecutor = ContextCompat.getMainExecutor(this)
                        val prompt = BiometricPrompt(
                            this,
                            ejecutor,
                            object : BiometricPrompt.AuthenticationCallback() {
                                override fun onAuthenticationSucceeded(
                                    resultado: BiometricPrompt.AuthenticationResult,
                                ) {
                                    result.success(true)
                                }

                                override fun onAuthenticationError(codigo: Int, mensaje: CharSequence) {
                                    result.success(false)
                                }

                                override fun onAuthenticationFailed() {
                                    // Un intento fallido no cierra el diálogo; se espera el
                                    // resultado final (éxito, error o cancelación).
                                }
                            },
                        )
                        val info = BiometricPrompt.PromptInfo.Builder()
                            .setTitle("AportaYa")
                            .setSubtitle(motivo)
                            .setAllowedAuthenticators(
                                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                                    BiometricManager.Authenticators.DEVICE_CREDENTIAL,
                            )
                            .build()
                        prompt.authenticate(info)
                    }
                    else -> result.notImplemented()
                }
            }

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, canalProteccionPantalla)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "activar" -> {
                        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                        result.success(null)
                    }
                    "desactivar" -> {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                        result.success(null)
                    }
                    else -> result.notImplemented()
                }
            }

        // `capturar` con `MediaStore.ACTION_IMAGE_CAPTURE`: el receptor de actividad
        // (`registerForActivityResult`) es trabajo del primer usuario real del
        // puerto (F3, cotejo documental de CU-01). El canal ya está declarado y el
        // método responde `notImplemented` hasta entonces — hueco anotado en el
        // informe del carril, no un `TODO` suelto.
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, canalCamara)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "capturar" -> result.success(null)
                    else -> result.notImplemented()
                }
            }
    }
}
