part of 'rutas.dart';

/// Retiro, permuta, verificación del sorteo, transparencia, estado, puntaje, turno, aporte y reclamo.
/// Vive aparte solo por tamaño: `rutasPasanaku` la inserta en el mismo lugar, así el orden no cambia.
final List<RouteBase> _rutasDeOperacion = [
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
    builder: (context, state) => PantallaMiEstadoDeLaSesion(
      participanteId: state.uri.queryParameters['participante'],
      usuarioId: state.uri.queryParameters['usuario'],
      grupoId: state.uri.queryParameters['grupo'],
      nombreDelGrupo: state.uri.queryParameters['nombre'],
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
