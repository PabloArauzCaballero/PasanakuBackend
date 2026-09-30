import 'enlaces_profundos.dart';
import 'retorno_de_invitacion.dart';

/// Tope del texto de un QR que se acepta mirar. Un QR legítimo de invitación mide
/// ~110 caracteres; uno enorme es basura o un intento de colgar el análisis.
const int _largoMaximoDelQr = 512;

/// **De lo que dice un QR a una ruta interna, o a nada.** Solo se acepta una invitación
/// de esquema propio (`aportaya://unirse/<uuid>.<hash>`): pasa por la misma traducción
/// que los enlaces externos y por el mismo validador estricto que el retorno del ingreso.
/// Un QR de cobro, una URL web o cualquier otro texto devuelve `null`.
///
/// Escanear no mueve plata ni acredita nada (regla 91.3): solo lleva a la pantalla donde
/// la persona confirma. El texto del QR nunca se registra en logs.
String? rutaDeInvitacionDesdeQr(String? texto) {
  if (texto == null) return null;
  final limpio = texto.trim();
  if (limpio.isEmpty || limpio.length > _largoMaximoDelQr) return null;
  final uri = Uri.tryParse(limpio);
  if (uri == null) return null;
  final interna = rutaInternaDesde(uri);
  return interna == null ? null : retornoDeInvitacion(interna);
}

/// **El cerrojo anti multi-lectura.** La cámara detecta el mismo QR decenas de veces por
/// segundo; sin esto se navegaría otras tantas. [tomar] deja pasar solo la primera
/// lectura hasta que se [liberar] (p. ej. al volver a la pantalla tras un rechazo).
class CerrojoDeLectura {
  bool _tomado = false;

  bool tomar() {
    if (_tomado) return false;
    _tomado = true;
    return true;
  }

  void liberar() => _tomado = false;
}
