/// Cada ruta registrada de la app, con el flujo al que pertenece y la sesión con la que
/// se abre. `inventario_test.dart` exige que toda ruta de `lib/**/rutas*.dart` esté acá
/// o en [rutasExcluidas] con su motivo: una pantalla nueva sin captura no pasa.
library;

import 'package:aportaya_movil/pantallas/identidad/dominio/cara_del_carril.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/juez_de_pose.dart';

import 'adaptador_por_contrato.dart';

enum SesionDeCaptura { anonima, activa }

class PantallaDeCaptura {
  const PantallaDeCaptura(
    this.flujo,
    this.nombre,
    this.ruta, {
    this.patron,
    this.sesion = SesionDeCaptura.activa,
    this.extra,
    this.estados = EstadoDeCaptura.values,
    this.motivoDeOmision,
    this.saleDesmontando = false,
  });

  final String flujo;
  final String nombre;

  /// La ubicación concreta que se abre (con ids sintéticos).
  final String ruta;

  /// La ruta tal como la declara go_router, si difiere de [ruta].
  final String? patron;
  final SesionDeCaptura sesion;

  /// Lo que el flujo real pasa por `extra` al navegar (la ruta no se puede abrir sin eso).
  final Object? extra;

  /// Los estados que se pueden ejercitar en `flutter test`; los demás se declaran.
  final List<EstadoDeCaptura> estados;
  final String? motivoDeOmision;

  /// La pantalla modifica un provider en su `dispose` (`AltaNotifier.abandonar`): al
  /// salir navegando, Riverpod lo rechaza en debug. Hallazgo del REPORTE; la captura sale
  /// desmontando la app entera, que no lo dispara.
  final bool saleDesmontando;

  String get declarada => patron ?? ruta.split('?').first;
}

// Ids sintéticos: los ejemplos del contrato los aceptan todos (el adaptador matchea por patrón).
const _grupo = 'ed65a0cc-a1e4-425c-8c81-b2908f49b328';
const _sorteo = '22222222-2222-4222-8222-222222222222';
const _obligacion = '33333333-3333-4333-8333-333333333333';
const _codigo =
    '12345678-1234-4234-8234-123456789abc.'
    '0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef';

/// El usuario sintético del JWT de las capturas (`matriz.dart`).
const usuarioDeCaptura = '44444444-4444-4444-8444-444444444444';

const _anonima = SesionDeCaptura.anonima;

/// Con permiso concedido, la pantalla abre el plugin `camera`, que en `flutter test` no
/// tiene dispositivo: `CamaraEnVivoPlugin.iniciar` lanza y `_inicializar` no lo atrapa
/// (hallazgo registrado en el REPORTE, fuera de alcance). Se fotografían los permisos.
const _soloPermisos = [EstadoDeCaptura.vacio, EstadoDeCaptura.error];
const _sinCamara =
    'concedido requiere cámara real: ver goldens H10 con cámara sintética';

const pantallasDeCaptura = <PantallaDeCaptura>[
  // Entrada
  PantallaDeCaptura('entrada', 'portada', '/portada', sesion: _anonima),
  PantallaDeCaptura('entrada', 'tour', '/tour', sesion: _anonima),
  PantallaDeCaptura('entrada', 'ingreso', '/ingreso', sesion: _anonima),
  PantallaDeCaptura('entrada', 'mfa', '/identidad/mfa', sesion: _anonima),
  // Alta y KYC
  PantallaDeCaptura(
    'alta',
    'registro',
    '/registro',
    sesion: _anonima,
    saleDesmontando: true,
  ),
  PantallaDeCaptura('alta', 'pautas', '/registro/pautas', sesion: _anonima),
  PantallaDeCaptura(
    'alta',
    'camara',
    '/registro/camara',
    sesion: _anonima,
    extra: CaraDelCarril.anverso,
    estados: _soloPermisos,
    motivoDeOmision: _sinCamara,
  ),
  PantallaDeCaptura(
    'alta',
    'prueba-de-vida',
    '/registro/prueba-de-vida',
    sesion: _anonima,
    extra: [PoseDeVida.frente, PoseDeVida.izquierda, PoseDeVida.derecha],
    estados: _soloPermisos,
    motivoDeOmision: _sinCamara,
  ),
  PantallaDeCaptura(
    'alta',
    'subida',
    '/registro/subida?usuario=$usuarioDeCaptura',
    sesion: _anonima,
  ),
  PantallaDeCaptura(
    'alta',
    'estado',
    '/registro/estado?usuario=$usuarioDeCaptura',
    sesion: _anonima,
  ),
  PantallaDeCaptura('alta', 'contrato', '/identidad/contrato'),
  PantallaDeCaptura('alta', 'bienvenida', '/identidad/bienvenida'),
  PantallaDeCaptura(
    'alta',
    'verificacion-basica',
    '/identidad/verificacion-basica',
  ),
  PantallaDeCaptura(
    'alta',
    'verificar-identidad',
    '/identidad/verificar',
    saleDesmontando: true,
  ),
  PantallaDeCaptura('alta', 'identidad-requerida', '/identidad/requerida'),
  PantallaDeCaptura('alta', 'verificacion-de-sesion', '/sesion/verificacion'),
  // Billetera
  PantallaDeCaptura('billetera', 'inicio', '/billetera/inicio'),
  PantallaDeCaptura('billetera', 'recargar', '/billetera/recargar'),
  PantallaDeCaptura('billetera', 'retirar', '/billetera/retirar'),
  PantallaDeCaptura('billetera', 'transferir', '/billetera/transferir'),
  PantallaDeCaptura('billetera', 'extracto', '/billetera/extracto'),
  // Grupos
  PantallaDeCaptura('grupos', 'mi-estado', '/pasanaku/mi-estado'),
  PantallaDeCaptura('grupos', 'mi-puntaje', '/pasanaku/mi-puntaje'),
  PantallaDeCaptura('grupos', 'nuevo', '/pasanaku/grupos/nuevo'),
  PantallaDeCaptura(
    'grupos',
    'pedir-cupo',
    '/pasanaku/grupos/$_grupo/pedir-cupo',
    patron: '/pasanaku/grupos/:grupoId/pedir-cupo',
  ),
  PantallaDeCaptura(
    'grupos',
    'invitar',
    '/pasanaku/grupos/$_grupo/invitar',
    patron: '/pasanaku/grupos/:grupoId/invitar',
  ),
  PantallaDeCaptura(
    'grupos',
    'invitar-numero',
    '/pasanaku/grupos/$_grupo/invitar/numero',
    patron: '/pasanaku/grupos/:grupoId/invitar/numero',
  ),
  PantallaDeCaptura(
    'grupos',
    'solicitudes',
    '/pasanaku/grupos/$_grupo/solicitudes',
    patron: '/pasanaku/grupos/:grupoId/solicitudes',
  ),
  PantallaDeCaptura(
    'grupos',
    'retiro',
    '/pasanaku/grupos/$_grupo/retiro',
    patron: '/pasanaku/grupos/:grupoId/retiro',
  ),
  PantallaDeCaptura(
    'grupos',
    'transparencia',
    '/pasanaku/grupos/$_grupo/transparencia',
    patron: '/pasanaku/grupos/:grupoId/transparencia',
  ),
  PantallaDeCaptura('grupos', 'permuta', '/pasanaku/turnos/permuta'),
  PantallaDeCaptura(
    'grupos',
    'sorteo-turno',
    '/pasanaku/sorteos/$_sorteo/turno',
    patron: '/pasanaku/sorteos/:sorteoId/turno',
  ),
  PantallaDeCaptura(
    'grupos',
    'sorteo-verificar',
    '/pasanaku/sorteos/$_sorteo/verificar',
    patron: '/pasanaku/sorteos/:sorteoId/verificar',
  ),
  PantallaDeCaptura(
    'grupos',
    'aportar',
    '/pasanaku/obligaciones/$_obligacion/aportar',
    patron: '/pasanaku/obligaciones/:obligacionId/aportar',
  ),
  PantallaDeCaptura('grupos', 'reclamo-nuevo', '/pasanaku/reclamos/nuevo'),
  PantallaDeCaptura('grupos', 'escanear', '/pasanaku/escanear'),
  PantallaDeCaptura(
    'grupos',
    'unirse',
    '/pasanaku/unirse/$_codigo',
    patron: '/pasanaku/unirse/:codigo',
  ),
  PantallaDeCaptura(
    'grupos',
    'enlace-de-grupo',
    '/g/$_codigo',
    patron: '/g/:codigo',
    sesion: _anonima,
  ),
  // Perfil
  PantallaDeCaptura('perfil', 'perfil', '/identidad/perfil'),
  PantallaDeCaptura('perfil', 'verificacion', '/identidad/perfil/verificacion'),
  PantallaDeCaptura('perfil', 'dispositivos', '/identidad/dispositivos'),
  PantallaDeCaptura(
    'perfil',
    'verificacion-profunda',
    '/identidad/verificacion-profunda',
  ),
  PantallaDeCaptura('perfil', 'contrasena', '/identidad/contrasena'),
  PantallaDeCaptura('perfil', 'baja', '/identidad/baja'),
  // Notificaciones y ayuda
  PantallaDeCaptura('sistema', 'notificaciones', '/notificaciones/bandeja'),
  PantallaDeCaptura('sistema', 'ayuda', '/soporte/ayuda'),
];

/// Rutas registradas que NO se fotografían, con su motivo.
const rutasExcluidas = <String, String>{
  '/identidad':
      'Marcador del shell (rutas.dart): redirige, no tiene pantalla propia',
  '/pasanaku':
      'Marcador del shell (rutas.dart): redirige, no tiene pantalla propia',
};
