/// Lo que la bienvenida avisa antes de empezar el alta: qué hay que tener a mano.
///
/// Sale de los pasos reales del alta (CU-01): el correo se confirma con un código
/// (`PasoCorreo`), el carnet se fotografía de los dos lados (`PasoDocumento`) y la
/// prueba de vida es una foto de la cara. No se promete un tiempo exacto: no está
/// medido.
abstract final class TextosDePortada {
  static const necesitasTitulo = 'Para abrir tu cuenta vas a necesitar';
  static const necesitasCarnet =
      'Tu carnet de identidad, para sacarle foto de los dos lados';
  static const necesitasContacto =
      'Tu celular y un correo donde te llegue el código';
  static const necesitasLuz =
      'Unos minutos y buena luz para la foto de tu cara';
}
