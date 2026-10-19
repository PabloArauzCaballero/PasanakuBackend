import '../proveedores/sesion.dart';

enum AccesoDeRuta { soloSinSesion, publica, privada }

const _soloSinSesion = <String>{'/portada', '/tour', '/ingreso', '/registro'};
const rutaDeVerificacionDeSesion = '/sesion/verificacion';

/// Se interpone cuando una cuenta sin identidad verificada intenta mover dinero o
/// participar en un pasanaku. Desde ahí se verifica (o se ve en qué va la revisión).
const rutaDeIdentidadRequerida = '/identidad/requerida';

/// La verificación de identidad desde adentro de la app (carnet, fotos, actividad).
const rutaDeVerificarIdentidad = '/identidad/verificar';

/// Lo que mueve dinero o compromete a la persona en un grupo. Ver la billetera, un
/// grupo o una invitación sigue libre: la identidad se pide al operar, no al mirar.
final _exigenIdentidad = <RegExp>[
  RegExp(r'^/billetera/(recargar|retirar|transferir)$'),
  RegExp(r'^/pasanaku/grupos/nuevo$'),
  RegExp(r'^/pasanaku/grupos/[^/]+/pedir-cupo$'),
  RegExp(r'^/pasanaku/obligaciones/[^/]+/aportar$'),
];

bool exigeIdentidadVerificada(String ruta) {
  final camino = Uri.parse(ruta).path;
  return _exigenIdentidad.any((r) => r.hasMatch(camino));
}

const _publicas = <String>{
  '/identidad/mfa',
  '/identidad/verificacion-basica',
  '/identidad/contrato',
  '/identidad/bienvenida',
  '/registro/camara',
  '/registro/pautas',
  '/registro/prueba-de-vida',
  '/registro/subida',
  '/registro/estado',
};

/// Unknown routes default private: adding a route can never silently expose it.
AccesoDeRuta accesoDe(String ruta) {
  final camino = Uri.parse(ruta).path;
  if (_soloSinSesion.contains(camino)) return AccesoDeRuta.soloSinSesion;
  if (camino.startsWith('/pasanaku/unirse/')) return AccesoDeRuta.publica;
  if (_publicas.contains(camino)) return AccesoDeRuta.publica;
  return AccesoDeRuta.privada;
}

String? decidirRuta({
  required EstadoDeSesion estado,
  required Uri destino,
  bool requiereVerificacion = false,
}) {
  final acceso = accesoDe(destino.toString());
  switch (estado) {
    case EstadoDeSesion.desconocida:
      return acceso == AccesoDeRuta.privada ? '/ingreso' : null;
    case EstadoDeSesion.anonima:
      if (acceso != AccesoDeRuta.privada) return null;
      return Uri(
        path: '/ingreso',
        queryParameters: {'volver': destino.toString()},
      ).toString();
    case EstadoDeSesion.activa:
      // Sin la identidad verificada se entra igual a la app: el aviso del shell lo
      // dice. Encerrar a la persona en una pantalla con «Salir» como única salida
      // era lo que la hacía desinstalar la app. La verificación se pide recién al
      // operar con dinero o entrar a un grupo (2026-10-17).
      if (acceso == AccesoDeRuta.soloSinSesion) return '/billetera/inicio';
      if (requiereVerificacion &&
          exigeIdentidadVerificada(destino.toString())) {
        return Uri(
          path: rutaDeIdentidadRequerida,
          queryParameters: {'volver': destino.toString()},
        ).toString();
      }
      return null;
  }
}

/// A caller-controlled return path may only target an internal private screen.
String destinoTrasIngresar(String? volver) {
  if (volver == null || !volver.startsWith('/') || volver.startsWith('//')) {
    return '/billetera/inicio';
  }
  final uri = Uri.tryParse(volver);
  if (uri == null || uri.hasScheme || uri.hasAuthority) {
    return '/billetera/inicio';
  }
  return accesoDe(volver) == AccesoDeRuta.privada
      ? volver
      : '/billetera/inicio';
}
