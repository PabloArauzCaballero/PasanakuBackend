import 'package:aportaya_diseno/dinero/formatear.dart';

import 'periodicidades.dart';

/// Solo datos del contrato que pueden mostrarse y usarse para aceptar.
class DetalleVisibleDeInvitacion {
  const DetalleVisibleDeInvitacion({
    required this.nombre,
    required this.aporteFormateado,
    required this.frecuencia,
    required this.reglamento,
    required this.hashReglamento,
  });

  final String nombre;
  final String aporteFormateado;
  final String frecuencia;
  final String reglamento;
  final String hashReglamento;

  factory DetalleVisibleDeInvitacion.desde(Map<String, dynamic>? datos) {
    final nombre = datos?['nombre'];
    final monto = datos?['montoAporte'];
    final moneda = datos?['moneda'];
    final periodicidad = datos?['periodicidad'];
    final reglamento = datos?['reglamento'];
    final hash = datos?['hashReglamento'];
    if (nombre is! String ||
        nombre.trim().isEmpty ||
        monto is! String ||
        moneda is! String ||
        (moneda != 'BOB' && moneda != 'USD') ||
        periodicidad is! String ||
        reglamento is! String ||
        hash is! String ||
        !RegExp(r'^[0-9a-f]{64}$').hasMatch(hash)) {
      throw const FormatException('Detalle de invitación fuera de contrato');
    }
    final frecuencia = periodicidades[periodicidad]?.nombre;
    if (frecuencia == null || centavosDe(monto) <= 0) {
      throw const FormatException('Compromiso de invitación no verificable');
    }
    return DetalleVisibleDeInvitacion(
      nombre: nombre,
      aporteFormateado: formatearMonto(monto: monto, moneda: moneda),
      frecuencia: frecuencia,
      reglamento: reglamento,
      hashReglamento: hash,
    );
  }
}
