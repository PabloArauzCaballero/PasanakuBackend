/// Textos del grupo por dentro (2026-10-20): resumen, integrantes, firma y sorteo.
class TextosDetalleDelGrupo {
  TextosDetalleDelGrupo._();

  // Resumen
  static String cupos(int ocupados, int totales) =>
      '$ocupados de $totales cupos ocupados';
  static String aporte(String monto, String periodicidad) =>
      'Aporte de $monto · ${periodicidad.toLowerCase()}';
  static String estado(String estado) => switch (estado) {
    'BORRADOR' => 'Abriéndose',
    'ABIERTO_A_INSCRIPCION' => 'Recibiendo integrantes',
    'CONFORMADO' => 'Completo, listo para el sorteo',
    'EN_CURSO' || 'ACTIVO' => 'En curso',
    'FINALIZADO' => 'Terminado',
    _ => estado,
  };

  // Integrantes
  static const integrantes = 'Integrantes';
  static const cupoLibre = 'Cupo libre';
  static String cupo(int numero) => 'Cupo $numero';
  static const organiza = 'Organiza';
  static const vos = 'Tú';
  static const tu = 'Tú';
  static const firmo = 'Firmó';
  static const faltaFirmar = 'Falta firmar';
  static const libre = 'Libre';
  static String turno(int n) => 'Turno $n';
  static const sinTurno = 'Sin turno todavía';
  static const sinNombre = 'Integrante';
  static String etiquetaDeTarjeta(String nombre, String estado, int? turno) =>
      turno == null ? '$nombre, $estado' : '$nombre, $estado, turno $turno';
  static const queEs =
      'Cada cupo aporta en cada periodo y cobra una vez, en el turno que salga en el sorteo.';

  // Firma
  static const debesFirmar = 'Te falta firmar el reglamento';
  static const debesFirmarDetalle =
      'Leelo y firmalo para ocupar tu cupo. El sorteo se hace cuando todos firmaron.';
  static const firmarReglamento = 'Leer y firmar el reglamento';
  static const tituloFirma = 'Reglamento del grupo';
  static const firmar = 'Firmar el reglamento';
  static const firmado = 'Listo, firmaste el reglamento.';
  static const firmadoYCompleto =
      '¡Listo! Con tu firma el grupo quedó completo.';

  // Sorteo
  static const iniciarSorteo = 'Iniciar sorteo';
  static const verSorteo = 'Ver el orden de cobro';
  static const esperandoFirmas =
      'Cuando todos los cupos estén ocupados y firmados vas a poder iniciar el sorteo.';
  static const tituloSorteo = 'Sorteo de turnos';
  static const sorteoAyuda =
      'El orden lo decide el servidor con una semilla sellada antes del sorteo: nadie '
      'puede elegirlo ni repetirlo. Cualquiera puede verificarlo después.';
  static const girar = 'Girar la ruleta';
  static const girando = 'Sorteando…';
  static const ordenDeCobro = 'Orden de cobro';
  static String posicion(int n, String nombre) => '$n.º  $nombre';
  static const listo = 'Volver al grupo';
  static const ruletaEtiqueta = 'Ruleta del sorteo de turnos';
}
