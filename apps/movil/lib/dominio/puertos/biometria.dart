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

/// Lo que contestó el sistema la última vez que no pudo abrir el diálogo de huella o
/// PIN (código y mensaje del propio sistema), para que el candado lo diga en vez de
/// quedarse quieto (2026-10-19). `null` si la última vez no hubo fallo.
abstract interface class DiagnosticoDeBiometria {
  String? get ultimoFallo;
}
