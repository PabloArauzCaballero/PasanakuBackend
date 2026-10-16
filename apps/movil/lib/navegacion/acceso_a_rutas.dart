import '../proveedores/sesion.dart';

enum AccesoDeRuta { soloSinSesion, publica, privada }

const _soloSinSesion = <String>{'/portada', '/tour', '/ingreso', '/registro'};
const rutaDeVerificacionDeSesion = '/sesion/verificacion';

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
      if (requiereVerificacion && destino.path != rutaDeVerificacionDeSesion) {
        return rutaDeVerificacionDeSesion;
      }
      if (acceso == AccesoDeRuta.soloSinSesion) return '/billetera/inicio';
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
