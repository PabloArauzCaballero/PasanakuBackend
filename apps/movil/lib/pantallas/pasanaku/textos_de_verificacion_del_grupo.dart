/// Las entradas y etiquetas de las pantallas de verificación y reclamo del grupo
/// (`IntroDePantalla`, `FilaDeDato`; ADR-053). Antes los datos iban corridos
/// («Hash recomputado: 9f2c…», «Método: …») y la causa de una cadena rota salía como el
/// nombre del enum.
abstract final class TextosDeVerificacionDelGrupo {
  static const sorteoTitulo = 'Comprobá el sorteo vos mismo';
  static const datosParaReproducir = 'Datos para reproducirlo';
  static const hashRecomputado = 'Hash recomputado (con la semilla revelada)';
  static const metodo = 'Método';
  static const ordenReproducido = 'Orden reproducido';
  static const ordenCoincide = 'El orden coincide';
  static String ordenNoCoincide(int? desde) => desde == null
      ? 'El orden no coincide'
      : 'El orden no coincide desde el cupo $desde';

  static const transparenciaTitulo = 'El registro del grupo, sellado';
  static const transparenciaExplicacion =
      'Los registros del grupo se sellan en bloques encadenados. Acá se comprueba si la cadena está entera.';
  static const bloquesVerificados = 'Bloques verificados';
  static const fallaEn = 'Dónde falla';
  static String componente(String nombre) => switch (nombre) {
    'HASH_CONTENIDO' => 'El contenido de un bloque',
    'HASH_ANTERIOR' => 'El enlace con el bloque anterior',
    'HASH_BLOQUE' => 'La huella del bloque',
    'SECUENCIA' => 'El orden de los bloques',
    _ => 'Un bloque',
  };
  static String desdeBloque(int bloque, String componente) =>
      'Desde el bloque $bloque: $componente';

  static const turnoSorteoExplicacion =
      'El sorteo deja un compromiso guardado antes de sortear y revela la semilla después. Con esos datos podés reproducir el orden.';
  static const comprobarSorteo = 'Comprobar el sorteo';

  static const reclamoTitulo = 'Tu reclamo';
  static const reclamoInstancias = 'Si la respuesta no te convence';
}
