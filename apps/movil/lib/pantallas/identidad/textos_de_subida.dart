import 'dominio/capturas_del_expediente.dart';

/// Los textos de la pantalla de subida del expediente (CU-02), estados calcados de
/// Atlas: «Subiendo…», «está tardando», «reintentar» o «repetir».
class TextosDeSubida {
  TextosDeSubida._();

  static String subiendo(CaraDelCarril cara) =>
      'Subiendo ${_nombreCorto(cara)}…';
  static const detalle = 'Se guarda cifrada en tu expediente.';
  static const lenta =
      'Está tardando más de lo normal. Podés esperar o cancelar y volver a intentar.';
  static const reintentar = 'Reintentar';
  static String repetirFotoDe(CaraDelCarril cara) => switch (cara) {
    CaraDelCarril.anverso => 'Repetir foto del anverso',
    CaraDelCarril.reverso => 'Repetir foto del reverso',
    CaraDelCarril.selfie => 'Repetir foto de frente',
    CaraDelCarril.perfilIzquierdo => 'Repetir foto del perfil izquierdo',
    CaraDelCarril.perfilDerecho => 'Repetir foto del perfil derecho',
  };
  static const cancelarLaSubida = 'Cancelar la subida';
  static const todoListo = 'Documento enviado';
  static const tituloSubiendo = 'Enviando tus documentos';
  static const tituloEnviados = 'Tus documentos están enviados';
  static const continuar = 'Continuar';

  static String _nombreCorto(CaraDelCarril cara) => switch (cara) {
    CaraDelCarril.anverso => 'el anverso',
    CaraDelCarril.reverso => 'el reverso',
    CaraDelCarril.selfie => 'tu foto de frente',
    CaraDelCarril.perfilIzquierdo => 'tu perfil izquierdo',
    CaraDelCarril.perfilDerecho => 'tu perfil derecho',
  };
}
