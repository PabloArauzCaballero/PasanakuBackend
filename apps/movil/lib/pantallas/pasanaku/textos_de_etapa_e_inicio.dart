/// Textos de la etapa mínima al crear un grupo y del inicio al 75 % (2026-10-30, plan
/// del inicio al 75 %, H1.S3 y H1.S6). Cada frase dice lo que el servidor
/// hace hoy (`grupos.yaml`: `crearGrupo.etapaMinima`, `iniciarGrupo`).
abstract final class TextosDeEtapaEInicio {
  // Etapa mínima (crear grupo)
  static const etapaEtiqueta = 'Etapa mínima para entrar';
  static String etapaAyuda(String nombre) =>
      'Solo pueden entrar personas desde $nombre. No podés pedir una etapa más '
      'alta que la tuya.';
  // La hoja reserva 48 dp por fila (hoja_de_seleccion.dart): el detalle tiene que
  // entrar en una línea a 360 dp, así que «tuya» va primero y corto.
  static String etapaDesde(String puntos, {required bool esLaTuya}) =>
      esLaTuya ? 'Tuya · desde $puntos puntos' : 'Desde $puntos puntos';
  static const etapaCargando = 'Cargando las etapas';
  static const etapaError =
      'No pudimos traer las etapas. Si creás el grupo así, pide tu etapa actual.';
  static const etapaSinCatalogo =
      'Por ahora no hay etapas para elegir. Podés crear el grupo igual.';
  static const reintentar = 'Reintentar';

  // Inicio al 75 % (pantalla del grupo)
  static const inicioTitulo = 'Para iniciar el grupo';
  static const inicioEtiquetaProgreso = 'Cupos ocupados para poder iniciar';
  static String inicioProgreso(int ocupados, int total, int faltan) =>
      '$ocupados de $total · ${faltan <= 0
          ? 'ya se puede iniciar'
          : faltan == 1
          ? 'falta 1 para poder iniciar'
          : 'faltan $faltan para poder iniciar'}';
  static String inicioRegla(int minimo, int total) =>
      'Se inicia con el 75 % de los cupos ocupados: $minimo de $total.';
  static const iniciarBoton = 'Iniciar el grupo';
  static String motivoFaltan(int faltan) => faltan == 1
      ? 'Falta 1 cupo ocupado para poder iniciar.'
      : 'Faltan $faltan cupos ocupados para poder iniciar.';
  static String motivoFirmas(int pendientes) => pendientes == 1
      ? '1 persona todavía no firmó el reglamento. Cuando firme, vas a poder iniciar.'
      : '$pendientes personas todavía no firmaron el reglamento. Cuando firmen, '
            'vas a poder iniciar.';
  static const motivoGeneral =
      'Todavía no se puede iniciar. Deslizá hacia abajo para actualizar.';

  // Confirmación: lo que cambia y que no tiene vuelta atrás.
  static String confirmarTitulo(int ocupados) =>
      'Iniciar el grupo con $ocupados personas';
  static String confirmarDetalle(int ocupados, int total) => ocupados < total
      ? 'El grupo se achica a los $ocupados cupos ocupados: va a tener $ocupados '
            'turnos en vez de $total. Los ${total - ocupados} cupos libres se cierran '
            'y nadie más va a poder entrar. No se puede deshacer.'
      : 'El grupo queda con sus $ocupados cupos y nadie más va a poder entrar. '
            'No se puede deshacer.';
  static const confirmar = 'Sí, iniciar';
  static String iniciado(int cupos) =>
      'Iniciaste el grupo con $cupos personas.';
}
