import 'package:flutter/widgets.dart';
import 'package:go_router/go_router.dart';

import 'pantalla_aportar.dart';
import 'pantalla_crear_grupo.dart';
import 'pantalla_enlace_del_grupo.dart';
import 'pantalla_invitar.dart';
import 'pantalla_solicitudes_del_grupo.dart';
import 'pantalla_del_grupo.dart';
import 'pantalla_firmar_reglamento.dart';
import 'pantalla_sorteo.dart';
import 'pantalla_unirse_a_un_grupo.dart';
import 'pantalla_unirse_al_grupo.dart';
import 'pantalla_mi_estado.dart';
import 'pantalla_mi_puntaje.dart';
import 'pantalla_pedir_cupo.dart';
import 'pantalla_permuta.dart';
import 'pantalla_reclamo_pendiente.dart';
import 'pantalla_retiro.dart';
import 'pantalla_subasta.dart';
import 'pantalla_subastar_cupo.dart';
import 'pantalla_subastas.dart';
import 'pantalla_transparencia.dart';
import 'pantalla_traspaso.dart';
import 'pantalla_turno.dart';
import 'pantalla_escanear.dart';
import 'pantalla_unirse.dart';
import 'pantalla_verificar_sorteo.dart';
import 'pantalla_detalle_miembro.dart';

part 'rutas_operacion.dart';

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
  // «Invitar» es el enlace del grupo con su QR (2026-10-19): sirve para cualquiera y el
  // organizador aprueba cada pedido. Invitar a un número en particular sigue abajo.
  GoRoute(
    path: '/pasanaku/grupos/:grupoId/invitar',
    name: 'pasanaku.invitar',
    builder: (context, state) => PantallaEnlaceDelGrupo(
      grupoId: state.pathParameters['grupoId']!,
      nombre: state.uri.queryParameters['nombre'],
    ),
  ),
  GoRoute(
    path: '/pasanaku/grupos/:grupoId/invitar/numero',
    name: 'pasanaku.invitarANumero',
    builder: (context, state) =>
        PantallaInvitar(grupoId: state.pathParameters['grupoId']!),
  ),
  GoRoute(
    path: '/pasanaku/grupos/:grupoId/solicitudes',
    name: 'pasanaku.solicitudes',
    builder: (context, state) =>
        PantallaSolicitudesDelGrupo(grupoId: state.pathParameters['grupoId']!),
  ),
  GoRoute(
    path: '/pasanaku/grupos/:grupoId/reglamento',
    name: 'pasanaku.firmarReglamento',
    builder: (context, state) =>
        PantallaFirmarReglamento(grupoId: state.pathParameters['grupoId']!),
  ),
  GoRoute(
    path: '/pasanaku/grupos/:grupoId/sorteo',
    name: 'pasanaku.sorteo',
    builder: (context, state) =>
        PantallaSorteo(grupoId: state.pathParameters['grupoId']!),
  ),
  GoRoute(
    path: '/pasanaku/grupos/:grupoId',
    name: 'pasanaku.grupo',
    builder: (context, state) => PantallaDelGrupo(
      key: ValueKey(state.pathParameters['grupoId']),
      grupoId: state.pathParameters['grupoId']!,
      nombre: state.uri.queryParameters['nombre'],
      participanteId: state.uri.queryParameters['participante'],
      usuarioId: state.uri.queryParameters['usuario'],
    ),
  ),
  GoRoute(
    path: '/pasanaku/grupos/:grupoId/integrante/:cupoId',
    name: 'pasanaku.integrante',
    builder: (context, state) {
      final q = state.uri.queryParameters;
      return PantallaDetalleMiembro(
        grupoId: state.pathParameters['grupoId']!,
        cupoId: state.pathParameters['cupoId']!,
        nombre: q['nombre'] ?? '',
        numero: int.tryParse(q['numero'] ?? '1') ?? 1,
        turno: q['turno'] != null ? int.tryParse(q['turno']!) : null,
        esOrganizador: q['organizador'] == 'true',
        soyYo: q['soyYo'] == 'true',
        estado: q['estado'] ?? 'FIRMADO',
        nombreGrupo: q['nombreGrupo'],
        usuarioId: q['usuarioId'],
      );
    },
  ),
  GoRoute(
    path: '/pasanaku/unirse-a-un-grupo',
    name: 'pasanaku.unirseAUnGrupo',
    builder: (context, state) => const PantallaUnirseAUnGrupo(),
  ),
  // El enlace del grupo: `https://<app web>/g/<código>` (App Links) o el QR. Con sesión:
  // sin ella, el ingreso vuelve acá.
  GoRoute(
    path: '/g/:codigo',
    name: 'pasanaku.unirseAlGrupo',
    builder: (context, state) {
      final codigo = state.pathParameters['codigo']!;
      return PantallaUnirseAlGrupo(key: ValueKey(codigo), codigo: codigo);
    },
  ),
  ..._rutasDeOperacion,
];

/// El enlace llega también antes de iniciar sesión, por eso vive fuera del shell.
final List<RouteBase> rutasDeInvitacion = [
  GoRoute(
    path: '/pasanaku/escanear',
    name: 'pasanaku.escanear',
    builder: (context, state) => const PantallaEscanear(),
  ),
  GoRoute(
    path: '/pasanaku/unirse/:codigo',
    name: 'pasanaku.unirse',
    // La clave por código: con la app ya abierta en una invitación, un segundo enlace llega a
    // la MISMA ruta con otro parámetro y, sin clave, Flutter reutilizaba el estado y seguía
    // mostrando la invitación anterior (hallazgo del carril H en emulador).
    builder: (context, state) {
      final codigo = state.pathParameters['codigo']!;
      return PantallaUnirse(key: ValueKey(codigo), codigo: codigo);
    },
  ),
];
