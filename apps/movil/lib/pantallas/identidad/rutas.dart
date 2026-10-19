import 'package:go_router/go_router.dart';

import 'pantalla_baja.dart';
import 'pantalla_contrasena.dart';
import 'pantalla_completar_kyc_de_sesion.dart';
import 'pantalla_dispositivos.dart';
import 'pantalla_perfil.dart';
import 'pantalla_registro.dart';
import 'pantalla_sesion.dart';
import 'pantalla_verificacion_profunda.dart';

export 'rutas_de_entrada.dart';

/// Las rutas del dominio identidad — carril M1 (ficha F3). Las once pantallas de
/// `docs/Views/AportaYa-Maqueta.html` §2.1 siguen estando todas; lo que cambió es
/// **dónde** vive cada una.
///
/// [rutasIdentidad] son las que van **dentro** del shell: pantallas de alguien que ya
/// tiene sesión y que por lo tanto pueden tener debajo la barra de Inicio · Grupos ·
/// Movimientos · Perfil.
///
/// [rutasDeEntrada] son las que van **encima** del shell, sin barra de pestañas,
/// porque se ven antes de tener sesión: portada, tour, ingreso, alta, contrato y
/// bienvenida.
final List<RouteBase> rutasIdentidad = [
  GoRoute(
    path: '/identidad',
    name: 'identidad.sesion',
    builder: (context, state) => const PantallaDeSesion(),
  ),
  // La maqueta separa "registro" (datos) de "verificacion-basica" (celular +
  // documento + prueba de vida + cotejo), pero ambas son pasos del mismo asistente
  // de seis pasos que gobierna `AltaNotifier` (§ pantalla_registro.dart) — dos
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
    path: '/identidad/perfil/verificacion',
    name: 'identidad.perfil.verificacion',
    builder: (context, state) => const PantallaCompletarKycDeSesion(),
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
