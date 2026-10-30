/// Los textos de las arenas, en voz de AportaYa.
///
/// La regla que manda acá es la honestidad: la arena es **consecuencia** del puntaje de
/// reputación, no algo que se compra. Los factores que se nombran son los del modelo v1
/// vigente (`sql/60_semillas/16-reputacion-y-scoring.sql`, tabla `peso_factor`):
/// puntualidad, ciclos completados y antigüedad suman; la mora y los incumplimientos
/// firmes restan. Ningún texto promete subir por aportar más plata.
abstract final class TextosDeArenas {
  static const titulo = 'Tus arenas';
  static const tuArena = 'Tu arena';
  static const verCamino = 'Ver el camino de arenas';
  static const cargando = 'Cargando las arenas…';

  static const comoSeSube =
      'Tu arena sale de tu puntaje de reputación. Sube cuando aportás a tiempo, '
      'terminás ciclos y sumás antigüedad; baja con la mora y los incumplimientos '
      'firmes. No se compra: aportar montos más grandes no te sube de arena.';

  static const recienEmpezas =
      'Arrancás en la arena del puntaje base: es el punto de partida, no una sanción.';

  static const tituloSinArenas = 'Todavía no hay arenas';
  static const sinArenas =
      'Las arenas todavía no están publicadas. Tu puntaje sigue contando igual; '
      'volvé a mirar más tarde.';

  static const llegasteArriba =
      'Estás en la arena más alta. Cuidarla depende de seguir cumpliendo.';

  static String faltanPuntos(String puntos, String arena) =>
      'Te faltan $puntos puntos para $arena';

  static String avanceHacia(String arena) => 'Avance hacia la arena $arena';
  static const avanceEnLaUltima = 'Avance dentro de la arena más alta';

  static String tuPuntaje(String puntos) => 'Tu arena · $puntos puntos';

  static String desde(String puntos) => 'Desde $puntos puntos';
  static String alcanzada(String puntos) =>
      'Ya la pasaste · desde $puntos puntos';
  static String seAbreCon(String puntos) => 'Se abre con $puntos puntos';

  static String beneficio(int cupos, String aporte) =>
      'Grupos de hasta $cupos cupos y aportes de hasta $aporte';

  static String etiquetaActual(String arena, String puntos) =>
      'Arena $arena, la tuya, desde $puntos puntos';
  static String etiquetaAlcanzada(String arena, String puntos) =>
      'Arena $arena, alcanzada, desde $puntos puntos';
  static String etiquetaBloqueada(String arena, String puntos) =>
      'Arena $arena, bloqueada, se abre con $puntos puntos';
}
