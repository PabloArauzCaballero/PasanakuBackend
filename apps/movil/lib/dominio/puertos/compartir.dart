/// Compartir un texto con otra app del teléfono (2026-10-18): la invitación a un grupo
/// se manda desde el WhatsApp o los SMS de quien invita, porque el servidor no tiene un
/// proveedor de mensajería conectado. Cada método devuelve `false` si no se pudo abrir
/// la app elegida; quien llama ofrece copiar el enlace en ese caso.
abstract interface class Compartir {
  /// Abre el chat de WhatsApp con [telefono] (E.164) y el [texto] escrito.
  Future<bool> porWhatsApp({required String telefono, required String texto});

  /// Abre un SMS nuevo para [telefono] con el [texto] escrito.
  Future<bool> porSms({required String telefono, required String texto});

  /// Abre la hoja de compartir del sistema con el [texto].
  Future<bool> conOtraApp({required String texto});
}
