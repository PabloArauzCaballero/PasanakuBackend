import 'enlaces_profundos.dart';
import 'retorno_de_invitacion.dart';

const int _largoMaximo = 300;
const String _prefijoDeInvitacion = '/pasanaku/unirse';

/// **De lo que trae un aviso push a una ruta interna, o a nada.**
///
/// El contenido de un push lo controla quien tenga el token del dispositivo, así que se
/// trata como no confiable: un aviso nunca puede abrir un sitio externo ni una pantalla
/// que la app no publicó. Se acepta solo:
/// - un enlace del esquema propio (`aportaya://billetera`), traducido por la misma tabla
///   que los enlaces externos (`rutaInternaDesde`), o
/// - una ruta interna absoluta, sin consulta ni fragmento, que caiga en uno de esos
///   mismos destinos.
/// Una invitación pasa además por el validador estricto del retorno del ingreso.
///
/// Un aviso solo **lleva** a una pantalla: nada se da por pagado ni por hecho porque
/// alguien tocó un push (regla 91.3).
String? rutaDelAviso(String? valor) {
  if (valor == null) return null;
  final texto = valor.trim();
  if (texto.isEmpty || texto.length > _largoMaximo) return null;

  final String? ruta;
  if (texto.startsWith('aportaya://')) {
    final uri = Uri.tryParse(texto);
    ruta = uri == null ? null : rutaInternaDesde(uri);
  } else if (texto.startsWith('/') && !texto.startsWith('//')) {
    final uri = Uri.tryParse(texto);
    ruta =
        (uri == null ||
            uri.hasScheme ||
            uri.hasAuthority ||
            uri.hasQuery ||
            uri.hasFragment)
        ? null
        : uri.path;
  } else {
    ruta = null;
  }
  if (ruta == null) return null;
  final destino = ruta;
  if (destino.split('/').contains('..')) return null;

  final permitida = rutasBaseDeEnlaces.any(
    (base) => destino == base || destino.startsWith('$base/'),
  );
  if (!permitida) return null;
  if (destino == _prefijoDeInvitacion ||
      destino.startsWith('$_prefijoDeInvitacion/')) {
    return retornoDeInvitacion(destino);
  }
  return destino;
}
