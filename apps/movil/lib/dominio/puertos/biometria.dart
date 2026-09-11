/// La biometría **confirma, no autentica** (regla 2 del shell, `planes/12`): quien
/// autentica es el servidor con el token; la huella o el rostro solo desbloquean el
/// envío de una operación ya armada. La implementación (BiometricPrompt en Android,
/// LocalAuthentication en iOS) vive en `infraestructura/`.
abstract interface class Biometria {
  /// Si el dispositivo tiene sensor y ya hay biometría enrolada.
  Future<bool> disponible();

  /// Pide confirmación con [motivo] visible en el diálogo nativo (por ejemplo,
  /// «Confirmá el envío de Bs 150»). `true` si la persona confirmó.
  Future<bool> confirmar(String motivo);
}
