import 'dominio/capturas_del_expediente.dart';

/// Los textos de la pantalla de subida del expediente (CU-02), estados calcados de
/// Atlas: «Subiendo…», «está tardando», «reintentar» o «repetir».
class TextosDeSubida {
  TextosDeSubida._();

  static String subiendo(CaraDelCarril cara) =>
      'Subiendo ${_nombreCorto(cara)}…';
  static const detalle = 'Se guarda cifrada en tu expediente.';
  static const lenta =
      'Está tardando más de lo normal. Puedes esperar o cancelar…';
  static const reintentar = 'Reintentar';
  static const repetirLaFoto = 'Repetir la foto';
  static const cancelarLaSubida = 'Cancelar la subida';
  static const todoListo = 'Documento enviado';
  static const tituloSubiendo = 'Enviando tus documentos';
  static const tituloEnviados = 'Tus documentos están enviados';
  static const continuar = 'Continuar';

  static String _nombreCorto(CaraDelCarril cara) => switch (cara) {
    CaraDelCarril.anverso => 'el anverso',
    CaraDelCarril.reverso => 'el reverso',
    CaraDelCarril.selfie => 'tu selfie',
    CaraDelCarril.perfilIzquierdo => 'tu perfil izquierdo',
    CaraDelCarril.perfilDerecho => 'tu perfil derecho',
  };
}
