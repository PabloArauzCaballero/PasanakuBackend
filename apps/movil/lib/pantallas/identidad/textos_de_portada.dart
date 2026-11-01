/// Lo que la bienvenida avisa antes de empezar el alta: qué te van a pedir.
///
/// Sale de los pasos reales del alta (CU-01): el correo se confirma con un código
/// (`PasoCorreo`), el carnet se fotografía de los dos lados (`PasoDocumento`) y la
/// prueba de vida es una foto de la cara. El carnet **se puede dejar para después**
/// («Saltar por ahora», 2026-10-17): decir que es obligatorio para abrir la cuenta
/// era falso y espantaba a quien no lo tenía a mano. No se promete un tiempo exacto:
/// no está medido.
abstract final class TextosDePortada {
  static const necesitasTitulo = 'Qué te vamos a pedir';
  static const necesitasContacto =
      'Tu celular y un correo, para mandarte un código';
  static const necesitasCarnet =
      'Tu carnet de los dos lados y una foto de tu cara, con buena luz';
  static const necesitasDespues =
      'Si no tenés el carnet a mano, podés abrir la cuenta y verificarte después';
}
