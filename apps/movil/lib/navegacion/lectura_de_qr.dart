import '../dominio/cliente.dart' show baseDelGateway;
import '../pantallas/pasanaku/dominio/enlace_para_compartir.dart';
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
///
/// También se acepta el enlace https que se comparte por WhatsApp o SMS (2026-10-18),
/// **solo** si es de la app web propia ([appWeb]); cualquier otra URL sigue siendo `null`.
String? rutaDeInvitacionDesdeQr(String? texto, {String? appWeb}) {
  if (texto == null) return null;
  final limpio = texto.trim();
  if (limpio.isEmpty || limpio.length > _largoMaximoDelQr) return null;
  final uri = Uri.tryParse(limpio);
  if (uri == null) return null;
  // Un https ajeno no entra nunca: el dominio se compara antes que nada.
  final interna = uri.scheme == 'https'
      ? _desdeLaAppWeb(uri, appWeb)
      : rutaInternaDesde(uri);
  return interna == null ? null : retornoDeInvitacion(interna);
}

String? _desdeLaAppWeb(Uri uri, String? appWeb) {
  final propia = Uri.tryParse(
    appWeb ?? baseDeLaAppWeb(api: baseDelGateway) ?? '',
  );
  if (propia == null || propia.host.isEmpty) return null;
  if (uri.scheme != 'https' || uri.host != propia.host) return null;
  final delGrupo = codigoDeEnlaceDelGrupo(uri);
  if (delGrupo != null) return '/g/$delGrupo';
  final codigo = codigoDeInvitacion(uri.toString());
  return codigo == null ? null : '/pasanaku/unirse/$codigo';
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
