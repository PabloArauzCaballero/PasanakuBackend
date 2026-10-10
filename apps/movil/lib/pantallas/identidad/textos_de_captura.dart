/// Los textos del carrusel de capturas y de la cámara del escáner (CU-02),
/// comportamiento copiado de Atlas. Aparte de `textos_del_alta.dart` por el mismo
/// motivo que ese archivo existe: 200 líneas por archivo.
class TextosDeCaptura {
  TextosDeCaptura._();

  static String contador(int listas) => 'Tus capturas ($listas de 5)';
  static const tomarFoto = 'Tomar foto';
  static const repetirFoto = 'Repetir la foto';
  static const listo = 'Lista';
  static const pendiente = 'Pendiente';
  static const usarCarnetDePrueba = 'Usar el carnet de prueba';

  static const antesDeEscanearTitulo = 'Antes de escanear';
  static const antesDeEscanearRequisitos = [
    'Mesa lisa y oscura',
    'Buena luz',
    'Sin reflejos',
  ];
  static const abrirElEscaner = 'Abrir el escáner';

  static const necesitamosTuCamara = 'Necesitamos tu cámara';
  static const permitirCamara = 'Permitir cámara';
  static const permisoBloqueado =
      'El permiso está bloqueado. Habilitalo desde los ajustes del teléfono.';
  static const abrirAjustes = 'Abrir ajustes';
  static const cancelar = 'Cancelar';

  static const consejosTitulo = 'Consejos para la foto';
  static const consejosSubtitulo = 'Luz, reflejos, nitidez y ángulo';
  static const entendido = 'Entendido';

  static const enviarDocumento = 'Enviar documento';

  // Prueba de vida automática: tres poses seguidas, la foto se saca sola.
  static String deTres(int i, int total) => '$i de $total';
  static const seSacaSola =
      'La foto se saca sola cuando estés en posición. No hace falta tocar nada.';
  static const preparando = 'Buscando tu cara…';
  static const fotoFallida = 'No pudimos sacar la foto. Probá de nuevo.';
  static const escaneoFallido =
      'El escáner no pudo leer el carnet. Probá de nuevo o usá la cámara.';

  /// Lo que dice el riel debajo de cada cara: corto, porque son cinco en una fila.
  static String corto(String valorApi) => switch (valorApi) {
    'ANVERSO' => 'Frente',
    'REVERSO' => 'Dorso',
    'SELFIE' => 'Selfie',
    'PERFIL_IZQUIERDO' => 'Perfil izq.',
    _ => 'Perfil der.',
  };

  /// La pista sobre el marco vacío de cada cara.
  static String pista(String valorApi) => switch (valorApi) {
    'ANVERSO' ||
    'REVERSO' => 'El carnet entero dentro del marco, sobre una mesa oscura',
    'SELFIE' => 'De frente, con la cara centrada y buena luz',
    'PERFIL_IZQUIERDO' => 'Gira la cabeza a tu izquierda, no el teléfono',
    _ => 'Gira la cabeza a tu derecha, no el teléfono',
  };

  static String irA(String corto, bool lista) =>
      '$corto, ${lista ? 'lista' : 'pendiente'}. Ir a esta captura';
}
