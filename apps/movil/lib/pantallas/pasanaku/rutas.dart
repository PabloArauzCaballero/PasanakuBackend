import 'package:go_router/go_router.dart';

import 'pantalla_aporte_no_disponible.dart';
import 'pantalla_accion_personal_no_disponible.dart';
import 'pantalla_crear_grupo.dart';
import 'pantalla_invitar.dart';
import 'pantalla_mi_estado.dart';
import 'pantalla_mi_puntaje.dart';
import 'pantalla_pedir_cupo.dart';
import 'pantalla_reclamo_pendiente.dart';
import 'pantalla_transparencia.dart';
import 'pantalla_turno.dart';
import 'pantalla_unirse.dart';
import 'pantalla_verificar_sorteo.dart';
import 'textos.dart';

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
    // Un ID de organizador recibido por URL no demuestra titularidad.
    builder: (context, state) => state.uri.hasQuery
        ? const PantallaAccionPersonalNoDisponible(
            titulo: TextosPasanaku.tituloCrearGrupo,
          )
        : const PantallaCrearGrupo(),
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
    // No hay contrato que derive la participación propia desde la sesión.
    builder: (context, state) => const PantallaAccionPersonalNoDisponible(
      titulo: TextosPasanaku.tituloRetiro,
    ),
  ),
  GoRoute(
    path: '/pasanaku/turnos/permuta',
    name: 'pasanaku.permuta',
    // Los IDs de turnos y contraparte de un enlace no prueban autorización.
    builder: (context, state) => const PantallaAccionPersonalNoDisponible(
      titulo: TextosPasanaku.tituloPermuta,
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
    // Un enlace no prueba la titularidad de la participación ni del usuario.
    redirect: (context, state) => state.uri.hasQuery ? state.uri.path : null,
    builder: (context, state) =>
        const PantallaMiEstado(participanteId: '', usuarioId: ''),
  ),
  GoRoute(
    path: '/pasanaku/mi-puntaje',
    name: 'pasanaku.miPuntaje',
    redirect: (context, state) => state.uri.hasQuery ? state.uri.path : null,
    builder: (context, state) => const PantallaMiPuntaje(usuarioId: ''),
  ),
  GoRoute(
    path: '/pasanaku/sorteos/:sorteoId/turno',
    name: 'pasanaku.turno',
    // El turno personal y el actual no están en el contrato publicado. Los
    // parámetros de un enlace no son una fuente confiable para mostrarlos.
    redirect: (context, state) => state.uri.hasQuery ? state.uri.path : null,
    builder: (context, state) =>
        PantallaTurno(sorteoId: state.pathParameters['sorteoId']!),
  ),
  GoRoute(
    path: '/pasanaku/obligaciones/:obligacionId/aportar',
    name: 'pasanaku.aportar',
    // Un importe de query no es una obligación verificada y además queda en
    // historiales/logs. Hasta tener GET autenticado, la ruta falla cerrado.
    redirect: (context, state) => state.uri.hasQuery ? state.uri.path : null,
    builder: (context, state) => const PantallaAporteNoDisponible(),
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
