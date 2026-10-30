import 'package:aportaya_diseno/errores.dart';

import '../../dominio/errores.dart';

/// Los AP-CU136 del contrato (`entregas.yaml`, subastas), en voseo y sin el texto
/// del backend.
const Map<String, String> _porCodigo = {
  'AP-CU136-01':
      'Este cupo no se puede subastar: puede que tengas aportes vencidos, que su '
      'turno ya se haya cobrado o que ya no sea tuyo.',
  'AP-CU136-02': 'Este cupo ya está en subasta, o ya tiene ofertas.',
  'AP-CU136-03':
      'Revisá el monto: tiene que ser al menos el mínimo que se muestra.',
  'AP-CU136-04':
      'Alguien ya ofreció eso o más. Mirá la oferta más alta y ofrecé por encima.',
  'AP-CU136-05': 'Esta subasta ya cerró o ya no está disponible.',
  'AP-CU136-06':
      'Todavía no podés entrar a este grupo: revisá tu verificación de identidad, tu '
      'reputación o cuántos grupos tenés.',
  'AP-CU136-07':
      'No te alcanza el saldo para esa oferta. Cargá saldo en tu billetera y volvé.',
  'AP-CU136-08':
      'No pudimos confirmar los datos del grupo. Probá de nuevo en un momento.',
};

/// El mensaje para la persona y la referencia para soporte.
({String mensaje, String? traza}) rechazoDeLaSubasta(Object error) {
  final traza = error is ErrorPresentable ? error.trazaId : null;
  if (error is ErrorDeApi) {
    final conocido = _porCodigo[error.codigo];
    if (conocido != null) return (mensaje: conocido, traza: traza);
  }
  final mensaje = error is ErrorPresentable
      ? error.mensaje
      : 'No pudimos completar la subasta. Probá de nuevo en un momento.';
  return (mensaje: mensaje, traza: traza);
}
