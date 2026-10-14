// Los textos del carrusel de capturas y de la cámara del escáner (CU-02),
// comportamiento copiado de Atlas. Aparte de `textos_del_alta.dart` por el mismo
// motivo que ese archivo existe: 200 líneas por archivo.
import 'dominio/capturas_del_expediente.dart';
import 'dominio/juez_de_pose.dart';

class TextosDeCaptura {
  TextosDeCaptura._();

  static String contador(int listas) => 'Tus capturas ($listas de 5)';
  static const tomarFoto = 'Tomar foto';
  static String repetirFotoDe(CaraDelCarril cara) => switch (cara) {
    CaraDelCarril.anverso => 'Repetir foto del anverso',
    CaraDelCarril.reverso => 'Repetir foto del reverso',
    CaraDelCarril.selfie => 'Repetir foto de frente',
    CaraDelCarril.perfilIzquierdo => 'Repetir foto del perfil izquierdo',
    CaraDelCarril.perfilDerecho => 'Repetir foto del perfil derecho',
  };
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
  static const soloParaTuPrueba = 'Solo se usa para tu prueba de vida.';
  static const privacidadDeVida =
      'La foto queda asociada únicamente a tu expediente. No accedemos a tu galería.';
  static const permitirCamara = 'Permitir cámara';
  static const permisoBloqueado =
      'Abrí los ajustes del teléfono para permitirla.';
  static const abrirAjustes = 'Abrir ajustes';
  static const cancelar = 'Cancelar';
  static const tomarAhora = 'Tomar ahora';
  static const continuar = 'Continuar';

  static const consejosTitulo = 'Consejos para la foto';
  static const consejosSubtitulo = 'Luz, reflejos, nitidez y ángulo';
  static const entendido = 'Entendido';

  static const enviarDocumento = 'Enviar documento';

  // Prueba de vida automática: tres poses seguidas, la foto se saca sola.
  static String deTres(int i, int total) => '$i de $total';
  static String fotoDeVida(int i, int total) => 'FOTO $i DE $total';
  static String tituloDePose(PoseDeVida pose) => switch (pose) {
    PoseDeVida.frente => 'Mirá de frente',
    PoseDeVida.izquierda => 'Girá a tu izquierda',
    PoseDeVida.derecha => 'Girá a tu derecha',
  };
  static String instruccionDePose(PoseDeVida pose) => switch (pose) {
    PoseDeVida.frente => 'Poné tu cara dentro del óvalo.',
    PoseDeVida.izquierda => 'Despacio, hasta que se vea tu oreja derecha.',
    PoseDeVida.derecha => 'Despacio, hasta que se vea tu oreja izquierda.',
  };
  static const quietoFoto = 'Quedate quieto: la foto sale sola.';
  static String instruccionDuranteCuenta(PoseDeVida pose) =>
      '${instruccionDePose(pose)} $quietoFoto';
  static const fotoTomada = '¡Foto tomada!';
  static const seguirSiguiente = 'Seguí la siguiente indicación.';
  static const vidaLista = '¡Listo! Tus tres fotos están';
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
    'SELFIE' => 'De frente',
    'PERFIL_IZQUIERDO' => 'Perfil izq.',
    _ => 'Perfil der.',
  };

  /// La pista sobre el marco vacío de cada cara.
  static String pista(String valorApi) => switch (valorApi) {
    'ANVERSO' ||
    'REVERSO' => 'El carnet entero dentro del marco, sobre una mesa oscura',
    'SELFIE' => 'Foto de frente, con la cara centrada y buena luz',
    'PERFIL_IZQUIERDO' => 'Gira la cabeza a tu izquierda, no el teléfono',
    _ => 'Gira la cabeza a tu derecha, no el teléfono',
  };

  static String irA(String corto, bool lista) =>
      '$corto, ${lista ? 'lista' : 'pendiente'}. Ir a esta captura';
}
