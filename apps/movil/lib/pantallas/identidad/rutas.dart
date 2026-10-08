import 'package:aportaya_diseno/moviles/transicion_de_marca.dart';
import 'package:flutter/widgets.dart';
import 'package:go_router/go_router.dart';

import 'dominio/capturas_del_expediente.dart';
import 'pantalla_baja.dart';
import 'pantalla_bienvenida.dart';
import 'dominio/juez_de_pose.dart';
import 'pantalla_camara.dart';
import 'pantalla_prueba_de_vida.dart';
import 'pantalla_contrasena.dart';
import 'pantalla_contrato.dart';
import 'pantalla_dispositivos.dart';
import 'pantalla_estado_de_verificacion.dart';
import 'pantalla_mfa.dart';
import 'pantalla_perfil.dart';
import 'pantalla_portada.dart';
import 'pantalla_registro.dart';
import 'pantalla_sesion.dart';
import 'pantalla_subida_del_expediente.dart';
import 'pantalla_tour.dart';
import 'pantalla_verificacion_profunda.dart';

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
  // Las tres salidas de la bienvenida llegan con la transición de marca: la portada
  // se apaga y la pantalla nueva sube apenas al aparecer. `/registro`
  // estaba afuera y se notaba: quien tocaba «Saltar», o «Crear mi cuenta» en la
  // última lámina, caía en el formulario sin que pasara nada — justo el destino que
  // más se usa.
  GoRoute(
    path: '/tour',
    name: 'identidad.tour',
    pageBuilder: (context, state) =>
        _conTransicionDeMarca(state, const PantallaDeTour()),
  ),
  // `?alta=lista` es lo que deja el último paso del alta para que el login pueda
  // decir «tu cuenta quedó creada, ahora entrá». Sin eso, terminar ocho pasos y
  // aparecer en un formulario de acceso se lee como un error.
  GoRoute(
    path: '/ingreso',
    name: 'identidad.ingreso',
    pageBuilder: (context, state) => _conTransicionDeMarca(
      state,
      PantallaDeSesion(
        cuentaRecienCreada: state.uri.queryParameters['alta'] == 'lista',
      ),
    ),
  ),
  GoRoute(
    path: '/registro',
    name: 'identidad.registro',
    pageBuilder: (context, state) =>
        _conTransicionDeMarca(state, const PantallaDeRegistro()),
  ),
  // El visor en vivo del escáner de identidad (CU-02): pantalla completa, sin la
  // barra de pestañas ni la transición de marca de las demás — es una toma de foto, no
  // una transición de producto. Se llega con `context.push<Object?>(..., extra:
  // CaraDelCarril)` y devuelve la `Captura` aceptada, `'prueba'` (atajo de
  // desarrollo) o `null` (cancelado).
  GoRoute(
    path: '/registro/camara',
    name: 'identidad.registro.camara',
    builder: (context, state) =>
        PantallaDeCamara(cara: state.extra! as CaraDelCarril),
  ),
  // La prueba de vida automática (frente, izquierda, derecha): pantalla completa
  // como la cámara. Recibe las poses a pedir y devuelve las capturas logradas.
  GoRoute(
    path: '/registro/prueba-de-vida',
    name: 'identidad.registro.pruebaDeVida',
    builder: (context, state) =>
        PantallaDePruebaDeVida(poses: state.extra! as List<PoseDeVida>),
  ),
  // La subida en lote de las cinco fotos, ya con el `usuarioId` que devolvió el
  // alta — Atlas sube apenas captura; acá se sube recién ahora porque la persona
  // no existe hasta que `POST /usuarios` responde (ver `envio_del_alta.dart`).
  GoRoute(
    path: '/registro/subida',
    name: 'identidad.registro.subida',
    builder: (context, state) => PantallaDeSubidaDelExpediente(
      usuarioId: state.uri.queryParameters['usuario']!,
    ),
  ),
  GoRoute(
    path: '/registro/estado',
    name: 'identidad.registro.estado',
    builder: (context, state) => PantallaDeEstadoDeVerificacion(
      usuarioId: state.uri.queryParameters['usuario']!,
    ),
  ),
  // El contrato es el octavo paso del alta, y el alta pasa **antes** de que exista la
  // cuenta. Vivía dentro del shell, así que el último paso de crear una cuenta se leía
  // con la barra de pestañas debajo y «Perfil» encendido: la app decía que había una
  // sesión —y un perfil— mientras todavía se estaba pidiendo abrir la cuenta.
  GoRoute(
    path: '/identidad/contrato',
    name: 'identidad.contrato',
    builder: (context, state) => const PantallaDeContrato(),
  ),
  // Idem: la bienvenida es lo que se ve al cerrar el alta, cuando todavía no se
  // ingresó.
  GoRoute(
    path: '/identidad/bienvenida',
    name: 'identidad.bienvenida',
    builder: (context, state) => const PantallaDeBienvenida(),
  ),
];

Page<void> _conTransicionDeMarca(GoRouterState state, Widget pantalla) =>
    CustomTransitionPage<void>(
      key: state.pageKey,
      child: pantalla,
      transitionDuration: TransicionDeMarca.duracion,
      reverseTransitionDuration: TransicionDeMarca.duracionDeVuelta,
      transitionsBuilder: TransicionDeMarca.construir,
    );
