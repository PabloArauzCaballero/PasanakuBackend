/// Los textos de «Traspasar mi cupo» (CU-64). Dicen lo que el servidor hace y nada más:
/// no hay precio, no hay pago, el turno se queda con el cupo y la deuda vencida con
/// quien la generó (`grupos.yaml`, `traspasarCupo`).
abstract final class TextosDelTraspaso {
  static const titulo = 'Traspasar mi cupo';
  static const accesoDetalle =
      'Pasale tu lugar a otra persona del grupo. Tu turno queda con el cupo.';

  static const pasos = ['Quién entra', 'Motivo', 'Revisá y confirmá'];

  static const elegirTitulo = '¿A quién le pasás tu cupo?';
  static const elegirExplicacion =
      'Solo se puede traspasar a alguien que ya participa de este grupo.';
  static const nadieTitulo = 'Todavía no hay a quién traspasarle';
  static const nadieDetalle =
      'La persona que entra tiene que participar ya de este grupo. Cuando haya '
      'alguien más, vas a poder elegirla acá.';
  static const sinCupoTitulo = 'No ocupás un cupo en este grupo';
  static const sinCupoDetalle =
      'Solo quien ocupa un cupo lo puede traspasar. Volvé al grupo para ver tu lugar.';

  static const motivoTitulo = 'Contá por qué';
  static const motivoExplicacion =
      'El motivo queda registrado junto con el traspaso.';
  static const motivoEtiqueta = 'Motivo del traspaso';
  static const motivoAyuda = 'Entre 10 y 300 caracteres.';
  static const motivoCorto = 'Escribí al menos 10 caracteres.';
  static String contador(int n) => '$n / 300';

  static const entregas = 'ENTREGÁS';
  static const recibe = 'RECIBE';
  static String tuCupo(int numero) => 'Tu cupo N° $numero';
  static String cupoN(int numero) => 'Cupo $numero';
  static String delGrupo(String nombre) => 'Grupo $nombre';
  static String turno(int n) => 'Turno $n';
  static const sinTurno = 'Sin turno todavía';
  static const integranteDelGrupo = 'Integrante del grupo';
  static const firmoReglamento = 'Firmó el reglamento';
  static const faltaFirmar = 'Falta que firme';
  static const organiza = 'Organiza';
  static const sinNombre = 'Integrante sin nombre';

  static const noSeMueveTitulo = 'Lo que NO se mueve';
  static const turnoQueda = 'Tu turno se queda con el cupo';
  static String turnoQuedaDetalle(int? turno) => turno == null
      ? 'El turno que salga en el sorteo es del cupo, no de la persona.'
      : 'Quien entra cobra en el turno $turno, en el mismo lugar del calendario. '
            'Nadie se adelanta ni se atrasa.';
  static const deudaQueda = 'Tu deuda vencida se queda con vos';
  static const deudaQuedaDetalle =
      'Si tenés aportes vencidos, primero hay que saldarlos. Los aportes que '
      'vencen después del traspaso los paga quien entra.';
  static const salisDelGrupo = 'Dejás de participar del grupo';
  static const salisDelGrupoDetalle =
      'Al traspasar salís del grupo y tu lugar lo ocupa quien entra.';
  static const sinPrecio = 'No hay precio ni pago';
  static const sinPrecioDetalle =
      'Este traspaso no mueve dinero: la app no cobra ni paga nada por él.';

  static const condicionesTitulo = 'Lo que se revisa al confirmar';
  static const condicionAlDia = 'Tus aportes están al día';
  static const condicionVerificacion =
      'Quien entra tiene la verificación de identidad que pide el grupo';
  static const condicionReputacion =
      'Quien entra llega a la reputación mínima del grupo';
  static const condicionAcuerdo = 'El grupo aprobó el traspaso en una votación';
  static const acuerdoAdjunto = 'Votación aprobada adjunta.';
  static const acuerdoFalta =
      'Todavía no pediste la votación. Pedila abajo: el grupo vota y, aprobada, '
      'volvés acá a traspasar.';
  static String acuerdoAbierto(DateTime cierra) =>
      'El grupo está votando, hasta el ${cierra.day}/${cierra.month}. Cuando se '
      'apruebe, volvés acá a traspasar.';
  static const acuerdoRevisando = 'Revisando la votación del grupo…';
  static const pedirVotacion = 'Pedir la votación al grupo';
  static const esperandoVotacion = 'Esperando la votación';
  static const votacionEnCurso = 'El grupo está votando tu traspaso.';
  static String descripcionDeVotacion(
    int numero,
    String entrante,
    String motivo,
  ) => 'Traspaso del cupo N° $numero a $entrante. Motivo: $motivo';

  static const continuar = 'Continuar';
  static const volver = 'Volver';
  static const traspasar = 'Traspasar cupo';
  static const confirmarTitulo = '¿Traspasás tu cupo?';
  static const confirmar = 'Sí, traspasar';
  static const pasaA = 'Pasa a';
  static const cupo = 'Cupo';
  static const motivo = 'Motivo';

  static const listoTitulo = 'Cupo traspasado';
  static String listoDetalle(String nombre) =>
      '$nombre ocupa ahora tu cupo, con el mismo turno. Vos ya no participás de '
      'este grupo.';
  static String comprobante(String id) =>
      'Traspaso ${id.length > 8 ? id.substring(0, 8) : id}';
  static const numeroDeTraspaso = 'Número de traspaso';
  static const irAMisGrupos = 'Ir a mis grupos';
  static String codigoDeSeguimiento(String traza) =>
      'Código de seguimiento: $traza';
}
