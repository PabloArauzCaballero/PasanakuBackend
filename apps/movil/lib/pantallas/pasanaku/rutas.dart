import 'package:go_router/go_router.dart';

import 'pantalla_aportar.dart';
import 'pantalla_crear_grupo.dart';
import 'pantalla_invitar.dart';
import 'pantalla_mi_estado.dart';
import 'pantalla_mi_puntaje.dart';
import 'pantalla_pedir_cupo.dart';
import 'pantalla_permuta.dart';
import 'pantalla_reclamo_pendiente.dart';
import 'pantalla_retiro.dart';
import 'pantalla_transparencia.dart';
import 'pantalla_turno.dart';
import 'pantalla_unirse.dart';
import 'pantalla_verificar_sorteo.dart';

/// Las rutas del dominio pasanaku. Sigue el mismo patrón que `billetera` (M2) e
/// `identidad` (M1): un archivo por pantalla, todo registrado acá, nada en el
/// shell. Las de más carga de datos (grupo, turno, entrega, aporte con lectura
/// propia) no tienen `GET` en ningún contrato real todavía — quedan documentadas
/// como hueco en `planes/informes/carril-M3.md` y no están acá porque no hay nada
/// real que mostrar sin inventar la forma de una respuesta (regla cero).
final List<RouteBase> rutasPasanaku = [
  GoRoute(
    path: '/pasanaku/grupos/nuevo',
    name: 'pasanaku.crearGrupo',
    builder: (context, state) => PantallaCrearGrupo(
      organizadorId: state.uri.queryParameters['organizador'],
    ),
  ),
  GoRoute(
    path: '/pasanaku/grupos/:grupoId/pedir-cupo',
    name: 'pasanaku.pedirCupo',
    builder: (context, state) =>
        PantallaPedirCupo(grupoId: state.pathParameters['grupoId']!),
  ),
  GoRoute(
    path: '/pasanaku/grupos/:grupoId/invitar',
    name: 'pasanaku.invitar',
    builder: (context, state) =>
        PantallaInvitar(grupoId: state.pathParameters['grupoId']!),
  ),
  GoRoute(
    path: '/pasanaku/grupos/:grupoId/retiro',
    name: 'pasanaku.retiro',
    builder: (context, state) => PantallaRetiro(
      grupoId: state.pathParameters['grupoId']!,
      participanteId: state.uri.queryParameters['participante'] ?? '',
    ),
  ),
  GoRoute(
    path: '/pasanaku/turnos/permuta',
    name: 'pasanaku.permuta',
    builder: (context, state) => PantallaPermuta(
      turnoOrigenId: state.uri.queryParameters['origen'] ?? '',
      turnoDestinoId: state.uri.queryParameters['destino'] ?? '',
      contraparteId: state.uri.queryParameters['contraparte'] ?? '',
    ),
  ),
  GoRoute(
    path: '/pasanaku/sorteos/:sorteoId/verificar',
    name: 'pasanaku.verificarSorteo',
    builder: (context, state) =>
        PantallaVerificarSorteo(sorteoId: state.pathParameters['sorteoId']!),
  ),
  GoRoute(
    path: '/pasanaku/grupos/:grupoId/transparencia',
    name: 'pasanaku.transparencia',
    builder: (context, state) =>
        PantallaTransparencia(grupoId: state.pathParameters['grupoId']!),
  ),
  GoRoute(
    path: '/pasanaku/mi-estado',
    name: 'pasanaku.miEstado',
    builder: (context, state) => PantallaMiEstado(
      participanteId: state.uri.queryParameters['participante'] ?? '',
      usuarioId: state.uri.queryParameters['usuario'] ?? '',
    ),
  ),
  GoRoute(
    path: '/pasanaku/mi-puntaje',
    name: 'pasanaku.miPuntaje',
    builder: (context, state) => PantallaMiPuntaje(
      usuarioId: state.uri.queryParameters['usuario'] ?? '',
    ),
  ),
  GoRoute(
    path: '/pasanaku/sorteos/:sorteoId/turno',
    name: 'pasanaku.turno',
    builder: (context, state) {
      final q = state.uri.queryParameters;
      return PantallaTurno(
        sorteoId: state.pathParameters['sorteoId']!,
        total: int.parse(q['total'] ?? '0'),
        miTurno: int.parse(q['mio'] ?? '0'),
        turnoActual: int.parse(q['actual'] ?? '0'),
      );
    },
  ),
  GoRoute(
    path: '/pasanaku/obligaciones/:obligacionId/aportar',
    name: 'pasanaku.aportar',
    builder: (context, state) => PantallaAportar(
      obligacionId: state.pathParameters['obligacionId']!,
      montoDelPeriodo: state.uri.queryParameters['monto'] ?? '0.00',
    ),
  ),
  GoRoute(
    path: '/pasanaku/reclamos/nuevo',
    name: 'pasanaku.reclamo',
    // CU-52/53: hueco de contrato declarado en el informe — `cumplimiento`
    // reserva /reclamos pero no publica ninguna operación bajo ese prefijo.
    builder: (context, state) => const PantallaReclamoPendiente(),
  ),
];

/// El enlace llega también antes de iniciar sesión, por eso vive fuera del shell.
final List<RouteBase> rutasDeInvitacion = [
  GoRoute(
    path: '/pasanaku/unirse/:codigo',
    name: 'pasanaku.unirse',
    builder: (context, state) =>
        PantallaUnirse(codigo: state.pathParameters['codigo']!),
  ),
];
