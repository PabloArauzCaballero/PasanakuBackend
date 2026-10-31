/// Textos del perfil de un integrante (2026-10-21). Todo lo que se muestra viene
/// del servidor; acá solo está cómo se dice.
class TextosPerfilDelIntegrante {
  TextosPerfilDelIntegrante._();

  static const titulo = 'Perfil del integrante';
  static const cargando = 'Cargando el perfil';
  static const soloMismoGrupo =
      'Solo quienes están en el mismo grupo ven estos datos.';

  // Contacto
  static const contacto = 'Contacto';
  static const telefono = 'Teléfono';

  // Confianza
  static const confianza = 'Confianza';
  static const nivel = 'Nivel de confianza';
  static const puntualidad = 'Aportes a tiempo';
  static const completados = 'Grupos completados';
  static const abandonados = 'Grupos abandonados';
  static const incumplimientos = 'Incumplimientos abiertos';
  static const antiguedad = 'En AportaYa hace';
  static const sinHistorial =
      'Todavía no tiene historial en AportaYa. Es normal si recién empieza: '
      'no es una mala señal.';
  static const queMide =
      'Lo calcula AportaYa con los aportes y los grupos reales de la persona. '
      'Nadie lo puede editar a mano.';

  // Insignias
  static const insignias = 'Insignias';
  static const sinInsignias = 'Todavía no ganó insignias.';

  static String porcentaje(String valor) => '$valor %';

  static String meses(int n) => switch (n) {
    0 => 'Menos de un mes',
    1 => '1 mes',
    _ => '$n meses',
  };

  /// El nivel tal como lo nombra `transparencia`, dicho para una persona.
  static String nivelDeConfianza(String codigo) => switch (codigo) {
    'REFERENTE' => 'Referente',
    'MUY_CONFIABLE' => 'Muy confiable',
    'CONFIABLE' => 'Confiable',
    'BASICO' => 'Básico',
    'EN_OBSERVACION' => 'En observación',
    'RESTRINGIDO' => 'Restringido',
    'SIN_HISTORIAL' => 'Sin historial',
    _ => codigo,
  };
}
