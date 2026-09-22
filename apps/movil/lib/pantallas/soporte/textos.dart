/// Los textos del centro de ayuda, en voz de marca. Un archivo por dominio.
abstract final class TextosSoporte {
  static const titulo = 'Ayuda';
  static const proposito =
      'Aprendé a usar AportaYa haciéndolo, sobre las pantallas de verdad. Ningún tutorial mueve tu plata ni confirma nada por vos.';
  static const buscar = 'Buscar un tutorial';
  static const todos = 'Todos';
  static const pendientes = 'Pendientes';
  static const enProgreso = 'En progreso';
  static const completados = 'Completados';
  static const avanceTitulo = 'Tu avance';
  static const avanceEtiqueta = 'Avance general de los tutoriales';
  static const sinResultados =
      'Ningún tutorial coincide con lo que buscaste. Probá con otra palabra.';
  static const sinTutoriales =
      'Todavía no hay tutoriales para vos. Cuando los haya, aparecen acá.';
  static const recomendado = 'Te conviene empezar por acá';
  static const comenzar = 'Comenzar';
  static const continuar = 'Continuar';
  static const repetir = 'Repetir';
  static const reiniciar = 'Reiniciar';
  static const obligatorio = 'Importante';
  static const requisitos = 'Antes conviene hacer';
  static const siguiente = 'Siguiente';
  static const terminar = 'Terminar';
  static const omitir = 'Dejar el tutorial';
  static const salirTitulo = '¿Dejamos el tutorial acá?';
  static const salirCuerpo =
      'Guardamos por qué paso ibas. Podés retomarlo cuando quieras desde Ayuda.';
  static const salirConfirmar = 'Dejarlo por ahora';
  static const salirSeguir = 'Seguir';
  static const comoFunciona = 'Cómo funciona esta pantalla';
  static const problemasTitulo = 'Hay tutoriales mal configurados';

  static String avanceDe(int hechos, int total) =>
      '$hechos de $total tutoriales completados';
  static String minutos(int n) => '$n min';
  static String pasos(int n) => n == 1 ? '1 paso' : '$n pasos';
}
