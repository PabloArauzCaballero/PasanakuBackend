/// Voz de marca de la bandeja. Nada de literales sueltos en los widgets.
library;

class TextosNotificaciones {
  static const titulo = 'Notificaciones';
  // El vacío dice qué es esta lista, sin afirmar «estás al día» (la bandeja no sabe si
  // hay algo pendiente en otro lado) ni prometer que va a mostrar «todo».
  static const tituloVacio = 'Todavía no tenés avisos';
  static const sinAvisos = 'Esta lista junta los avisos sobre tu cuenta.';

  static const ahora = 'ahora';
  static String haceMinutos(int n) => 'hace $n min';
  static String haceHoras(int n) => 'hace $n h';
}
