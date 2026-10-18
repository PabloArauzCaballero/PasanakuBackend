package bo.aportaya.aportaya_movil

import android.content.Intent
import android.view.WindowManager
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
    private val desbloqueo by lazy { DesbloqueoLocal(this) }
    private val compartir by lazy { CompartirMensaje(this) }

    @Deprecated("La pantalla de PIN del sistema en Android 10 o anterior responde por acá")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (desbloqueo.alVolverDeLaCredencial(requestCode, resultCode)) return
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, canalBiometria)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "disponible" -> result.success(desbloqueo.disponible())
                    "confirmar" -> desbloqueo.confirmar(
                        call.argument<String>("motivo") ?: "Confirmá la operación",
                        result,
                    )
                    else -> result.notImplemented()
                }
            }

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "bo.aportaya/compartir")
            .setMethodCallHandler { call, result ->
                val telefono = call.argument<String>("telefono") ?: ""
                val texto = call.argument<String>("texto") ?: ""
                when (call.method) {
                    "whatsapp" -> result.success(compartir.porWhatsApp(telefono, texto))
                    "sms" -> result.success(compartir.porSms(telefono, texto))
                    "otra" -> result.success(compartir.conOtraApp(texto))
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
