/// Los textos de las etapas, en voz de AportaYa.
///
/// La regla que manda acá es la honestidad: la etapa es **consecuencia** del puntaje de
/// reputación, no algo que se compra. Los factores que se nombran son los del modelo v1
/// vigente (`sql/60_semillas/16-reputacion-y-scoring.sql`, tabla `peso_factor`):
/// puntualidad, ciclos completados y antigüedad suman; la mora y los incumplimientos
/// firmes restan. Ningún texto promete subir por aportar más plata.
abstract final class TextosDeEtapas {
  static const titulo = 'Tus etapas';
  static const tuEtapa = 'Tu etapa';
  static const verCamino = 'Ver el camino de etapas';
  static const cargando = 'Cargando las etapas…';

  static const comoSeSube =
      'Tu etapa sale de tu puntaje de reputación. Sube cuando aportás a tiempo, '
      'terminás ciclos y sumás antigüedad; baja con la mora y los incumplimientos '
      'firmes. No se compra: aportar montos más grandes no te sube de etapa.';

  static const recienEmpezas =
      'Arrancás en la etapa del puntaje base: es el punto de partida, no una sanción.';

  static const tituloSinEtapas = 'Todavía no hay etapas';
  static const sinEtapas =
      'Las etapas todavía no están publicadas. Tu puntaje sigue contando igual; '
      'volvé a mirar más tarde.';

  static const llegasteArriba =
      'Estás en la etapa más alta. Cuidarla depende de seguir cumpliendo.';

  /// Lo que se lee de una etapa (H1.S6): «Etapa 2 · En marcha»; la de abajo del
  /// puntaje base no lleva número, porque no es un escalón sino un tramo para
  /// recuperarse: «En recuperación». El servidor manda el nombre corto («En marcha»);
  /// si algún día lo manda ya rotulado, no se repite el prefijo.
  static String rotulo(int orden, String nombre) =>
      orden <= 0 || nombre.startsWith('Etapa ')
      ? nombre
      : 'Etapa $orden · $nombre';

  /// Lo mismo para un lector de pantalla: con coma en vez del punto medio, que se
  /// lee como «punto».
  static String rotuloAccesible(int orden, String nombre) =>
      rotulo(orden, nombre).replaceFirst(' · ', ', ');

  static String faltanPuntos(String puntos, String etapa) =>
      'Te faltan $puntos puntos para $etapa';

  static String avanceHacia(String etapa) => 'Avance hacia $etapa';
  static const avanceEnLaUltima = 'Avance dentro de la etapa más alta';

  static String tuPuntaje(String puntos) => 'Tu etapa · $puntos puntos';

  static String desde(String puntos) => 'Desde $puntos puntos';
  static String alcanzada(String puntos) =>
      'Ya la pasaste · desde $puntos puntos';
  static String seAbreCon(String puntos) => 'Llegás con $puntos puntos';

  static String beneficio(int cupos, String aporte) =>
      'Grupos de hasta $cupos cupos y aportes de hasta $aporte';

  static String etiquetaActual(String etapa, String puntos) =>
      '$etapa, la tuya, desde $puntos puntos';
  static String etiquetaAlcanzada(String etapa, String puntos) =>
      '$etapa, ya la pasaste, desde $puntos puntos';
  static String etiquetaBloqueada(String etapa, String puntos) =>
      '$etapa, todavía por delante, llegás con $puntos puntos';
}
