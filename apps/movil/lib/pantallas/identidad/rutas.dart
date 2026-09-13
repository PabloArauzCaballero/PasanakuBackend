import 'package:go_router/go_router.dart';

import 'pantalla_baja.dart';
import 'pantalla_bienvenida.dart';
import 'pantalla_contrasena.dart';
import 'pantalla_contrato.dart';
import 'pantalla_dispositivos.dart';
import 'pantalla_mfa.dart';
import 'pantalla_perfil.dart';
import 'pantalla_portada.dart';
import 'pantalla_registro.dart';
import 'pantalla_sesion.dart';
import 'pantalla_verificacion_profunda.dart';

/// Las rutas del dominio identidad — carril M1 (ficha F3). Las once pantallas de
/// `docs/Views/AportaYa-Maqueta.html` §2.1, y **solo** ellas: `rutasIdentidad` tiene
/// once `GoRoute` y ninguna más.
///
/// A eso se suma [rutasDeEntrada], que son otras dos (`/portada` y `/ingreso`) y van
/// aparte a propósito: no cuelgan de `/identidad/` ni viven dentro del shell, porque
/// son lo que se ve **antes** de tener sesión. Por eso el viejo `grep -c "identidad\."`
/// sobre este archivo ahora da 13 y no 11.
final List<RouteBase> rutasIdentidad = [
  GoRoute(
    path: '/identidad',
    name: 'identidad.sesion',
    builder: (context, state) => const PantallaDeSesion(),
  ),
  GoRoute(
    path: '/identidad/bienvenida',
    name: 'identidad.bienvenida',
    builder: (context, state) => const PantallaDeBienvenida(),
  ),
  // La maqueta separa "registro" (datos) de "verificacion-basica" (celular +
  // documento + prueba de vida + cotejo), pero ambas son pasos del mismo asistente
  // de ocho pasos que gobierna `AltaNotifier` (§ pantalla_registro.dart) — dos
  // puertas de entrada a un único estado compartido por Riverpod, no dos flujos
  // independientes. Se registran como dos rutas porque así las nombra la maqueta y
  // así las linkea el resto de la app (por ejemplo, un enlace profundo que retoma
  // justo después de los datos personales).
  GoRoute(
    path: '/identidad/verificacion-basica',
    name: 'identidad.verificacion-basica',
    builder: (context, state) => const PantallaDeRegistro(),
  ),
  GoRoute(
    path: '/identidad/contrato',
    name: 'identidad.contrato',
    builder: (context, state) => const PantallaDeContrato(),
  ),
  GoRoute(
    path: '/identidad/mfa',
    name: 'identidad.mfa',
    builder: (context, state) => const PantallaDeMfa(),
  ),
  GoRoute(
    path: '/identidad/dispositivos',
    name: 'identidad.dispositivos',
    builder: (context, state) => const PantallaDeDispositivos(),
  ),
  GoRoute(
    path: '/identidad/verificacion-profunda',
    name: 'identidad.verificacion-profunda',
    builder: (context, state) => const PantallaDeVerificacionProfunda(),
  ),
  GoRoute(
    path: '/identidad/perfil',
    name: 'identidad.perfil',
    builder: (context, state) => const PantallaDePerfil(),
  ),
  GoRoute(
    path: '/identidad/contrasena',
    name: 'identidad.contrasena',
    builder: (context, state) => const PantallaDeContrasena(),
  ),
  GoRoute(
    path: '/identidad/baja',
    name: 'identidad.baja',
    builder: (context, state) => const PantallaDeBaja(),
  ),
];

/// **Las rutas de entrada, fuera de la barra de pestañas.**
///
/// Portada e ingreso son las dos pantallas que alguien ve *antes* de tener sesión, y
/// por eso no pueden vivir dentro del shell: una barra de pestañas con «Inicio ·
/// Grupos · Movimientos · Perfil» debajo de un formulario de acceso ofrece cuatro
/// destinos a los que todavía no se puede ir. `crearEnrutador` las registra al lado
/// de alianzas y soporte, encima del shell.
///
/// `/registro` está acá y no bajo `/identidad/` por la misma razón, y por una peor:
/// vivía dentro del shell, así que tocar «Crear mi cuenta» montaba la barra de
/// pestañas con Inicio · Grupos · Movimientos · Perfil. La app parecía haber
/// iniciado sesión sola, con una cuenta que todavía no existía.
///
/// `/ingreso` es además el destino del guardia de sesión, que antes apuntaba a
/// `/identidad/ingreso` — una ruta que nunca existió: quien perdía la sesión no
/// llegaba al login sino a «ruta no encontrada».
final List<RouteBase> rutasDeEntrada = [
  GoRoute(
    path: '/portada',
    name: 'identidad.portada',
    builder: (context, state) => const PantallaDePortada(),
  ),
  GoRoute(
    path: '/ingreso',
    name: 'identidad.ingreso',
    builder: (context, state) => const PantallaDeSesion(),
  ),
  GoRoute(
    path: '/registro',
    name: 'identidad.registro',
    builder: (context, state) => const PantallaDeRegistro(),
  ),
];
