import 'package:aportaya_movil/navegacion/acceso_a_rutas.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  const privadas = <String>[
    '/pasanaku',
    '/identidad',
    '/notificaciones/bandeja',
    '/billetera/inicio',
    '/billetera/recargar',
    '/billetera/retirar',
    '/billetera/transferir',
    '/billetera/extracto',
    '/pasanaku/grupos/nuevo',
    '/pasanaku/grupos/demo/pedir-cupo',
    '/pasanaku/grupos/demo/invitar',
    '/pasanaku/grupos/demo/retiro',
    '/pasanaku/turnos/permuta',
    '/pasanaku/sorteos/demo/verificar',
    '/pasanaku/grupos/demo/transparencia',
    '/pasanaku/mi-estado',
    '/pasanaku/mi-puntaje',
    '/pasanaku/sorteos/demo/turno',
    '/pasanaku/obligaciones/demo/aportar',
    '/pasanaku/reclamos/nuevo',
    '/pasanaku/escanear',
    '/identidad/dispositivos',
    '/identidad/verificacion-profunda',
    '/identidad/perfil',
    '/identidad/contrasena',
    '/identidad/baja',
    '/soporte/ayuda',
  ];
  const soloSinSesion = ['/portada', '/tour', '/ingreso', '/registro'];
  const publicas = [
    '/identidad/mfa',
    '/identidad/verificacion-basica',
    '/identidad/contrato',
    '/identidad/bienvenida',
    '/registro/camara',
    '/registro/pautas',
    '/registro/prueba-de-vida',
    '/registro/subida',
    '/registro/estado',
    '/pasanaku/unirse/codigo',
  ];
  final rutas = [...privadas, ...soloSinSesion, ...publicas];

  test(
    'todas las rutas inventariadas tienen acceso predeterminado explícito',
    () {
      expect(rutas, hasLength(41));
      for (final ruta in privadas) {
        expect(accesoDe(ruta), AccesoDeRuta.privada, reason: ruta);
      }
      for (final ruta in soloSinSesion) {
        expect(accesoDe(ruta), AccesoDeRuta.soloSinSesion, reason: ruta);
      }
      for (final ruta in publicas) {
        expect(accesoDe(ruta), AccesoDeRuta.publica, reason: ruta);
      }
      expect(accesoDe('/ruta/nueva'), AccesoDeRuta.privada);
    },
  );

  test('cada ruta conserva el acceso previsto con sesión anónima y activa', () {
    for (final ruta in rutas) {
      final tipo = accesoDe(ruta);
      final anonima = decidirRuta(
        estado: EstadoDeSesion.anonima,
        destino: Uri.parse(ruta),
      );
      final activa = decidirRuta(
        estado: EstadoDeSesion.activa,
        destino: Uri.parse(ruta),
      );
      expect(
        anonima,
        tipo == AccesoDeRuta.privada ? startsWith('/ingreso?volver=') : isNull,
        reason: '$ruta con sesión anónima',
      );
      expect(
        activa,
        tipo == AccesoDeRuta.soloSinSesion ? '/billetera/inicio' : isNull,
        reason: '$ruta con sesión activa',
      );
    }
  });

  test('estado desconocido no deja pasar rutas privadas', () {
    expect(
      decidirRuta(
        estado: EstadoDeSesion.desconocida,
        destino: Uri.parse('/billetera/inicio'),
      ),
      '/ingreso',
    );
    expect(
      decidirRuta(
        estado: EstadoDeSesion.desconocida,
        destino: Uri.parse('/identidad/mfa'),
      ),
      isNull,
    );
  });

  test(
    'volver rechaza redirecciones externas y conserva destinos privados',
    () {
      expect(destinoTrasIngresar('https://malo.com'), '/billetera/inicio');
      expect(destinoTrasIngresar('//malo'), '/billetera/inicio');
      expect(destinoTrasIngresar('/pasanaku/mi-estado'), '/pasanaku/mi-estado');
      expect(destinoTrasIngresar('/identidad/mfa'), '/billetera/inicio');
    },
  );
}
