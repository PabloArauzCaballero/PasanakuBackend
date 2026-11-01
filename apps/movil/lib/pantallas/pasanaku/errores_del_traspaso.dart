import 'package:aportaya_diseno/errores.dart';

import '../../dominio/errores.dart';

/// En qué parte de la revisión del traspaso cae un rechazo. Cada tarjeta de la revisión
/// muestra el suyo debajo, en vez de un error genérico al pie.
enum LugarDelRechazo { cupo, deuda, verificacion, reputacion, acuerdo, general }

/// Un rechazo del traspaso ya traducido: dónde va y qué decirle a la persona.
class RechazoDelTraspaso {
  const RechazoDelTraspaso(this.lugar, this.mensaje, {this.traza});
  final LugarDelRechazo lugar;
  final String mensaje;
  final String? traza;
}

/// Los AP-CU64 del contrato (`grupos.yaml`, `traspasarCupo`), en voseo y sin el texto
/// del backend. AP-CU64-01 cubre también «quien entra no participa de ese grupo».
const Map<String, (LugarDelRechazo, String)> _porCodigo = {
  'AP-CU64-01': (
    LugarDelRechazo.cupo,
    'Este cupo ya no se puede traspasar: puede que su turno ya se haya cobrado, '
        'que el cupo esté libre o que la persona elegida ya no participe del grupo.',
  ),
  'AP-CU64-02': (
    LugarDelRechazo.deuda,
    'Tenés aportes vencidos. Saldalos primero: tu deuda no pasa a quien entra.',
  ),
  'AP-CU64-03': (
    LugarDelRechazo.verificacion,
    'Esta persona todavía no tiene el nivel de verificación de identidad que '
        'pide el grupo.',
  ),
  'AP-CU64-04': (
    LugarDelRechazo.reputacion,
    'Esta persona no llega a la reputación mínima que pide el grupo.',
  ),
  'AP-CU64-05': (
    LugarDelRechazo.acuerdo,
    'El grupo tiene que aprobar este traspaso en una votación antes de hacerlo.',
  ),
  'AP-CU64-06': (
    LugarDelRechazo.cupo,
    'Este cupo ya no es tuyo: solo quien lo tiene puede traspasarlo.',
  ),
};

/// Sin el permiso del servidor (403): se dice qué pasa, no «algo salió mal».
const _sinPermiso =
    'Tu cuenta todavía no tiene permiso para traspasar cupos en este grupo.';

RechazoDelTraspaso rechazoDelTraspaso(Object error) {
  final traza = error is ErrorPresentable ? error.trazaId : null;
  if (error is ErrorDeApi) {
    final conocido = _porCodigo[error.codigo];
    if (conocido != null) {
      return RechazoDelTraspaso(conocido.$1, conocido.$2, traza: traza);
    }
    if (error.estado == 403) {
      return RechazoDelTraspaso(
        LugarDelRechazo.general,
        _sinPermiso,
        traza: traza,
      );
    }
  }
  final mensaje = error is ErrorPresentable
      ? error.mensaje
      : 'No pudimos traspasar el cupo. Probá de nuevo en un momento.';
  return RechazoDelTraspaso(LugarDelRechazo.general, mensaje, traza: traza);
}
