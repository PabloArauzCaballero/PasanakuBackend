import 'package:flutter/widgets.dart';

/// Preferencia única para consultar el movimiento reducido del sistema.
bool reducirMovimiento(BuildContext context) =>
    MediaQuery.disableAnimationsOf(context);

/// Duración visual respetuosa con el sistema; conserva el valor normal si no reduce.
Duration duracion(BuildContext context, Duration normal) =>
    reducirMovimiento(context) ? Duration.zero : normal;

/// **El movimiento Flutter de AportaYa, en un solo lugar.** Duraciones, curvas y resortes.
///
/// La app mueve plata de gente que confía: el movimiento tiene que sentirse firme y
/// rápido, nunca juguetón. Nada disparado por la persona pasa de 320 ms; la salida es
/// siempre más corta que la entrada; y nada escala la pantalla entera — los objetos se
/// desplazan y aparecen, no se inflan.
///
/// En Flutter, las pantallas y `diseno_flutter` usan esta clase. `MovimientoDeInterfaz`
/// pertenece a los tokens de otro stack y no es una API de movimiento Flutter; no
/// mantener una segunda copia con otro nombre.
abstract final class Movimiento {
  /// Respuesta al dedo: hover, apretar, cambiar un color de estado.
  static const micro = Duration(milliseconds: 120);

  /// Contracción y expansión discretas de un indicador de estado.
  static const cambioDeEstado = Duration(milliseconds: 150);

  /// ApariciÃ³n de una tilde al validar un campo de formulario.
  static const tildeDeValidez = Duration(milliseconds: 160);

  /// Trazado de la marca de cuenta creada.
  static const celebracionDeAlta = Duration(milliseconds: 360);

  /// ConfirmaciÃ³n visual al guardar una captura.
  static const popDeCaptura = Duration(milliseconds: 200);

  /// Ataque corto del botón hundible antes de que se suelte al resorte.
  static const ataqueDelToque = Duration(milliseconds: 110);

  /// Un elemento que aparece o cambia de forma dentro de la pantalla.
  static const entrada = Duration(milliseconds: 220);

  /// Giro del icono de reintento tras activar la acción.
  static const reintento = Duration(milliseconds: 400);

  /// Una pantalla nueva que llega.
  static const pagina = Duration(milliseconds: 300);

  /// Una pantalla que se va al volver: más corta que la que llega.
  static const paginaDeVuelta = Duration(milliseconds: 240);

  /// Separación entre bloques de la entrada escalonada de portada.
  static const escalonDePortada = Duration(milliseconds: 40);

  /// Duración de cada bloque de portada y su botón.
  static const entradaDePortada = Duration(milliseconds: 220);

  /// Salida de la cubierta de sesión, más corta que su entrada.
  static const saleCandado = Duration(milliseconds: 160);

  /// Secuencia visual de confirmación de una captura (H6).
  static const destelloDeCaptura = Duration(milliseconds: 500);

  /// Cambio legible del número de cuenta regresiva, que actualiza una vez por segundo.
  static const cambioDeCuentaAtras = Duration(milliseconds: 140);

  /// Tres oscilaciones del formulario al rechazar credenciales.
  static const sacudidaDeCredencial = Duration(milliseconds: 300);

  /// Lo que tarda el brillo en cruzar un botón principal.
  static const brillo = Duration(milliseconds: 1100);

  /// Cada cuánto vuelve a cruzar: lo bastante espaciado para no ser un parpadeo.
  static const pausaDelBrillo = Duration(milliseconds: 4200);

  /// Desacelera al llegar: lo que entra (Material «emphasized decelerate»).
  static const llega = Cubic(0.05, 0.7, 0.1, 1);

  /// Acelera al irse: lo que sale.
  static const sale = Cubic(0.3, 0, 0.8, 0.15);

  /// Para lo que va y vuelve dentro del mismo lugar.
  static const pareja = Curves.easeInOutCubic;

  /// El resorte del botón al soltarlo: pasa apenas de su tamaño y se asienta.
  static const resorte = SpringDescription(
    mass: 1,
    stiffness: 520,
    damping: 19,
  );

  /// Cuánto se corre lateralmente una pantalla al entrar, en píxeles lógicos.
  /// Treinta es lo que usa Material para su eje compartido: se lee como dirección
  /// sin que la pantalla viaje.
  static const corrimiento = 30.0;
}
