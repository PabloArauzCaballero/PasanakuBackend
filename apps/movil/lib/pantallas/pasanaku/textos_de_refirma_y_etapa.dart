/// Textos de la versión nueva del reglamento y de la etapa de cada integrante
/// (H1.S7.M4, 2026-10-30). Cada frase dice lo que hace el servidor hoy
/// (`grupos.yaml`: `DetalleDelGrupo.reglamentoPendienteDeAceptar`,
/// `firmarReglamentoDelGrupo`, `IntegranteDelGrupo.etapa`).
abstract final class TextosDeRefirmaYEtapa {
  // Versión nueva del reglamento: al iniciar, el grupo se achica y el reglamento se
  // vuelve a publicar con los números reales. Cada integrante la acepta antes del
  // sorteo; aceptarla no cambia su cupo.
  static const avisoTitulo = 'Firmá la versión nueva del reglamento';
  static const avisoDetalle =
      'Al iniciar el grupo, el reglamento se actualizó con los cupos y los turnos '
      'reales. Tu lugar sigue siendo el mismo; el sorteo se hace cuando todos '
      'aceptaron esta versión.';
  static const avisoBoton = 'Leer y firmar la versión nueva';
  static String version(int numero) => 'Versión $numero del reglamento';
  static const firmarNueva = 'Firmar la versión nueva';
  static const firmadaNueva =
      'Listo, aceptaste la versión nueva del reglamento.';

  // Etapa del integrante: solo la etapa, nunca el puntaje.
  static String etiquetaDeEtapa(String etapa) => 'Etapa de reputación: $etapa';
}
